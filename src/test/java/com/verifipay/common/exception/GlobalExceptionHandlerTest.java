package com.verifipay.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingStaticResourcesReturnNotFoundWithoutInternalDetails() {
        var response = handler.handleStaticResourceNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/missing-resource", "No static resource")
        );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Recurso no encontrado", response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void unexpectedExceptionsRemainInternalServerErrors() {
        var response = handler.handleGeneric(new IllegalStateException("internal detail"));

        assertEquals(500, response.getStatusCode().value());
        assertNotEquals("internal detail", response.getBody().message());
        assertNull(response.getBody().data());
    }
}
