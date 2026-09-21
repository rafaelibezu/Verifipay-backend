package com.verifipay.plan.dto;
import com.verifipay.common.enums.BillingInterval;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreatePlanRequest(
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Z][A-Z0-9_]*") String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal price,
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull BillingInterval billingInterval,
    @NotNull @Min(0) Integer maxCashiers,
    @NotNull @Min(0) Integer maxReceiverDevices,
    @NotNull Boolean active
) {}
