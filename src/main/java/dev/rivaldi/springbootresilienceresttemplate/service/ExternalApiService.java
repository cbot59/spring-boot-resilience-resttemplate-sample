package dev.rivaldi.springbootresilienceresttemplate.service;

import dev.rivaldi.springbootresilienceresttemplate.resilience.ResilientRestTemplate;
import dev.rivaldi.springbootresilienceresttemplate.resilience.ResilientRestTemplateFactory;
import dev.rivaldi.springbootresilienceresttemplate.resilience.ResilienceOptions;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Example service demonstrating different approaches to use resilience patterns.
 * Shows both programmatic (ResilientRestTemplate) and annotation-based approaches.
 */
@Slf4j
@Service
public class ExternalApiService {

    private final ResilientRestTemplate resilientRestTemplate;
    private final RestTemplate restTemplate;
    private final RestTemplate plainRestTemplate;
    private final ResilientRestTemplate customResilientRestTemplate;

    public ExternalApiService(ResilientRestTemplate resilientRestTemplate,
                               RestTemplate restTemplate,
                               @Qualifier("plainRestTemplate") RestTemplate plainRestTemplate,
                               ResilientRestTemplateFactory factory,
                               RestTemplateBuilder builder) {
        this.resilientRestTemplate = resilientRestTemplate;
        this.restTemplate = restTemplate;
        this.plainRestTemplate = plainRestTemplate;

        // Create a custom RestTemplate with different configuration and wrap it with resilience
        RestTemplate customRestTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(60))
                .build();
        this.customResilientRestTemplate = factory.wrap(customRestTemplate);
    }

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

    // ============================================
    // APPROACH 4: Using custom RestTemplate wrapped with resilience
    // ============================================

    /**
     * Call using a custom RestTemplate (different timeouts) wrapped with resilience.
     * Demonstrates using ResilientRestTemplateFactory to wrap any RestTemplate instance.
     * This custom RestTemplate has 10s connect timeout and 60s read timeout.
     */
    public String callWithCustomResilientRestTemplate(String url) {
        log.info("Calling external API with custom resilient RestTemplate: {}", url);
        return customResilientRestTemplate.getForObject("externalApi", url, String.class);
    }

    /**
     * Call using custom RestTemplate with specific resilience options.
     */
    public String callWithCustomResilientRestTemplate(String url, ResilienceOptions options) {
        log.info("Calling external API with custom resilient RestTemplate and options: {}", url);
        return customResilientRestTemplate.getForObject("externalApi", url, String.class, options);
    }
}
