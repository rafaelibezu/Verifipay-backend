package com.verifipay.subscription.dto;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
public record CreateSubscriptionRequest(@NotNull @Positive Long planId) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Atributo no permitido: " + name);
    }
}
