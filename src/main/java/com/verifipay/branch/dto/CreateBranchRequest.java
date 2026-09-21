package com.verifipay.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(

        @NotBlank(message = "El nombre del local es obligatorio")
        @Size(
                max = 150,
                message = "El nombre no puede superar los 150 caracteres"
        )
        String name,

        @Size(
                max = 250,
                message = "La dirección no puede superar los 250 caracteres"
        )
        String address

) {
}
