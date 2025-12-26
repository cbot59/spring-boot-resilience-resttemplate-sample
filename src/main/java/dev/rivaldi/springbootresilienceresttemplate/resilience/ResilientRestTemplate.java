package dev.rivaldi.springbootresilienceresttemplate.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

/**
 * A wrapper around RestTemplate that provides resilience patterns (retry and circuit breaker)
 * with the ability to enable/disable them per request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientRestTemplate {

    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    private static final String DEFAULT_INSTANCE = "default";

    // ============================================
    // Exchange methods
    // ============================================

    /**
     * Execute exchange with default resilience (retry + circuit breaker enabled)
     */
    public <T> ResponseEntity<T> exchange(
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            Class<T> responseType,
            Object... uriVariables) {

        return exchange(DEFAULT_INSTANCE, url, method, requestEntity, responseType,
                ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * Execute exchange with named resilience instance
     */
    public <T> ResponseEntity<T> exchange(
            String instanceName,
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            Class<T> responseType,
            Object... uriVariables) {

        return exchange(instanceName, url, method, requestEntity, responseType,
                ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * Execute exchange with custom resilience options (can disable retry/circuit breaker)
     */
    public <T> ResponseEntity<T> exchange(
            String instanceName,
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            Class<T> responseType,
            ResilienceOptions options,
            Object... uriVariables) {

        return executeWithResilience(
                instanceName,
                () -> restTemplate.exchange(url, method, requestEntity, responseType, uriVariables),
                options
        );
    }

    /**
     * Execute exchange with ParameterizedTypeReference for generic types
     */
    public <T> ResponseEntity<T> exchange(
            String instanceName,
            String url,
            HttpMethod method,
            HttpEntity<?> requestEntity,
            ParameterizedTypeReference<T> responseType,
            ResilienceOptions options,
            Object... uriVariables) {

        return executeWithResilience(
                instanceName,
                () -> restTemplate.exchange(url, method, requestEntity, responseType, uriVariables),
                options
        );
    }

    // ============================================
    // GET methods
    // ============================================

    /**
     * GET with default resilience
     */
    public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
        return getForObject(DEFAULT_INSTANCE, url, responseType, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * GET with named instance and default resilience
     */
    public <T> T getForObject(String instanceName, String url, Class<T> responseType, Object... uriVariables) {
        return getForObject(instanceName, url, responseType, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * GET with custom options
     */
    public <T> T getForObject(
            String instanceName,
            String url,
            Class<T> responseType,
            ResilienceOptions options,
            Object... uriVariables) {

        return executeWithResilience(
                instanceName,
                () -> restTemplate.getForObject(url, responseType, uriVariables),
                options
        );
    }

    /**
     * GET for ResponseEntity with default resilience
     */
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables) {
        return getForEntity(DEFAULT_INSTANCE, url, responseType, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * GET for ResponseEntity with custom options
     */
    public <T> ResponseEntity<T> getForEntity(
            String instanceName,
            String url,
            Class<T> responseType,
            ResilienceOptions options,
            Object... uriVariables) {

        return executeWithResilience(
                instanceName,
                () -> restTemplate.getForEntity(url, responseType, uriVariables),
                options
        );
    }

    // ============================================
    // POST methods
    // ============================================

    /**
     * POST with default resilience
     */
    public <T> T postForObject(String url, Object request, Class<T> responseType, Object... uriVariables) {
        return postForObject(DEFAULT_INSTANCE, url, request, responseType, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * POST with named instance and default resilience
     */
    public <T> T postForObject(String instanceName, String url, Object request, Class<T> responseType, Object... uriVariables) {
        return postForObject(instanceName, url, request, responseType, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * POST with custom options
     */
    public <T> T postForObject(
            String instanceName,
            String url,
            Object request,
            Class<T> responseType,
            ResilienceOptions options,
            Object... uriVariables) {

        return executeWithResilience(
                instanceName,
                () -> restTemplate.postForObject(url, request, responseType, uriVariables),
                options
        );
    }

    /**
     * POST for ResponseEntity with default resilience
     */
    public <T> ResponseEntity<T> postForEntity(String url, Object request, Class<T> responseType, Object... uriVariables) {
        return postForEntity(DEFAULT_INSTANCE, url, request, responseType, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * POST for ResponseEntity with custom options
     */
    public <T> ResponseEntity<T> postForEntity(
            String instanceName,
            String url,
            Object request,
            Class<T> responseType,
            ResilienceOptions options,
            Object... uriVariables) {

        return executeWithResilience(
                instanceName,
                () -> restTemplate.postForEntity(url, request, responseType, uriVariables),
                options
        );
    }

    // ============================================
    // PUT methods
    // ============================================

    /**
     * PUT with default resilience
     */
    public void put(String url, Object request, Object... uriVariables) {
        put(DEFAULT_INSTANCE, url, request, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * PUT with custom options
     */
    public void put(
            String instanceName,
            String url,
            Object request,
            ResilienceOptions options,
            Object... uriVariables) {

        executeWithResilience(
                instanceName,
                () -> {
                    restTemplate.put(url, request, uriVariables);
                    return null;
                },
                options
        );
    }

    // ============================================
    // DELETE methods
    // ============================================

    /**
     * DELETE with default resilience
     */
    public void delete(String url, Object... uriVariables) {
        delete(DEFAULT_INSTANCE, url, ResilienceOptions.defaults(), uriVariables);
    }

    /**
     * DELETE with custom options
     */
    public void delete(
            String instanceName,
            String url,
            ResilienceOptions options,
            Object... uriVariables) {

        executeWithResilience(
                instanceName,
                () -> {
                    restTemplate.delete(url, uriVariables);
                    return null;
                },
                options
        );
    }

    // ============================================
    // Core execution method
    // ============================================

    /**
     * Core execution method with resilience decorators.
     * Order: Retry wraps CircuitBreaker (Retry executes first, then CB)
     */
    private <T> T executeWithResilience(String instanceName, Supplier<T> supplier, ResilienceOptions options) {
        Supplier<T> decoratedSupplier = supplier;

        // Apply Circuit Breaker if enabled (inner decorator)
        if (options.isCircuitBreakerEnabled()) {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(instanceName);
            decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, decoratedSupplier);
            log.debug("Circuit breaker '{}' applied, state: {}", instanceName, circuitBreaker.getState());
        }

        // Apply Retry if enabled (outer decorator - wraps circuit breaker)
        if (options.isRetryEnabled()) {
            Retry retry = retryRegistry.retry(instanceName);
            decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
            log.debug("Retry '{}' applied", instanceName);
        }

        return decoratedSupplier.get();
    }

    /**
     * Get underlying RestTemplate for cases needing direct access
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
