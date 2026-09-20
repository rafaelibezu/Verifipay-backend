package com.verifipay.device.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record YapeDeviceEventRequest(

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El monto debe ser mayor a cero"
        )
        BigDecimal amount,

        String securityCode,

        String sender,

        @NotNull(message = "La fecha del evento es obligatoria")
        Long occurredAtEpochMillis,

        @NotBlank(message = "El identificador del evento es obligatorio")
        String sourceEventId

) {
}