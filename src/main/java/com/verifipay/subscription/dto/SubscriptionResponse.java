package com.verifipay.subscription.dto;
import com.verifipay.common.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
public record SubscriptionResponse(Long id, Long companyId, Long planId, SubscriptionStatus status,
    BigDecimal contractedPrice, String currency, BillingInterval billingInterval, Instant startedAt,
    Instant endedAt, Instant cancelledAt, Long version, Instant createdAt, Instant updatedAt) {}
