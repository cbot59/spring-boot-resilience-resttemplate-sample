package dev.rivaldi.springbootresilienceresttemplate.exception;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for resilience-related exceptions.
 * Translates circuit breaker and retry exceptions into proper HTTP responses.
 */
@Slf4j
@RestControllerAdvice
public class ResilienceExceptionHandler {

    /**
     * Handle circuit breaker open exception.
     * Returns 503 Service Unavailable when circuit breaker is in OPEN state.
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker is open: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("message", "Service is temporarily unavailable. Please try again later.");
        body.put("circuitBreaker", ex.getCausingCircuitBreakerName());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * Handle generic runtime exceptions that may occur after retries are exhausted.
     * This catches exceptions that propagate after all retry attempts have failed.
     */
    @ExceptionHandler(org.springframework.web.client.ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAccessException(
            org.springframework.web.client.ResourceAccessException ex) {
        log.error("Resource access failed after retries: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("message", "Unable to connect to external service. Please try again later.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    /**
     * Handle HTTP server errors (5xx) from external services.
     */
    @ExceptionHandler(org.springframework.web.client.HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleHttpServerError(
            org.springframework.web.client.HttpServerErrorException ex) {
        log.error("External service returned server error: {} - {}",
                ex.getStatusCode(), ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        body.put("error", "Bad Gateway");
        body.put("message", "External service error. Please try again later.");
        body.put("originalStatus", ex.getStatusCode().value());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
