package com.codex.finance.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleApi_returnsCorrectStatus() {
        ApiException ex = new ApiException(HttpStatus.NOT_FOUND, "not found");
        ResponseEntity<Map<String, Object>> resp = handler.handleApi(ex);
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertEquals("not_found", resp.getBody().get("error"));
        assertEquals("not found", resp.getBody().get("message"));
    }

    @Test
    void handleApi_returnsConflict() {
        ApiException ex = new ApiException(HttpStatus.CONFLICT, "conflict");
        ResponseEntity<Map<String, Object>> resp = handler.handleApi(ex);
        assertEquals("conflict", resp.getBody().get("error"));
    }

    @Test
    void handleApi_returnsUnauthorized() {
        ApiException ex = new ApiException(HttpStatus.UNAUTHORIZED, "unauth");
        ResponseEntity<Map<String, Object>> resp = handler.handleApi(ex);
        assertEquals("unauthorized", resp.getBody().get("error"));
    }

    @Test
    void handleApi_returnsForbidden() {
        ApiException ex = new ApiException(HttpStatus.FORBIDDEN, "forbidden");
        ResponseEntity<Map<String, Object>> resp = handler.handleApi(ex);
        assertEquals("forbidden", resp.getBody().get("error"));
    }

    @Test
    void handleApi_returnsBadRequest_forOther4xx() {
        ApiException ex = new ApiException(HttpStatus.BAD_REQUEST, "bad");
        ResponseEntity<Map<String, Object>> resp = handler.handleApi(ex);
        assertEquals("bad_request", resp.getBody().get("error"));
    }

    @Test
    void handleApi_returnsMessage_whenNull() {
        ApiException ex = new ApiException(HttpStatus.BAD_REQUEST, null);
        ResponseEntity<Map<String, Object>> resp = handler.handleApi(ex);
        assertNull(resp.getBody().get("message"));
    }

    @Test
    void handleValidation_returnsBadRequestWithFields() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
            new FieldError("obj", "email", "Email is required"),
            new FieldError("obj", "password", "Password too short")
        ));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("validation_failed", resp.getBody().get("error"));
        assertTrue(resp.getBody().containsKey("fields"));
        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) resp.getBody().get("fields");
        assertEquals("Email is required", fields.get("email"));
    }

    @Test
    void handleTypeMismatch_returnsBadRequest() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("userId");
        ResponseEntity<Map<String, Object>> resp = handler.handleTypeMismatch(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("invalid_parameter", resp.getBody().get("error"));
    }

    @Test
    void handleTypeMismatch_returnsBadRequest_withUnknownParam() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn(null);
        ResponseEntity<Map<String, Object>> resp = handler.handleTypeMismatch(ex);
        assertEquals("invalid_parameter", resp.getBody().get("error"));
    }

    @Test
    void handleNotReadable_returnsBadRequest() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(new Throwable("invalid format"));
        ResponseEntity<Map<String, Object>> resp = handler.handleNotReadable(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("invalid_payload", resp.getBody().get("error"));
    }

    @Test
    void handleNotReadable_returnsGenericMessage_whenNull() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(null);
        when(ex.getMessage()).thenReturn(null);
        ResponseEntity<Map<String, Object>> resp = handler.handleNotReadable(ex);
        assertEquals("invalid_payload", resp.getBody().get("error"));
        assertTrue(((String) resp.getBody().get("message")).contains("no es válido"));
    }

    @Test
    void handleNotReadable_returnsGenericMessage_whenInvalidFormat() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMostSpecificCause()).thenReturn(new Throwable("Cannot deserialize value of type"));
        ResponseEntity<Map<String, Object>> resp = handler.handleNotReadable(ex);
        assertEquals("invalid_payload", resp.getBody().get("error"));
        assertTrue(((String) resp.getBody().get("message")).contains("no es válido"));
    }

    @Test
    void handleDataIntegrityViolation_returnsBadRequest() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Some error");
        ResponseEntity<Map<String, Object>> resp = handler.handleDataIntegrityViolation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("data_integrity", resp.getBody().get("error"));
    }

    @Test
    void handleDataIntegrityViolation_returnsEnumMessage_whenEnum() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("invalid input value for enum");
        ResponseEntity<Map<String, Object>> resp = handler.handleDataIntegrityViolation(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("valores seleccionados"));
    }

    @Test
    void handleWebClient_returnsUnauthorized_for4xx() {
        WebClientResponseException ex = WebClientResponseException.create(
            401, "Unauthorized", null, "{\"error\":\"invalid login credentials\"}".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("external_request_error", resp.getBody().get("error"));
    }

    @Test
    void handleWebClient_returnsServerError_for5xx() {
        WebClientResponseException ex = WebClientResponseException.create(
            503, "Service Unavailable", null, "".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, resp.getStatusCode());
        assertEquals("external_service_error", resp.getBody().get("error"));
    }

    @Test
    void handleOther_returnsInternalServerError() {
        Exception ex = new RuntimeException("unexpected");
        ResponseEntity<Map<String, Object>> resp = handler.handleOther(ex);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("internal_error", resp.getBody().get("error"));
    }

    @Test
    void handleOther_returnsDefaultMessage() {
        Exception ex = new RuntimeException();
        ResponseEntity<Map<String, Object>> resp = handler.handleOther(ex);
        assertEquals("internal_error", resp.getBody().get("error"));
        assertEquals("Ocurrió un error inesperado. Intenta nuevamente.", resp.getBody().get("message"));
    }

    @Test
    void handleWebClient_withInvalidLoginCredentials() {
        WebClientResponseException ex = WebClientResponseException.create(
            401, "Unauthorized", null, "{\"msg\":\"invalid login credentials\"}".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("incorrectos"));
    }

    @Test
    void handleWebClient_withEmailNotConfirmed() {
        WebClientResponseException ex = WebClientResponseException.create(
            401, "Unauthorized", null, "email not confirmed".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("confirmar"));
    }

    @Test
    void handleWebClient_alreadyRegistered() {
        WebClientResponseException ex = WebClientResponseException.create(
            422, "Unprocessable", null, "User already registered".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("registrado"));
    }

    @Test
    void handleWebClient_signupDisabled() {
        WebClientResponseException ex = WebClientResponseException.create(
            403, "Forbidden", null, "signup disabled".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("deshabilitado"));
    }

    @Test
    void handleWebClient_rateLimited() {
        WebClientResponseException ex = WebClientResponseException.create(
            429, "Too Many Requests", null, "rate limit exceeded".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("tarde"));
    }

    @Test
    void handleWebClient_tokenExpired() {
        WebClientResponseException ex = WebClientResponseException.create(
            401, "Unauthorized", null, "token has expired".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("sesión"));
    }

    @Test
    void handleWebClient_fallbackFor403() {
        WebClientResponseException ex = WebClientResponseException.create(
            403, "Forbidden", null, "some other error".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("permisos"));
    }

    @Test
    void handleWebClient_fallbackFor500() {
        WebClientResponseException ex = WebClientResponseException.create(
            500, "Internal Server Error", null, "".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("disponible"));
    }

    @Test
    void handleWebClient_genericFallback() {
        WebClientResponseException ex = WebClientResponseException.create(
            302, "Found", null, "".getBytes(StandardCharsets.UTF_8), null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("autenticación"));
    }

    @Test
    void handleWebClient_nullBody() {
        WebClientResponseException ex = WebClientResponseException.create(
            401, "Unauthorized", null, (byte[]) null, null
        );
        ResponseEntity<Map<String, Object>> resp = handler.handleWebClient(ex);
        assertTrue(((String) resp.getBody().get("message")).contains("incorrectos"));
    }
}
