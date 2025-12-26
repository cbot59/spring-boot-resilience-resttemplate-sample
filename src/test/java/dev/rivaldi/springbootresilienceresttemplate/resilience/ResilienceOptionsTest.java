package dev.rivaldi.springbootresilienceresttemplate.resilience;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResilienceOptions Unit Tests")
class ResilienceOptionsTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("defaults() should enable both retry and circuit breaker")
        void defaultsShouldEnableBoth() {
            ResilienceOptions options = ResilienceOptions.defaults();

            assertThat(options.isRetryEnabled()).isTrue();
            assertThat(options.isCircuitBreakerEnabled()).isTrue();
        }

        @Test
        @DisplayName("none() should disable both retry and circuit breaker")
        void noneShouldDisableBoth() {
            ResilienceOptions options = ResilienceOptions.none();

            assertThat(options.isRetryEnabled()).isFalse();
            assertThat(options.isCircuitBreakerEnabled()).isFalse();
        }

        @Test
        @DisplayName("circuitBreakerOnly() should enable circuit breaker and disable retry")
        void circuitBreakerOnlyShouldEnableCbOnly() {
            ResilienceOptions options = ResilienceOptions.circuitBreakerOnly();

            assertThat(options.isCircuitBreakerEnabled()).isTrue();
            assertThat(options.isRetryEnabled()).isFalse();
        }

        @Test
        @DisplayName("retryOnly() should enable retry and disable circuit breaker")
        void retryOnlyShouldEnableRetryOnly() {
            ResilienceOptions options = ResilienceOptions.retryOnly();

            assertThat(options.isRetryEnabled()).isTrue();
            assertThat(options.isCircuitBreakerEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("Builder should create options with default values")
        void builderShouldUseDefaultValues() {
            ResilienceOptions options = ResilienceOptions.builder().build();

            assertThat(options.isRetryEnabled()).isTrue();
            assertThat(options.isCircuitBreakerEnabled()).isTrue();
        }

        @Test
        @DisplayName("Builder should allow customizing retry enabled")
        void builderShouldCustomizeRetry() {
            ResilienceOptions options = ResilienceOptions.builder()
                    .retryEnabled(false)
                    .build();

            assertThat(options.isRetryEnabled()).isFalse();
            assertThat(options.isCircuitBreakerEnabled()).isTrue();
        }

        @Test
        @DisplayName("Builder should allow customizing circuit breaker enabled")
        void builderShouldCustomizeCircuitBreaker() {
            ResilienceOptions options = ResilienceOptions.builder()
                    .circuitBreakerEnabled(false)
                    .build();

            assertThat(options.isRetryEnabled()).isTrue();
            assertThat(options.isCircuitBreakerEnabled()).isFalse();
        }

        @Test
        @DisplayName("Builder should allow customizing both options")
        void builderShouldCustomizeBoth() {
            ResilienceOptions options = ResilienceOptions.builder()
                    .retryEnabled(false)
                    .circuitBreakerEnabled(false)
                    .build();

            assertThat(options.isRetryEnabled()).isFalse();
            assertThat(options.isCircuitBreakerEnabled()).isFalse();
        }
    }
}
