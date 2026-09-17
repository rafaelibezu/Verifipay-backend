package com.verifipay.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateMockPaymentEventRequest(

        @NotNull(message = "La empresa es obligatoria")
        Long companyId,

        @NotNull(message = "El local es obligatorio")
        Long branchId,

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