package com.verifipay.verification.dto;

import com.verifipay.common.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateVerificationRequest(

        @NotNull(message = "El método es obligatorio")
        PaymentMethod method,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El monto debe ser mayor a cero"
        )
        BigDecimal amount,

        String reference,

        String securityCode

) {
}