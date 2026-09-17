package com.verifipay.verification.service;

import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.payment.entity.PaymentEvent;
import com.verifipay.payment.repository.PaymentEventRepository;
import com.verifipay.security.CurrentUserService;
import com.verifipay.user.entity.User;
import com.verifipay.verification.dto.CreateVerificationRequest;
import com.verifipay.verification.dto.VerificationResponse;
import com.verifipay.verification.entity.Verification;
import com.verifipay.verification.provider.PaymentVerificationProvider;
import com.verifipay.verification.provider.VerificationProviderResolver;
import com.verifipay.verification.provider.VerificationResult;
import com.verifipay.verification.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.verifipay.audit.service.AuditService;
import com.verifipay.common.enums.AuditAction;
import com.verifipay.verification.mapper.VerificationMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final VerificationProviderResolver providerResolver;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final VerificationMapper verificationMapper;

    @Transactional
    public VerificationResponse verify(
            CreateVerificationRequest request
    ) {

        User cashier =
                currentUserService.getCurrentUser();

        if (cashier.getBranch() == null) {
            throw new BusinessException(
                    "El usuario no tiene un local asignado"
            );
        }

        PaymentVerificationProvider provider =
                providerResolver.resolve(request.method());

        VerificationResult result =
                provider.verify(request, cashier);

        PaymentEvent paymentEvent =
                result.paymentEvent();

        LocalDateTime verifiedAt = null;

        if (result.status() == VerificationStatus.DETECTED &&
                paymentEvent != null) {

            paymentEvent.setConsumed(true);

            verifiedAt = LocalDateTime.now();
        }

        Verification verification =
                Verification.builder()
                        .company(cashier.getCompany())
                        .branch(cashier.getBranch())
                        .cashier(cashier)
                        .paymentEvent(paymentEvent)
                        .method(request.method())
                        .amount(request.amount())
                        .reference(request.reference())
                        .securityCode(request.securityCode())
                        .status(result.status())
                        .verifiedAt(verifiedAt)
                        .build();

        Verification saved =
                verificationRepository.save(verification);

        AuditAction auditAction =
                switch (saved.getStatus()) {

                    case DETECTED ->
                            AuditAction.PAYMENT_VERIFICATION_DETECTED;

                    case NOT_FOUND ->
                            AuditAction.PAYMENT_VERIFICATION_NOT_FOUND;

                    case AMBIGUOUS ->
                            AuditAction.PAYMENT_VERIFICATION_AMBIGUOUS;

                    default ->
                            null;
                };

        if (auditAction != null) {

            auditService.register(
                    cashier.getCompany(),
                    cashier,
                    auditAction,
                    "VERIFICATION",
                    saved.getId(),
                    "Método: " + saved.getMethod()
                            + ", monto: " + saved.getAmount()
            );
        }

        return verificationMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VerificationResponse> findMyHistory() {

        User cashier =
                currentUserService.getCurrentUser();

        return verificationRepository
                .findAllByCashierIdOrderByCreatedAtDesc(
                        cashier.getId()
                )
                .stream()
                .map(verificationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VerificationResponse findMyVerificationById(
            Long verificationId
    ) {

        User cashier =
                currentUserService.getCurrentUser();

        Verification verification =
                verificationRepository
                        .findByIdAndCashierId(
                                verificationId,
                                cashier.getId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Verificación no encontrada"
                                )
                        );

        return verificationMapper.toResponse(verification);
    }
}