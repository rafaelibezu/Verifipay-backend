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
import org.springframework.web.context.WebApplicationContext;

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

    @Test void productionDefaultsAreSafeAndContextStarts() {
        assertEquals("false", environment.getProperty("springdoc.api-docs.enabled"));
        assertEquals("false", environment.getProperty("springdoc.swagger-ui.enabled"));
        assertEquals("false", environment.getProperty("billing.manual.enabled"));
        assertEquals("false", environment.getProperty("platform.bootstrap.enabled"));
        assertEquals("health", environment.getProperty("management.endpoints.web.exposure.include"));
        assertEquals("never", environment.getProperty("management.endpoint.health.show-details"));
        assertEquals("never", environment.getProperty("server.error.include-stacktrace"));
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
}
