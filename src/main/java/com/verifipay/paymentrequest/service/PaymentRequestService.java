package com.verifipay.paymentrequest.service;

import com.verifipay.common.api.PageResponse;
import com.verifipay.common.enums.PaymentRequestStatus;
import com.verifipay.common.enums.UserRole;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.paymentrequest.dto.CreatePaymentRequestRequest;
import com.verifipay.paymentrequest.dto.PaymentRequestResponse;
import com.verifipay.paymentrequest.repository.PaymentRequestRepository;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@Validated
public class PaymentRequestService {
    private final PaymentRequestRepository repository;
    private final CurrentUserService currentUserService;
    private final CompanyRepository companyRepository;
    private final Duration expiration;
    private final Clock clock;

    @Autowired
    public PaymentRequestService(PaymentRequestRepository repository, CurrentUserService currentUserService,
            CompanyRepository companyRepository,
            @Value("${payment-request.expiration-minutes:5}") long expirationMinutes) {
        this(repository, currentUserService, companyRepository, expirationMinutes, Clock.systemUTC());
    }

    PaymentRequestService(PaymentRequestRepository repository, CurrentUserService currentUserService,
            CompanyRepository companyRepository,
            long expirationMinutes, Clock clock) {
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("payment-request.expiration-minutes debe ser positivo");
        }
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.companyRepository = companyRepository;
        this.expiration = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    public record CreationResult(PaymentRequestResponse paymentRequest, boolean created) { }

    @Transactional
    public CreationResult create(@Valid CreatePaymentRequestRequest input) {
        User cashier = requireActiveCashier();
        lockCompany(cashier);
        Instant now = clock.instant();
        int inserted = repository.insertIfAbsent(cashier.getCompany().getId(),
                cashier.getBranch() == null ? null : cashier.getBranch().getId(), cashier.getId(),
                input.amount(), input.clientRequestId(), now, now.plus(expiration));
        var request = repository.findByCashierIdAndClientRequestId(cashier.getId(), input.clientRequestId())
                .orElseThrow(() -> new IllegalStateException("Cobro no encontrado tras la insercion"));
        if (!request.getCompany().getId().equals(cashier.getCompany().getId())) {
            throw new ResourceNotFoundException("Cobro no encontrado");
        }
        if (request.getAmount().compareTo(input.amount()) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "clientRequestId ya fue utilizado con otro monto");
        }
        return new CreationResult(PaymentRequestResponse.from(request), inserted == 1);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentRequestResponse> findMine(int page, int size) {
        User cashier = requireActiveCashier();
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page debe ser >= 0 y size entre 1 y 100");
        }
        if ((long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El offset de paginacion excede el limite permitido");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(repository.findAllByCashierIdAndCompanyId(
                cashier.getId(), cashier.getCompany().getId(), pageable).map(PaymentRequestResponse::from));
    }

    @Transactional(readOnly = true)
    public PaymentRequestResponse findMineById(Long id) {
        User cashier = requireActiveCashier();
        return PaymentRequestResponse.from(repository.findByIdAndCashierIdAndCompanyId(
                id, cashier.getId(), cashier.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cobro no encontrado")));
    }

    @Transactional
    public PaymentRequestResponse cancel(Long id) {
        User cashier = requireActiveCashier();
        lockCompany(cashier);
        var request = repository.findOwnedForUpdate(id, cashier.getId(), cashier.getCompany().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cobro no encontrado"));
        if (request.getStatus() != PaymentRequestStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede cancelar un cobro WAITING");
        }
        request.cancel(clock.instant());
        return PaymentRequestResponse.from(request);
    }

    private void lockCompany(User cashier) {
        var company = companyRepository.findForUpdate(cashier.getCompany().getId())
                .orElseThrow(() -> new AccessDeniedException("Negocio no disponible"));
        if (!Boolean.TRUE.equals(company.getActive())) {
            throw new AccessDeniedException("Se requiere un negocio activo");
        }
    }

    private User requireActiveCashier() {
        User user = currentUserService.getCurrentUser();
        if (user.getRole() != UserRole.CASHIER || !Boolean.TRUE.equals(user.getActive())
                || user.getCompany() == null || !Boolean.TRUE.equals(user.getCompany().getActive())) {
            throw new AccessDeniedException("Se requiere un cajero y negocio activos");
        }
        if (user.getBranch() != null && (!Boolean.TRUE.equals(user.getBranch().getActive())
                || !user.getBranch().getCompany().getId().equals(user.getCompany().getId()))) {
            throw new AccessDeniedException("El local debe estar activo y pertenecer al negocio");
        }
        return user;
    }
}
