package dev.rivaldi.springbootresilienceresttemplate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate beans.
 * Provides both a primary RestTemplate (used by ResilientRestTemplate)
 * and a plain RestTemplate for cases where resilience should be bypassed entirely.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${rest-template.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${rest-template.read-timeout:30000}")
    private int readTimeout;

    /**
     * Primary RestTemplate bean used by ResilientRestTemplate wrapper.
     * Configured with connection and read timeouts.
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }

    /**
     * Plain RestTemplate without any resilience wrapping.
     * Use this bean when you explicitly need to bypass retry/circuit breaker entirely.
     * Inject with @Qualifier("plainRestTemplate").
     */
    @Bean("plainRestTemplate")
    public RestTemplate plainRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeout))
                .setReadTimeout(Duration.ofMillis(readTimeout))
                .build();
    }
}
