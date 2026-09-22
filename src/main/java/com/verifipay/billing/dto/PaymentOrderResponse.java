package com.verifipay.billing.dto;
import com.verifipay.common.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
public record PaymentOrderResponse(Long id, Long companyId, Long subscriptionId, BigDecimal amount, String currency,
    PaymentOrderStatus status, PaymentProvider provider, String externalReference, String providerTransactionId,
    Instant paidAt, Instant failedAt, Instant cancelledAt, Instant expiresAt, Instant createdAt, Instant updatedAt) {}
