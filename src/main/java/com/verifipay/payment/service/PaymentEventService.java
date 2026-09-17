package com.verifipay.payment.service;

import com.verifipay.branch.entity.Branch;
import com.verifipay.branch.repository.BranchRepository;
import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.company.entity.Company;
import com.verifipay.company.repository.CompanyRepository;
import com.verifipay.payment.dto.CreateMockPaymentEventRequest;
import com.verifipay.payment.dto.PaymentEventResponse;
import com.verifipay.payment.entity.PaymentEvent;
import com.verifipay.payment.repository.PaymentEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.verifipay.audit.service.AuditService;
import com.verifipay.common.enums.AuditAction;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentEventService {

    private final PaymentEventRepository paymentEventRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;

    @Transactional
    public PaymentEventResponse createMockYapeEvent(
            CreateMockPaymentEventRequest request
    ) {

        Company company = companyRepository
                .findById(request.companyId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Empresa no encontrada"
                        )
                );

        Branch branch = branchRepository
                .findById(request.branchId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Local no encontrado"
                        )
                );

        if (!branch.getCompany().getId()
                .equals(company.getId())) {

            throw new BusinessException(
                    "El local no pertenece a la empresa indicada"
            );
        }

        PaymentEvent event = PaymentEvent.builder()
                .company(company)
                .branch(branch)
                .method(PaymentMethod.YAPE)
                .amount(request.amount())
                .reference(request.reference())
                .securityCode(request.securityCode())
                .occurredAt(LocalDateTime.now())
                .consumed(false)
                .build();

        PaymentEvent saved =
                paymentEventRepository.save(event);

        auditService.register(
                company,
                null,
                AuditAction.PAYMENT_EVENT_RECEIVED,
                "PAYMENT_EVENT",
                saved.getId(),
                "Yape recibido por S/ " + saved.getAmount()
        );

        return new PaymentEventResponse(
                saved.getId(),
                company.getId(),
                branch.getId(),
                saved.getMethod(),
                saved.getAmount(),
                saved.getReference(),
                saved.getSecurityCode(),
                saved.getOccurredAt(),
                saved.getConsumed()
        );
    }
}