package com.codex.finance.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(errorBody(codeForStatus(ex.getStatus()), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> fields.put(err.getField(), err.getDefaultMessage()));
        String firstMessage = fields.values().stream().findFirst().orElse("Revisa los campos marcados.");
        Map<String, Object> body = errorBody("validation_failed", firstMessage);
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName() != null ? ex.getName() : "unknown";
        return ResponseEntity.badRequest().body(errorBody(
            "invalid_parameter",
            "El valor de '" + paramName + "' no es válido."
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        String raw = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        return ResponseEntity.badRequest().body(errorBody("invalid_payload", friendlyPayloadMessage(raw)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrity(DataIntegrityViolationException ex) {
        String raw = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        String message = friendlyIntegrityMessage(raw);
        HttpStatus status = message.startsWith("Ya existe") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(errorBody("data_integrity_error", message));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClient(WebClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        String message = friendlyAuthMessage(ex.getStatusCode().value(), body);
        String error = ex.getStatusCode().is4xxClientError() ? "external_request_error" : "external_service_error";
        return ResponseEntity.status(ex.getStatusCode()).body(errorBody(error, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        log.error("Unhandled backend error", ex);
        return ResponseEntity.internalServerError().body(errorBody("internal_error", "Ocurrió un error inesperado. Intenta nuevamente."));
    }

    private Map<String, Object> errorBody(String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error == null ? "internal_error" : error);
        if (message != null && !message.isBlank()) {
            body.put("message", message);
        }
        return body;
    }

    private String codeForStatus(HttpStatus status) {
        if (status == null) return "internal_error";
        if (status == HttpStatus.NOT_FOUND) return "not_found";
        if (status == HttpStatus.CONFLICT) return "conflict";
        if (status == HttpStatus.UNAUTHORIZED) return "unauthorized";
        if (status == HttpStatus.FORBIDDEN) return "forbidden";
        if (status.is4xxClientError()) return "bad_request";
        return "internal_error";
    }

    private String friendlyPayloadMessage(String raw) {
        if (raw == null) return "El cuerpo de la solicitud no es válido.";
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("cannot deserialize value of type") || lower.contains("invalid format")) {
            return "Uno de los valores seleccionados no es válido.";
        }
        return "El cuerpo de la solicitud no es válido.";
    }

    private String friendlyIntegrityMessage(String raw) {
        if (raw == null) {
            return "No se pudo guardar la información. Verifica los datos.";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("duplicate key value violates unique constraint")) {
            return "Ya existe un registro con esos datos.";
        }
        if (lower.contains("invalid input value for enum") || lower.contains("is not a valid input value for enum")) {
            return "Uno de los valores seleccionados no es válido.";
        }
        if (lower.contains("foreign key") || lower.contains("violates foreign key constraint")) {
            return "Selecciona un registro relacionado válido.";
        }
        if (lower.contains("null value in column") || lower.contains("not-null violation")) {
            return "Completa los campos obligatorios.";
        }
        if (lower.contains("check constraint") || lower.contains("violates check constraint")) {
            return "Uno de los campos no cumple con las reglas de validación.";
        }
        return "No se pudo guardar la información. Verifica los datos.";
    }

    private String friendlyAuthMessage(int status, String raw) {
        String lower = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        if (lower.contains("invalid login credentials") || lower.contains("bad credentials")) {
            return "Usuario y/o contraseña incorrectos.";
        }
        if (lower.contains("already registered") || lower.contains("already exists") || lower.contains("already been taken")) {
            return "El correo ya está registrado.";
        }
        if (lower.contains("email not confirmed")) {
            return "Debes confirmar tu correo antes de iniciar sesión.";
        }
        if (lower.contains("signup disabled")) {
            return "El registro está deshabilitado temporalmente.";
        }
        if (lower.contains("rate limit") || lower.contains("too many requests")) {
            return "Demasiados intentos. Intenta más tarde.";
        }
        if (lower.contains("token") && lower.contains("expired")) {
            return "Tu sesión expiró. Inicia sesión de nuevo.";
        }
        if (status == HttpStatus.UNAUTHORIZED.value()) {
            return "Usuario y/o contraseña incorrectos.";
        }
        if (status == HttpStatus.FORBIDDEN.value()) {
            return "No tienes permisos para realizar esta acción.";
        }
        if (status >= 500) {
            return "El servicio de autenticación no está disponible en este momento.";
        }
        return "No se pudo completar la autenticación.";
    }
}
