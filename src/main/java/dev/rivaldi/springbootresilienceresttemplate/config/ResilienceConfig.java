package dev.rivaldi.springbootresilienceresttemplate.config;

import org.springframework.context.annotation.Configuration;

/**
 * Optional programmatic customization for Resilience4j configurations.
 * These customizers override or supplement YAML configuration for specific instances.
 */
@Configuration
public class ResilienceConfig {

    /*
      Example: Programmatic CircuitBreaker customization for a specific instance.
      Uncomment and modify as needed.
     */
    // @Bean
    // public CircuitBreakerConfigCustomizer customCircuitBreakerConfig() {
    //     return CircuitBreakerConfigCustomizer.of("customApi",
    //         builder -> builder
    //             .slidingWindowSize(20)
    //             .failureRateThreshold(40.0f)
    //             .waitDurationInOpenState(Duration.ofSeconds(15))
    //     );
    // }

    /*
      Example: Programmatic Retry customization for a specific instance.
      Uncomment and modify as needed.
     */
    // @Bean
    // public RetryConfigCustomizer customRetryConfig() {
    //     return RetryConfigCustomizer.of("customApi",
    //         builder -> builder
    //             .maxAttempts(4)
    //             .waitDuration(Duration.ofMillis(500))
    //     );
    // }
}
