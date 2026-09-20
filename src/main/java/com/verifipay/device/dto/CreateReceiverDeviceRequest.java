package com.verifipay.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReceiverDeviceRequest(

        @NotNull(message = "El local es obligatorio")
        Long branchId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name
) {
}