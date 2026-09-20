package com.verifipay.device.service;

import com.verifipay.common.exception.BusinessException;
import com.verifipay.device.dto.YapeDeviceEventRequest;
import com.verifipay.device.dto.YapeDeviceEventResponse;
import com.verifipay.device.entity.ReceiverDevice;
import com.verifipay.device.repository.ReceiverDeviceRepository;
import com.verifipay.device.util.DeviceTokenUtil;
import com.verifipay.payment.entity.PaymentEvent;
import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DevicePaymentEventService {

    private static final ZoneId PERU_ZONE =
            ZoneId.of("America/Lima");

    private final ReceiverDeviceRepository receiverDeviceRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final DeviceTokenUtil deviceTokenUtil;

    @Transactional
    public YapeDeviceEventResponse receiveYapeEvent(
            String rawDeviceToken,
            YapeDeviceEventRequest request
    ) {

        if (rawDeviceToken == null || rawDeviceToken.isBlank()) {
            throw new BusinessException(
                    "Token de dispositivo requerido"
            );
        }

        String tokenHash =
                deviceTokenUtil.hash(rawDeviceToken.trim());

        ReceiverDevice device =
                receiverDeviceRepository
                        .findByTokenHashAndActiveTrue(tokenHash)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Dispositivo no autorizado"
                                )
                        );

        /*
         * Si Android vuelve a entregar exactamente
         * la misma notificación, no creamos otro pago.
         */
        if (paymentEventRepository.existsBySourceEventId(
                request.sourceEventId()
        )) {

            device.setLastSeenAt(LocalDateTime.now());

            return new YapeDeviceEventResponse(
                    null,
                    true
            );
        }

        LocalDateTime occurredAt =
                Instant
                        .ofEpochMilli(
                                request.occurredAtEpochMillis()
                        )
                        .atZone(PERU_ZONE)
                        .toLocalDateTime();

        PaymentEvent event =
                new PaymentEvent();

        event.setCompany(
                device.getCompany()
        );

        event.setBranch(
                device.getBranch()
        );

        event.setMethod(
                PaymentMethod.YAPE
        );

        event.setAmount(
                request.amount()
        );

        /*
         * Por ahora usamos reference para guardar
         * el nombre que Yape muestra del remitente.
         */
        event.setReference(
                request.sender()
        );

        event.setSecurityCode(
                request.securityCode()
        );

        event.setOccurredAt(
                occurredAt
        );

        event.setConsumed(false);

        event.setSourceEventId(
                request.sourceEventId()
        );

        PaymentEvent saved =
                paymentEventRepository.save(event);

        device.setLastSeenAt(
                LocalDateTime.now()
        );

        return new YapeDeviceEventResponse(
                saved.getId(),
                false
        );
    }
}