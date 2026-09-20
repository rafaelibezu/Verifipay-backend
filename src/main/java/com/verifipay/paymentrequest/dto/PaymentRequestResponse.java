package com.verifipay.paymentrequest.dto;

import com.verifipay.common.enums.PaymentRequestStatus;
import com.verifipay.paymentrequest.entity.PaymentRequest;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequestResponse(
        Long id, Long companyId, Long branchId, Long cashierId,
        BigDecimal amount, PaymentRequestStatus status, String clientRequestId,
        Instant createdAt, Instant expiresAt, Instant paidAt
) {
    public static PaymentRequestResponse from(PaymentRequest request) {
        return new PaymentRequestResponse(
                request.getId(), request.getCompany().getId(),
                request.getBranch() == null ? null : request.getBranch().getId(),
                request.getCashier().getId(), request.getAmount(), request.getStatus(),
                request.getClientRequestId(), request.getCreatedAt(), request.getExpiresAt(), request.getPaidAt());
    }
}
