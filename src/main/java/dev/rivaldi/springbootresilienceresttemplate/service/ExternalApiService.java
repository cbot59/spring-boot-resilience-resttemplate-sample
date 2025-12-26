package dev.rivaldi.springbootresilienceresttemplate.service;

import dev.rivaldi.springbootresilienceresttemplate.resilience.ResilientRestTemplate;
import dev.rivaldi.springbootresilienceresttemplate.resilience.ResilienceOptions;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Example service demonstrating different approaches to use resilience patterns.
 * Shows both programmatic (ResilientRestTemplate) and annotation-based approaches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalApiService {

    private final ResilientRestTemplate resilientRestTemplate;
    private final RestTemplate restTemplate;

    // Also inject the plain RestTemplate for cases where resilience should be bypassed
    @Qualifier("plainRestTemplate")
    private final RestTemplate plainRestTemplate;

    // ============================================
    // APPROACH 1: Using ResilientRestTemplate (Programmatic)
    // ============================================

    /**
     * Call with default resilience (both retry and circuit breaker enabled)
     */
    public String callWithDefaultResilience(String url) {
        log.info("Calling external API with default resilience: {}", url);
        return resilientRestTemplate.getForObject(url, String.class);
    }

    /**
     * Call with named instance configuration
     */
    public String callWithNamedInstance(String url) {
        log.info("Calling external API with 'externalApi' instance: {}", url);
        return resilientRestTemplate.getForObject("externalApi", url, String.class);
    }

    /**
     * Call with circuit breaker only (no retry)
     */
    public String callWithCircuitBreakerOnly(String url) {
        log.info("Calling external API with circuit breaker only: {}", url);
        return resilientRestTemplate.getForObject(
                "externalApi",
                url,
                String.class,
                ResilienceOptions.circuitBreakerOnly()
        );
    }

    /**
     * Call with retry only (no circuit breaker)
     */
    public String callWithRetryOnly(String url) {
        log.info("Calling external API with retry only: {}", url);
        return resilientRestTemplate.getForObject(
                "externalApi",
                url,
                String.class,
                ResilienceOptions.retryOnly()
        );
    }

    /**
     * Call without any resilience (bypass entirely)
     */
    public String callWithoutResilience(String url) {
        log.info("Calling external API without resilience: {}", url);
        return resilientRestTemplate.getForObject(
                "externalApi",
                url,
                String.class,
                ResilienceOptions.none()
        );
    }

    /**
     * POST with default resilience
     */
    public ResponseEntity<String> postWithResilience(String url, Object requestBody) {
        log.info("POST to external API with resilience: {}", url);
        return resilientRestTemplate.exchange(
                "externalApi",
                url,
                HttpMethod.POST,
                new HttpEntity<>(requestBody),
                String.class,
                ResilienceOptions.defaults()
        );
    }

    // ============================================
    // APPROACH 2: Using Annotations (Declarative)
    // ============================================

    /**
     * Annotation-based approach with fallback.
     * Retry wraps CircuitBreaker (Retry executes first, then CB).
     */
    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackGetData")
    @Retry(name = "externalApi")
    public String getDataWithAnnotations(String url) {
        log.info("Calling external API with annotations: {}", url);
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * Fallback method - must have same signature + Throwable parameter
     */
    private String fallbackGetData(String url, Throwable t) {
        log.warn("Fallback triggered for URL: {}, reason: {}", url, t.getMessage());
        return "{\"status\": \"fallback\", \"message\": \"Service temporarily unavailable\"}";
    }

    // ============================================
    // APPROACH 3: Using plain RestTemplate (No resilience)
    // ============================================

    /**
     * Direct call using plain RestTemplate - completely bypasses resilience.
     * Use this for calls where you explicitly don't want any retry or circuit breaker.
     */
    public String callWithPlainRestTemplate(String url) {
        log.info("Calling external API with plain RestTemplate: {}", url);
        return plainRestTemplate.getForObject(url, String.class);
    }
}
