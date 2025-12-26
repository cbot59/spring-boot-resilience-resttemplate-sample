package dev.rivaldi.springbootresilienceresttemplate.resilience;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration options for per-request resilience behavior.
 * Allows enabling/disabling retry and circuit breaker on a per-request basis.
 */
@Getter
@Builder
public class ResilienceOptions {

    @Builder.Default
    private boolean retryEnabled = true;

    @Builder.Default
    private boolean circuitBreakerEnabled = true;

    /**
     * Default options: both retry and circuit breaker enabled
     */
    public static ResilienceOptions defaults() {
        return ResilienceOptions.builder().build();
    }

    /**
     * No resilience: both retry and circuit breaker disabled
     */
    public static ResilienceOptions none() {
        return ResilienceOptions.builder()
                .retryEnabled(false)
                .circuitBreakerEnabled(false)
                .build();
    }

    /**
     * Only circuit breaker, no retry
     */
    public static ResilienceOptions circuitBreakerOnly() {
        return ResilienceOptions.builder()
                .retryEnabled(false)
                .circuitBreakerEnabled(true)
                .build();
    }

    /**
     * Only retry, no circuit breaker
     */
    public static ResilienceOptions retryOnly() {
        return ResilienceOptions.builder()
                .retryEnabled(true)
                .circuitBreakerEnabled(false)
                .build();
    }
}
