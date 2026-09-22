package com.verifipay.config;
import org.springframework.beans.factory.annotation.Value; import org.springframework.context.annotation.Configuration; import org.springframework.web.servlet.config.annotation.*; import java.util.Arrays;
@Configuration public class CorsConfig implements WebMvcConfigurer {
 @Value("${cors.allowed-origins:}") private String origins;
 @Override public void addCorsMappings(CorsRegistry registry){String[] values=Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toArray(String[]::new);if(values.length>0)registry.addMapping("/api/**").allowedOrigins(values).allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS").allowedHeaders("Authorization","Content-Type","Idempotency-Key").allowCredentials(false);}
}
