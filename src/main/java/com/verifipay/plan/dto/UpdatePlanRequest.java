package com.verifipay.plan.dto;
import com.verifipay.common.enums.BillingInterval;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record UpdatePlanRequest(
    @Size(min = 1, max = 150) @Pattern(regexp = ".*\\S.*") String name,
    @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
    @Pattern(regexp = "[A-Z]{3}") String currency,
    BillingInterval billingInterval,
    @Min(0) Integer maxCashiers,
    @Min(0) Integer maxReceiverDevices,
    Boolean active
) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("Atributo no permitido: " + name);
    }
}
