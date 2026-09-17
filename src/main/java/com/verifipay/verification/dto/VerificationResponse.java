package com.verifipay.verification.dto;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VerificationResponse(

        Long id,

        PaymentMethod method,
        BigDecimal amount,

        VerificationStatus status,

        String reference,
        String securityCode,

        LocalDateTime paymentTime,
        LocalDateTime verifiedAt,

        String cashierName,
        String branchName

) {
}