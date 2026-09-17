package com.verifipay.payment.dto;

import com.verifipay.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEventResponse(

        Long id,
        Long companyId,
        Long branchId,

        PaymentMethod method,
        BigDecimal amount,

        String reference,
        String securityCode,

        LocalDateTime occurredAt,
        Boolean consumed

) {
}