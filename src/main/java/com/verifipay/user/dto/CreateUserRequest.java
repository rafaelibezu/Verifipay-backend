package com.verifipay.user.dto;

import com.verifipay.common.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotNull(message = "La empresa es obligatoria")
        Long companyId,

        Long branchId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(
                max = 150,
                message = "El nombre no puede superar los 150 caracteres"
        )
        String name,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(
                min = 8,
                max = 100,
                message = "La contraseña debe tener entre 8 y 100 caracteres"
        )
        String password,

        @NotNull(message = "El rol es obligatorio")
        UserRole role
) {
}