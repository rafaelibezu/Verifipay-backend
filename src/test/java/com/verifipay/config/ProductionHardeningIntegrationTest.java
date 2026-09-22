package com.verifipay.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:4200")
class ProductionHardeningIntegrationTest {
    @Autowired WebApplicationContext context;

    private MockMvc mvc() { return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    @Test void healthIsPublicAndOtherActuatorEndpointsAreNotPublic() throws Exception {
        mvc().perform(get("/actuator/health")).andExpect(status().isOk());
        mvc().perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test void corsAllowsOnlyConfiguredOriginAndRequiredPaymentOrderHeader() throws Exception {
        mvc().perform(options("/api/v1/platform/companies/1/subscriptions/1/payment-orders")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization, Content-Type, Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Idempotency-Key")));
        mvc().perform(options("/api/v1/platform/companies/1/subscriptions/1/payment-orders")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
