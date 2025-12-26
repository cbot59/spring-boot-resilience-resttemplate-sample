package dev.rivaldi.springbootresilienceresttemplate.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResilientRestTemplate Unit Tests")
class ResilientRestTemplateTest {

    @Mock
    private RestTemplate restTemplate;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RetryRegistry retryRegistry;
    private ResilientRestTemplate resilientRestTemplate;

    @BeforeEach
    void setUp() {
        // Create real registries with custom configurations for testing
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(100))
                .retryExceptions(HttpServerErrorException.class)
                .build();
        retryRegistry = RetryRegistry.of(retryConfig);

        resilientRestTemplate = new ResilientRestTemplate(
                restTemplate,
                circuitBreakerRegistry,
                retryRegistry
        );
    }

    @Nested
    @DisplayName("getForObject tests")
    class GetForObjectTests {

        @Test
        @DisplayName("Should return response when call succeeds with default resilience")
        void shouldReturnResponseOnSuccess() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn("success");

            String result = resilientRestTemplate.getForObject(
                    "http://test.com/api",
                    String.class
            );

            assertThat(result).isEqualTo("success");
            verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
        }

        @Test
        @DisplayName("Should return response when using named instance")
        void shouldReturnResponseWithNamedInstance() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn("named-success");

            String result = resilientRestTemplate.getForObject(
                    "externalApi",
                    "http://test.com/api",
                    String.class
            );

            assertThat(result).isEqualTo("named-success");
        }

        @Test
        @DisplayName("Should retry on failure and succeed on retry")
        void shouldRetryOnFailureAndSucceed() {
            AtomicInteger attempts = new AtomicInteger(0);
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenAnswer(invocation -> {
                        if (attempts.incrementAndGet() < 3) {
                            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        return "success-after-retry";
                    });

            String result = resilientRestTemplate.getForObject(
                    "http://test.com/api",
                    String.class
            );

            assertThat(result).isEqualTo("success-after-retry");
            assertThat(attempts.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should throw exception after max retries exceeded")
        void shouldThrowAfterMaxRetries() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> resilientRestTemplate.getForObject(
                    "http://test.com/api",
                    String.class
            )).isInstanceOf(HttpServerErrorException.class);

            verify(restTemplate, times(3)).getForObject(anyString(), eq(String.class));
        }

        @Test
        @DisplayName("Should not retry when resilience is disabled")
        void shouldNotRetryWhenResilienceDisabled() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> resilientRestTemplate.getForObject(
                    "default",
                    "http://test.com/api",
                    String.class,
                    ResilienceOptions.none()
            )).isInstanceOf(HttpServerErrorException.class);

            // Should only be called once - no retry
            verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
        }

        @Test
        @DisplayName("Should apply only circuit breaker when retry is disabled")
        void shouldApplyOnlyCircuitBreaker() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn("cb-only-success");

            String result = resilientRestTemplate.getForObject(
                    "default",
                    "http://test.com/api",
                    String.class,
                    ResilienceOptions.circuitBreakerOnly()
            );

            assertThat(result).isEqualTo("cb-only-success");
        }

        @Test
        @DisplayName("Should apply only retry when circuit breaker is disabled")
        void shouldApplyOnlyRetry() {
            AtomicInteger attempts = new AtomicInteger(0);
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenAnswer(invocation -> {
                        if (attempts.incrementAndGet() < 2) {
                            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                        return "retry-only-success";
                    });

            String result = resilientRestTemplate.getForObject(
                    "default",
                    "http://test.com/api",
                    String.class,
                    ResilienceOptions.retryOnly()
            );

            assertThat(result).isEqualTo("retry-only-success");
            assertThat(attempts.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("exchange tests")
    class ExchangeTests {

        @Test
        @DisplayName("Should execute exchange with default resilience")
        void shouldExecuteExchangeWithDefaultResilience() {
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("exchange-success");
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(expectedResponse);

            ResponseEntity<String> result = resilientRestTemplate.exchange(
                    "http://test.com/api",
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    String.class
            );

            assertThat(result.getBody()).isEqualTo("exchange-success");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should execute exchange with named instance")
        void shouldExecuteExchangeWithNamedInstance() {
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("named-exchange");
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(expectedResponse);

            ResponseEntity<String> result = resilientRestTemplate.exchange(
                    "externalApi",
                    "http://test.com/api",
                    HttpMethod.POST,
                    new HttpEntity<>("request-body"),
                    String.class
            );

            assertThat(result.getBody()).isEqualTo("named-exchange");
        }

        @Test
        @DisplayName("Should execute exchange with custom options")
        void shouldExecuteExchangeWithCustomOptions() {
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("custom-options");
            when(restTemplate.exchange(
                    anyString(),
                    eq(HttpMethod.PUT),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(expectedResponse);

            ResponseEntity<String> result = resilientRestTemplate.exchange(
                    "externalApi",
                    "http://test.com/api",
                    HttpMethod.PUT,
                    new HttpEntity<>("update-body"),
                    String.class,
                    ResilienceOptions.none()
            );

            assertThat(result.getBody()).isEqualTo("custom-options");
        }
    }

    @Nested
    @DisplayName("postForObject tests")
    class PostForObjectTests {

        @Test
        @DisplayName("Should post with default resilience")
        void shouldPostWithDefaultResilience() {
            when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                    .thenReturn("post-success");

            String result = resilientRestTemplate.postForObject(
                    "http://test.com/api",
                    "request-body",
                    String.class
            );

            assertThat(result).isEqualTo("post-success");
        }

        @Test
        @DisplayName("Should retry POST on failure")
        void shouldRetryPostOnFailure() {
            AtomicInteger attempts = new AtomicInteger(0);
            when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                    .thenAnswer(invocation -> {
                        if (attempts.incrementAndGet() < 2) {
                            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
                        }
                        return "post-retry-success";
                    });

            String result = resilientRestTemplate.postForObject(
                    "http://test.com/api",
                    "request-body",
                    String.class
            );

            assertThat(result).isEqualTo("post-retry-success");
            assertThat(attempts.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("postForEntity tests")
    class PostForEntityTests {

        @Test
        @DisplayName("Should return ResponseEntity on successful post")
        void shouldReturnResponseEntityOnSuccess() {
            ResponseEntity<String> expectedResponse = ResponseEntity.status(HttpStatus.CREATED).body("created");
            when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<String> result = resilientRestTemplate.postForEntity(
                    "http://test.com/api",
                    "new-resource",
                    String.class
            );

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isEqualTo("created");
        }
    }

    @Nested
    @DisplayName("getForEntity tests")
    class GetForEntityTests {

        @Test
        @DisplayName("Should return ResponseEntity on successful get")
        void shouldReturnResponseEntityOnSuccess() {
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("entity-success");
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(expectedResponse);

            ResponseEntity<String> result = resilientRestTemplate.getForEntity(
                    "http://test.com/api",
                    String.class
            );

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo("entity-success");
        }
    }

    @Nested
    @DisplayName("put tests")
    class PutTests {

        @Test
        @DisplayName("Should execute put with default resilience")
        void shouldExecutePutWithDefaultResilience() {
            doNothing().when(restTemplate).put(anyString(), any());

            resilientRestTemplate.put("http://test.com/api", "update-body");

            verify(restTemplate, times(1)).put(anyString(), any());
        }

        @Test
        @DisplayName("Should retry put on failure")
        void shouldRetryPutOnFailure() {
            AtomicInteger attempts = new AtomicInteger(0);
            doAnswer(invocation -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return null;
            }).when(restTemplate).put(anyString(), any());

            resilientRestTemplate.put("http://test.com/api", "update-body");

            assertThat(attempts.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("delete tests")
    class DeleteTests {

        @Test
        @DisplayName("Should execute delete with default resilience")
        void shouldExecuteDeleteWithDefaultResilience() {
            doNothing().when(restTemplate).delete(anyString());

            resilientRestTemplate.delete("http://test.com/api/1");

            verify(restTemplate, times(1)).delete(anyString());
        }

        @Test
        @DisplayName("Should retry delete on failure")
        void shouldRetryDeleteOnFailure() {
            AtomicInteger attempts = new AtomicInteger(0);
            doAnswer(invocation -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return null;
            }).when(restTemplate).delete(anyString());

            resilientRestTemplate.delete("http://test.com/api/1");

            assertThat(attempts.get()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker behavior tests")
    class CircuitBreakerTests {

        @Test
        @DisplayName("Circuit breaker should open after failures exceed threshold")
        void circuitBreakerShouldOpen() {
            // Create a specific circuit breaker for this test
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(50)
                    .slidingWindowSize(4)
                    .minimumNumberOfCalls(4)
                    .waitDurationInOpenState(Duration.ofSeconds(60))
                    .build();
            CircuitBreakerRegistry testRegistry = CircuitBreakerRegistry.of(config);
            RetryConfig noRetryConfig = RetryConfig.custom().maxAttempts(1).build();
            RetryRegistry noRetryRegistry = RetryRegistry.of(noRetryConfig);

            ResilientRestTemplate testTemplate = new ResilientRestTemplate(
                    restTemplate,
                    testRegistry,
                    noRetryRegistry
            );

            // Configure mock to fail
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            // Make enough calls to trigger circuit breaker
            for (int i = 0; i < 4; i++) {
                try {
                    testTemplate.getForObject(
                            "testCB",
                            "http://test.com/api",
                            String.class,
                            ResilienceOptions.circuitBreakerOnly()
                    );
                } catch (HttpServerErrorException ignored) {
                }
            }

            // Verify circuit breaker is open
            CircuitBreaker cb = testRegistry.circuitBreaker("testCB");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }
    }

    @Nested
    @DisplayName("getRestTemplate tests")
    class GetRestTemplateTests {

        @Test
        @DisplayName("Should return underlying RestTemplate")
        void shouldReturnUnderlyingRestTemplate() {
            RestTemplate underlying = resilientRestTemplate.getRestTemplate();

            assertThat(underlying).isSameAs(restTemplate);
        }
    }
}
