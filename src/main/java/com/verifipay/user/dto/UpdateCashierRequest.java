package com.verifipay.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateCashierRequest(@NotNull Long branchId, @NotBlank @Size(max = 150) String name,
                                   @NotBlank @Email String email) { }
