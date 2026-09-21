package com.verifipay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ApiConfig {
    private static final String DESCRIPTION = "VerifiPay es una plataforma para negocios que gestiona cobros esperados "
            + "y pagos Yape detectados mediante dispositivos receptores asociados a cada empresa. "
            + "Permite coordinar cobros entre usuarios, cajeros y dispositivos de forma aislada por negocio. "
            + "Las notificaciones detectadas no constituyen una confirmacion independiente de una API bancaria.";
    private static final String BEARER = "bearerAuth";
    private static final String DEVICE = "receiverDeviceToken";

    @Bean
    public OpenAPI verifiPayOpenApi() {
        return new OpenAPI().info(new Info().title("VerifiPay API").version("v1").description(DESCRIPTION))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT").description("JWT de un usuario autenticado de VerifiPay."))
                        .addSecuritySchemes(DEVICE, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-Device-Token")
                                .description("Token del dispositivo receptor activo; no es un JWT de usuario.")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return group("public", "Public / Auth", "Inicio de sesion sin autenticacion previa.",
                "/api/v1/auth/login");
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return group("admin", "ADMIN", "Administracion de empresas, locales, usuarios, dispositivos, "
                        + "dashboard y auditoria. Incluye el endpoint existente de pagos simulados, el perfil propio "
                        + "y Verification legacy compartido con usuarios autenticados.",
                "/api/v1/companies/**", "/api/v1/branches/**", "/api/v1/users/**",
                "/api/v1/admin/**", "/api/v1/payment-events/mock/**",
                "/api/v1/auth/me", "/api/v1/verifications/**");
    }

    @Bean
    public GroupedOpenApi cashierApi() {
        return group("cashier", "CASHIER", "Creacion, consulta y cancelacion de cobros propios; requiere "
                        + "cajero y negocio activos. Incluye el perfil propio y Verification legacy.",
                "/api/v1/payment-requests", "/api/v1/payment-requests/**",
                "/api/v1/auth/me", "/api/v1/verifications/**");
    }

    @Bean
    public GroupedOpenApi receiverDeviceApi() {
        return group("receiver-device", "Receiver Device", "Recepcion de notificaciones Yape mediante "
                        + "X-Device-Token. El servicio valida el dispositivo, negocio y local activos.",
                "/api/v1/device/payment-events/yape");
    }

    @Bean
    public GroupedOpenApi legacyVerificationApi() {
        return group("legacy-verification", "Legacy Verification", "Flujo legacy separado del matching de "
                        + "PaymentRequest. SecurityConfig exige JWT, sin restriccion de rol adicional: ADMIN y CASHIER "
                        + "pueden acceder. Crear una verificacion requiere un local asignado; el historial es propio. "
                        + "Solo consume eventos legacy con matching_status NULL.",
                "/api/v1/verifications/**");
    }

    private GroupedOpenApi group(String name, String displayName, String description, String... paths) {
        return GroupedOpenApi.builder().group(name).displayName(displayName).pathsToMatch(paths)
                .addOpenApiCustomizer(api -> api.setInfo(new Info().title("VerifiPay API").version("v1")
                        .description(DESCRIPTION + "\n\n" + description)))
                .build();
    }

    @Bean
    public GlobalOpenApiCustomizer operationSecurity() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
            if (path.equals("/api/v1/auth/login")) {
                operation.setSecurity(List.of());
            } else if (path.equals("/api/v1/device/payment-events/yape")) {
                operation.setSecurity(List.of(new SecurityRequirement().addList(DEVICE)));
                // Swagger UI supplies this header through Authorize, not a second parameter field.
                if (operation.getParameters() != null) {
                    operation.getParameters().removeIf(parameter -> "header".equals(parameter.getIn())
                            && "X-Device-Token".equalsIgnoreCase(parameter.getName()));
                }
            } else {
                operation.setSecurity(List.of(new SecurityRequirement().addList(BEARER)));
            }
        }));
    }
}
