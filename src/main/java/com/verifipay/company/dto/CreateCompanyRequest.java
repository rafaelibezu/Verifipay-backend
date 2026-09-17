package com.verifipay.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(

        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(
                max = 150,
                message = "El nombre no puede superar los 150 caracteres"
        )
        String name,

        @Pattern(
                regexp = "^\\d{11}$",
                message = "El RUC debe contener exactamente 11 dígitos"
        )
        String ruc

) {
}