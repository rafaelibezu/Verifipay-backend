package com.verifipay.config;

import com.verifipay.billing.controller.ManualPaymentOrderController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "DB_URL=jdbc:postgresql://127.0.0.1:55440/verifipay_paymentrequest_test",
        "DB_USERNAME=verifipay_test",
        "DB_PASSWORD=",
        "JWT_SECRET=test-only-production-profile-secret-0123456789",
        "CORS_ALLOWED_ORIGINS=https://frontend.test"
})
class ProductionProfileIntegrationTest {
    @Autowired Environment environment;
    @Autowired WebApplicationContext context;

    private MockMvc mvc() {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test void productionDefaultsAreSafeAndContextStarts() {
        assertEquals("false", environment.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", environment.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("false", environment.getProperty("billing.manual.enabled"));
        assertEquals("false", environment.getProperty("platform.bootstrap.enabled"));
        assertEquals("health", environment.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("never", environment.getProperty("management.endpoint.health.show-details"));
        assertEquals("never", environment.getProperty("server.error.include-stacktrace"));
        assertEquals("graceful", environment.getProperty("server.shutdown"));
        assertEquals("25s", environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase"));
        assertEquals(0, context.getBeansOfType(ManualPaymentOrderController.class).size());
    }

    @Test void developmentProfileKeepsOpenApiEnabled() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-dev.yaml"));
        assertEquals("true", yaml.getObject().getProperty("springdoc.api-docs.enabled"));
        assertEquals("true", yaml.getObject().getProperty("springdoc.swagger-ui.enabled"));
    }

    @Test void swaggerAndManualBillingAreDisabledInProduction() {
        assertEquals("false", environment.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", environment.getProperty("springdoc.swagger-ui.enabled"));
        assertFalse(context.containsBean("manualPaymentOrderController"));
    }

    @Test
    @WithMockUser
    void nonexistentResourcesReturnNotFoundInProduction() throws Exception {
        for (String path : new String[]{"/resource-that-does-not-exist", "/swagger-ui/index.html", "/v3/api-docs"}) {
            mvc().perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.message").value("Recurso no encontrado"));
        }
    }
}
