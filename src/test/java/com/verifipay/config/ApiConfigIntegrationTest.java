package com.verifipay.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ApiConfigIntegrationTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void groupsContainOnlyTheDocumentedActorPaths() throws Exception {
        assertPaths("public", "/auth/login", "/auth/refresh", "/auth/logout");
        assertPaths("platform-admin", "/platform/companies", "/platform/companies/{id}", "/platform/companies/{companyId}/owner", "/auth/me",
                "/platform/plans", "/platform/plans/{id}", "/platform/companies/{companyId}/subscriptions",
                "/platform/companies/{companyId}/subscriptions/current", "/platform/companies/{companyId}/subscriptions/{subscriptionId}/activate",
                "/platform/companies/{companyId}/subscriptions/{subscriptionId}/mark-past-due", "/platform/companies/{companyId}/subscriptions/{subscriptionId}/cancel",
                "/platform/companies/{companyId}/subscriptions/{subscriptionId}/expire",
                "/platform/companies/{companyId}/subscriptions/{subscriptionId}/payment-orders",
                "/platform/companies/{companyId}/subscriptions/{subscriptionId}/payment-orders/{paymentOrderId}/confirm-paid");
        assertPaths("owner", "/owner/company", "/owner/branches", "/owner/branches/{id}",
                "/owner/branches/{id}/status", "/owner/cashiers", "/owner/cashiers/{id}",
                "/owner/cashiers/{id}/status", "/owner/receiver-devices", "/owner/receiver-devices/{id}/status", "/owner/dashboard",
                "/owner/audit-logs", "/owner/verifications", "/auth/me");
        assertPaths("cashier", "/payment-requests", "/payment-requests/{id}", "/payment-requests/{id}/cancel",
                "/auth/me", "/verifications", "/verifications/{id}");
        assertPaths("receiver-device", "/device/payment-events/yape");
        assertPaths("legacy-verification", "/verifications", "/verifications/{id}");
    }

    private void assertPaths(String group, String... paths) throws Exception {
        var expected = java.util.Arrays.stream(paths).map(path -> "/api/v1" + path).toArray(String[]::new);
        var result = mvc.perform(get("/v3/api-docs/" + group)).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("VerifiPay API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.paths", aMapWithSize(expected.length)));
        for (String path : expected) {
            result.andExpect(jsonPath("$.paths", hasKey(path)));
        }
    }

    @Test
    void operationSecurityMatchesAuthenticationAndDoesNotDuplicateDeviceHeader() throws Exception {
        mvc.perform(get("/v3/api-docs/public"))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").isEmpty());
        mvc.perform(get("/v3/api-docs/cashier"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/v1/payment-requests'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/auth/me'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/verifications'].post.security[0].bearerAuth").isArray());
        mvc.perform(get("/v3/api-docs/receiver-device"))
                .andExpect(jsonPath("$.components.securitySchemes.receiverDeviceToken.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.receiverDeviceToken.in").value("header"))
                .andExpect(jsonPath("$.components.securitySchemes.receiverDeviceToken.name").value("X-Device-Token"))
                .andExpect(jsonPath("$.paths['/api/v1/device/payment-events/yape'].post.security[0].receiverDeviceToken").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/device/payment-events/yape'].post.parameters").isEmpty());
    }

    @Test
    void swaggerUiAndGroupSelectorAreAvailableWithoutJwt() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.urls[*].url", containsInAnyOrder("/v3/api-docs/public", "/v3/api-docs/platform-admin",
                        "/v3/api-docs/owner", "/v3/api-docs/cashier", "/v3/api-docs/receiver-device", "/v3/api-docs/legacy-verification")));
    }

    @Test
    void documentationAccessDoesNotOpenBusinessEndpoints() throws Exception {
        for (String path : new String[]{"/auth/me", "/platform/companies", "/owner/company", "/payment-requests", "/verifications"}) {
            mvc.perform(get("/api/v1" + path)).andExpect(status().isUnauthorized());
        }
    }
}
