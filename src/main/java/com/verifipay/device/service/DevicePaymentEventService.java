package com.verifipay.device.service;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.device.dto.YapeDeviceEventRequest;
import com.verifipay.device.dto.YapeDeviceEventResponse;
import com.verifipay.device.repository.ReceiverDeviceRepository;
import com.verifipay.device.util.DeviceTokenUtil;
import com.verifipay.payment.entity.PaymentEvent;
import com.verifipay.payment.repository.PaymentEventRepository;
import com.verifipay.payment.service.PaymentMatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Service
@Validated
@RequiredArgsConstructor
public class DevicePaymentEventService {
    private static final ZoneId PERU_ZONE = ZoneId.of("America/Lima");

    private final ReceiverDeviceRepository receiverDeviceRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final DeviceTokenUtil deviceTokenUtil;
    private final CompanyRepository companyRepository;
    private final PaymentMatchingService matchingService;

    @Transactional
    public YapeDeviceEventResponse receiveYapeEvent(String rawDeviceToken, @Valid YapeDeviceEventRequest request) {
        if (rawDeviceToken == null || rawDeviceToken.isBlank()) {
            throw new BusinessException("Token de dispositivo requerido");
        }
        var device = receiverDeviceRepository.findByTokenHashAndActiveTrue(deviceTokenUtil.hash(rawDeviceToken.trim()))
                .orElseThrow(() -> new BusinessException("Dispositivo no autorizado"));
        var company = companyRepository.findForUpdate(device.getCompany().getId())
                .orElseThrow(() -> new AccessDeniedException("Negocio no disponible"));
        if (!Boolean.TRUE.equals(company.getActive()) || !Boolean.TRUE.equals(device.getBranch().getActive())
                || !device.getBranch().getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException("El negocio y local del dispositivo deben estar activos y relacionados");
        }

        // Epoch milliseconds already identify an absolute instant. Lima is only the legacy representation.
        Instant occurredAtInstant = Instant.ofEpochMilli(request.occurredAtEpochMillis());
        LocalDateTime occurredAt = occurredAtInstant.atZone(PERU_ZONE).toLocalDateTime();
        int inserted;
        try {
            inserted = paymentEventRepository.insertDeviceEventIfAbsent(company.getId(), device.getBranch().getId(),
                    request.amount(), request.sender(), request.securityCode(), occurredAt,
                    LocalDateTime.now(), request.sourceEventId(), occurredAtInstant);
        } catch (DataIntegrityViolationException ex) {
            // A Java Instant can exceed PostgreSQL's timestamp range. Abort, never continue this transaction.
            if (ex.getMostSpecificCause() instanceof SQLException sql && "22008".equals(sql.getSQLState())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha del evento fuera de rango", ex);
            }
            throw ex;
        }
        var event = paymentEventRepository.findBySourceEventIdForUpdate(request.sourceEventId())
                .orElseThrow(() -> new IllegalStateException("Evento no encontrado tras la insercion"));
        if (!samePayload(event, company.getId(), request, occurredAtInstant)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "sourceEventId ya fue utilizado con otros datos");
        }
        if (inserted == 1) {
            matchingService.matchNewEvent(event);
        }
        device.setLastSeenAt(LocalDateTime.now());
        return new YapeDeviceEventResponse(event.getId(), inserted == 0);
    }

    private boolean samePayload(PaymentEvent event, Long companyId, YapeDeviceEventRequest request, Instant occurredAt) {
        Instant persistedInstant = event.getOccurredAtInstant() != null ? event.getOccurredAtInstant()
                : event.getOccurredAt().atZone(PERU_ZONE).toInstant();
        return event.getCompany().getId().equals(companyId) && event.getMethod() == PaymentMethod.YAPE
                && event.getAmount().compareTo(request.amount()) == 0 && persistedInstant.equals(occurredAt)
                && Objects.equals(event.getReference(), request.sender())
                && Objects.equals(event.getSecurityCode(), request.securityCode());
    }
}
