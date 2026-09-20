package com.verifipay.paymentrequest.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreatePaymentRequestRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
        BigDecimal amount,
        @NotBlank @Size(max = 150)
        @Pattern(regexp = "[^\\x00]*", message = "no debe contener el caracter NUL")
        String clientRequestId
) {
    // Reject unknown properties only for this contract, without changing legacy JSON handling.
    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new IllegalArgumentException("El body solo admite amount y clientRequestId");
    }
}
