package org.familychoregame.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> apiError(ApiException exception) {
        LOGGER.atWarn()
                .addKeyValue("event", "api_request_rejected")
                .addKeyValue("error_code", exception.code())
                .addKeyValue("http_status", exception.status().value())
                .log("API request rejected");
        return response(exception.status(), exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validationError(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");
        LOGGER.atWarn()
                .addKeyValue("event", "api_request_rejected")
                .addKeyValue("error_code", "validation_failed")
                .addKeyValue("http_status", HttpStatus.BAD_REQUEST.value())
                .log("API request validation failed");
        return response(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, Object>> malformedRequest(Exception exception) {
        LOGGER.atWarn()
                .addKeyValue("event", "api_request_rejected")
                .addKeyValue("error_code", "malformed_request")
                .addKeyValue("error_type", exception.getClass().getSimpleName())
                .addKeyValue("http_status", HttpStatus.BAD_REQUEST.value())
                .log("Malformed API request rejected");
        return response(HttpStatus.BAD_REQUEST, "malformed_request", "The request could not be parsed");
    }

    @ExceptionHandler(UnableToExecuteStatementException.class)
    ResponseEntity<Map<String, Object>> databaseConflict(UnableToExecuteStatementException exception) {
        if (exception.getMessage() != null && exception.getMessage().contains("SQLITE_CONSTRAINT")) {
            LOGGER.atWarn()
                    .addKeyValue("event", "api_request_rejected")
                    .addKeyValue("error_code", "constraint_conflict")
                    .addKeyValue("http_status", HttpStatus.CONFLICT.value())
                    .log("Database constraint rejected API request");
            return response(HttpStatus.CONFLICT, "constraint_conflict",
                    "The member or chore is already assigned on that date");
        }
        return unexpectedError(exception);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> unexpectedError(Exception exception) {
        LOGGER.atError()
                .addKeyValue("event", "unexpected_api_error")
                .addKeyValue("error_type", exception.getClass().getSimpleName())
                .addKeyValue("http_status", HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setCause(exception)
                .log("Unexpected API error");
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String code, String message) {
        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", code);
        body.put("message", message);
        var requestId = MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY);
        if (requestId != null) body.put("requestId", requestId);
        return ResponseEntity.status(status).body(body);
    }
}
