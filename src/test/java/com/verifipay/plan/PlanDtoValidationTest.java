package com.verifipay.plan;

import com.verifipay.common.enums.BillingInterval;
import com.verifipay.plan.dto.CreatePlanRequest;
import com.verifipay.plan.dto.UpdatePlanRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PlanDtoValidationTest {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void createPlanRejectsInvalidCodeAndCommercialValues() {
        assertValid(new CreatePlanRequest("BASIC_1", "Basic", new BigDecimal("40.00"),
                "PEN", BillingInterval.MONTHLY, 0, 0, true));
        for (String code : new String[]{"", "basic", "Basic", "1BASIC", "BASIC-1", "BASIC 1"}) {
            assertFalse(validator.validate(new CreatePlanRequest(code, "Basic", new BigDecimal("40.00"),
                    "PEN", BillingInterval.MONTHLY, 0, 0, true)).isEmpty(), code);
        }
        assertInvalid(new CreatePlanRequest("BASIC", "Basic", BigDecimal.ZERO, "PEN", BillingInterval.MONTHLY, 0, 0, true));
        assertInvalid(new CreatePlanRequest("BASIC", "Basic", new BigDecimal("-1"), "PEN", BillingInterval.MONTHLY, 0, 0, true));
        assertInvalid(new CreatePlanRequest("BASIC", "Basic", new BigDecimal("1.234"), "PEN", BillingInterval.MONTHLY, 0, 0, true));
        for (String currency : new String[]{"pen", "PE", "PENN", "P3N"}) {
            assertFalse(validator.validate(new CreatePlanRequest("BASIC", "Basic", new BigDecimal("40.00"),
                    currency, BillingInterval.MONTHLY, 0, 0, true)).isEmpty(), currency);
        }
        assertInvalid(new CreatePlanRequest("BASIC", "Basic", new BigDecimal("40.00"), "PEN", BillingInterval.MONTHLY, -1, 0, true));
        assertInvalid(new CreatePlanRequest("BASIC", "Basic", new BigDecimal("40.00"), "PEN", BillingInterval.MONTHLY, 0, -1, true));
    }

    @Test
    void updatePlanRejectsInvalidCommercialValues() {
        assertInvalid(new UpdatePlanRequest(null, BigDecimal.ZERO, null, null, null, null, null));
        assertInvalid(new UpdatePlanRequest(null, null, "pen", null, null, null, null));
        assertInvalid(new UpdatePlanRequest(null, null, "PEN", null, -1, null, null));
        assertInvalid(new UpdatePlanRequest(null, null, "PEN", null, null, -1, null));
        assertValid(new UpdatePlanRequest("Basic", new BigDecimal("40.00"), "PEN", BillingInterval.MONTHLY, 0, 0, true));
    }

    private void assertInvalid(Object value) { assertFalse(validator.validate(value).isEmpty()); }
    private void assertValid(Object value) { assertTrue(validator.validate(value).isEmpty()); }
}
