package com.verifipay.plan.dto;
import com.verifipay.common.enums.BillingInterval;
import java.math.BigDecimal;
import java.time.Instant;
public record PlanResponse(Long id, String code, String name, BigDecimal price, String currency,
    BillingInterval billingInterval, Integer maxCashiers, Integer maxReceiverDevices, Boolean active,
    Instant createdAt, Instant updatedAt) {}
