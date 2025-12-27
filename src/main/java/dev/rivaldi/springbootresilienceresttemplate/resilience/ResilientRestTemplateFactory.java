package dev.rivaldi.springbootresilienceresttemplate.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Factory for creating ResilientRestTemplate instances that wrap any RestTemplate
 * with resilience patterns (retry and circuit breaker).
 *
 * <p>Usage example:
 * <pre>{@code
 * // Wrap an existing RestTemplate with resilience
 * RestTemplate customRestTemplate = new RestTemplateBuilder()
 *     .setConnectTimeout(Duration.ofSeconds(10))
 *     .build();
 *
 * ResilientRestTemplate resilientTemplate = factory.wrap(customRestTemplate);
 * resilientTemplate.getForObject("externalApi", url, String.class);
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class ResilientRestTemplateFactory {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    /**
     * Wraps the given RestTemplate with resilience patterns.
     *
     * @param restTemplate the RestTemplate to wrap
     * @return a ResilientRestTemplate that decorates the given RestTemplate
     */
    public ResilientRestTemplate wrap(RestTemplate restTemplate) {
        return new ResilientRestTemplate(restTemplate, circuitBreakerRegistry, retryRegistry);
    }
}