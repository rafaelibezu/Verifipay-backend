package com.verifipay.device.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record YapeDeviceEventRequest(

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(
                value = "0.01",
                message = "El monto debe ser mayor a cero"
        )
        @Digits(integer = 10, fraction = 2)
        BigDecimal amount,

        @Size(max = 20) @Pattern(regexp = "[^\\x00]*")
        String securityCode,

        @Size(max = 100) @Pattern(regexp = "[^\\x00]*")
        String sender,

        @NotNull(message = "La fecha del evento es obligatoria")
        Long occurredAtEpochMillis,

        @NotBlank(message = "El identificador del evento es obligatorio")
        @Size(max = 150) @Pattern(regexp = "[^\\x00]*")
        String sourceEventId

) {
}
