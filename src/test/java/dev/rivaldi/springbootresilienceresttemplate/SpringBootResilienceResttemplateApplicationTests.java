package dev.rivaldi.springbootresilienceresttemplate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("DemoController End-to-End Tests")
class SpringBootResilienceResttemplateApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setUpWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(CircuitBreaker::reset);
    }

    @Test
    void contextLoads() {
    }

    @Nested
    @DisplayName("GET /api/demo/default - Default Resilience (Retry + Circuit Breaker)")
    class DefaultResilienceE2ETests {

        @Test
        @DisplayName("Should return success when external API responds successfully")
        void shouldReturnSuccessWhenApiAvailable() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/test"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"success\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("success")));

            verify(1, getRequestedFor(urlEqualTo("/api/test")));
        }

        @Test
        @DisplayName("Should retry on 5xx error and succeed on second attempt")
        void shouldRetryOnServerErrorAndSucceed() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-test"))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("First Retry"));

            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-test"))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("First Retry")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"success after retry\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/retry-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("success after retry")));

            verify(2, getRequestedFor(urlEqualTo("/api/retry-test")));
        }

        @Test
        @DisplayName("Should fail after exhausting all retry attempts")
        void shouldFailAfterAllRetriesExhausted() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/fail-test"))
                    .willReturn(aResponse().withStatus(500)));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/fail-test"))
                    .andExpect(status().is5xxServerError());

            verify(moreThanOrExactly(2), getRequestedFor(urlEqualTo("/api/fail-test")));
        }
    }

    @Nested
    @DisplayName("GET /api/demo/circuit-breaker-only - Circuit Breaker Only")
    class CircuitBreakerOnlyE2ETests {

        @Test
        @DisplayName("Should return success without retry")
        void shouldReturnSuccessWithoutRetry() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/cb-test"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"cb only success\"}")));

            mockMvc.perform(get("/api/demo/circuit-breaker-only")
                            .param("url", "http://localhost:8089/api/cb-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("cb only success")));

            verify(1, getRequestedFor(urlEqualTo("/api/cb-test")));
        }

        @Test
        @DisplayName("Should NOT retry when circuit breaker only mode")
        void shouldNotRetryInCbOnlyMode() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/cb-no-retry"))
                    .willReturn(aResponse().withStatus(500)));

            mockMvc.perform(get("/api/demo/circuit-breaker-only")
                            .param("url", "http://localhost:8089/api/cb-no-retry"))
                    .andExpect(status().is5xxServerError());

            verify(1, getRequestedFor(urlEqualTo("/api/cb-no-retry")));
        }
    }

    @Nested
    @DisplayName("GET /api/demo/retry-only - Retry Only")
    class RetryOnlyE2ETests {

        @Test
        @DisplayName("Should retry on failure without circuit breaker")
        void shouldRetryWithoutCircuitBreaker() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-only-test"))
                    .inScenario("Retry Only Scenario")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("Retry"));

            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-only-test"))
                    .inScenario("Retry Only Scenario")
                    .whenScenarioStateIs("Retry")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"retry success\"}")));

            mockMvc.perform(get("/api/demo/retry-only")
                            .param("url", "http://localhost:8089/api/retry-only-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("retry success")));

            verify(2, getRequestedFor(urlEqualTo("/api/retry-only-test")));
        }
    }

    @Nested
    @DisplayName("GET /api/demo/no-resilience - No Resilience")
    class NoResilienceE2ETests {

        @Test
        @DisplayName("Should return success without any resilience")
        void shouldReturnSuccessWithoutResilience() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/plain-test"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"no resilience success\"}")));

            mockMvc.perform(get("/api/demo/no-resilience")
                            .param("url", "http://localhost:8089/api/plain-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("no resilience success")));

            verify(1, getRequestedFor(urlEqualTo("/api/plain-test")));
        }

        @Test
        @DisplayName("Should fail immediately without retry when no resilience")
        void shouldFailImmediatelyWithoutRetry() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/no-retry-test"))
                    .willReturn(aResponse().withStatus(500)));

            mockMvc.perform(get("/api/demo/no-resilience")
                            .param("url", "http://localhost:8089/api/no-retry-test"))
                    .andExpect(status().is5xxServerError());

            verify(1, getRequestedFor(urlEqualTo("/api/no-retry-test")));
        }
    }

    @Nested
    @DisplayName("GET /api/demo/annotation - Annotation-based Resilience with Fallback")
    class AnnotationBasedE2ETests {

        @Test
        @DisplayName("Should use fallback when external API fails")
        void shouldUseFallbackOnFailure() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/annotation-test"))
                    .willReturn(aResponse().withStatus(500)));

            mockMvc.perform(get("/api/demo/annotation")
                            .param("url", "http://localhost:8089/api/annotation-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("fallback")));
        }

        @Test
        @DisplayName("Should return actual response when API succeeds")
        void shouldReturnActualResponseOnSuccess() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/annotation-success"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"annotation success\"}")));

            mockMvc.perform(get("/api/demo/annotation")
                            .param("url", "http://localhost:8089/api/annotation-success"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("annotation success")));
        }
    }

    @Nested
    @DisplayName("POST /api/demo/post - POST with Resilience")
    class PostE2ETests {

        @Test
        @DisplayName("Should POST data with resilience successfully")
        void shouldPostWithResilience() throws Exception {
            wireMockServer.stubFor(WireMock.post(urlEqualTo("/api/post-test"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"result\": \"posted\"}")));

            mockMvc.perform(post("/api/demo/post")
                            .param("url", "http://localhost:8089/api/post-test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"data\": \"test\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("posted")));

            verify(1, postRequestedFor(urlEqualTo("/api/post-test")));
        }

        @Test
        @DisplayName("Should retry POST on failure and succeed")
        void shouldRetryPostOnFailure() throws Exception {
            wireMockServer.stubFor(WireMock.post(urlEqualTo("/api/post-retry"))
                    .inScenario("POST Retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("Retry"));

            wireMockServer.stubFor(WireMock.post(urlEqualTo("/api/post-retry"))
                    .inScenario("POST Retry")
                    .whenScenarioStateIs("Retry")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"result\": \"posted after retry\"}")));

            mockMvc.perform(post("/api/demo/post")
                            .param("url", "http://localhost:8089/api/post-retry")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"data\": \"test\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("posted after retry")));

            verify(2, postRequestedFor(urlEqualTo("/api/post-retry")));
        }
    }

    @Nested
    @DisplayName("GET /api/demo/named - Named Instance Configuration")
    class NamedInstanceE2ETests {

        @Test
        @DisplayName("Should use named instance configuration")
        void shouldUseNamedInstanceConfig() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/named-test"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"named instance success\"}")));

            mockMvc.perform(get("/api/demo/named")
                            .param("url", "http://localhost:8089/api/named-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("named instance success")));

            verify(1, getRequestedFor(urlEqualTo("/api/named-test")));
        }
    }

    @Nested
    @DisplayName("GET /api/demo/plain - Plain RestTemplate (bypass resilience)")
    class PlainRestTemplateE2ETests {

        @Test
        @DisplayName("Should use plain RestTemplate bypassing resilience")
        void shouldBypassResilience() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/plain"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"plain success\"}")));

            mockMvc.perform(get("/api/demo/plain")
                            .param("url", "http://localhost:8089/api/plain"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("plain success")));

            verify(1, getRequestedFor(urlEqualTo("/api/plain")));
        }

        @Test
        @DisplayName("Should fail immediately without retry for plain RestTemplate")
        void shouldFailWithoutRetryForPlainRestTemplate() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/plain-fail"))
                    .willReturn(aResponse().withStatus(500)));

            mockMvc.perform(get("/api/demo/plain")
                            .param("url", "http://localhost:8089/api/plain-fail"))
                    .andExpect(status().is5xxServerError());

            verify(1, getRequestedFor(urlEqualTo("/api/plain-fail")));
        }
    }

    @Nested
    @DisplayName("Timeout Handling Tests")
    class TimeoutE2ETests {

        @Test
        @DisplayName("Should timeout on slow response and return error")
        void shouldTimeoutOnSlowResponse() throws Exception {
            // With read-timeout of 3s (from application-test.yml), a 5s delay will cause timeout
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/slow-endpoint"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withFixedDelay(5000)
                            .withBody("{\"message\": \"slow\"}")));

            // Expect 503 Service Unavailable after timeout
            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/slow-endpoint"))
                    .andExpect(status().isServiceUnavailable());

            // Verify at least one request was made (retries may or may not happen depending on config)
            verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/api/slow-endpoint")));
        }

        @Test
        @DisplayName("Should succeed when response is within timeout")
        void shouldSucceedWhenWithinTimeout() throws Exception {
            // Delay of 1s is within the 3s timeout
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/acceptable-delay"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withFixedDelay(1000)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"delayed but ok\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/acceptable-delay"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("delayed but ok")));

            verify(1, getRequestedFor(urlEqualTo("/api/acceptable-delay")));
        }
    }

    @Nested
    @DisplayName("Retry Exception Configuration Tests")
    class RetryExceptionConfigTests {

        @Test
        @DisplayName("Should retry on HttpServerErrorException (500) - configured in retryExceptions")
        void shouldRetryOnHttpServerErrorException() throws Exception {
            // 500 Internal Server Error triggers HttpServerErrorException which is in retryExceptions
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-500"))
                    .inScenario("500 Retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("Retried"));

            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-500"))
                    .inScenario("500 Retry")
                    .whenScenarioStateIs("Retried")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"recovered from 500\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/retry-500"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("recovered from 500")));

            // Should have retried - 2 requests total
            verify(2, getRequestedFor(urlEqualTo("/api/retry-500")));
        }

        @Test
        @DisplayName("Should retry on 502 Bad Gateway - HttpServerErrorException in retryExceptions")
        void shouldRetryOn502BadGateway() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-502"))
                    .inScenario("502 Retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(502))
                    .willSetStateTo("Retried"));

            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-502"))
                    .inScenario("502 Retry")
                    .whenScenarioStateIs("Retried")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"recovered from 502\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/retry-502"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("recovered from 502")));

            verify(2, getRequestedFor(urlEqualTo("/api/retry-502")));
        }

        @Test
        @DisplayName("Should retry on 503 Service Unavailable - HttpServerErrorException in retryExceptions")
        void shouldRetryOn503ServiceUnavailable() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-503"))
                    .inScenario("503 Retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(503))
                    .willSetStateTo("Retried"));

            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-503"))
                    .inScenario("503 Retry")
                    .whenScenarioStateIs("Retried")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"recovered from 503\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/retry-503"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("recovered from 503")));

            verify(2, getRequestedFor(urlEqualTo("/api/retry-503")));
        }

        @Test
        @DisplayName("Should retry on 504 Gateway Timeout - HttpServerErrorException in retryExceptions")
        void shouldRetryOn504GatewayTimeout() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-504"))
                    .inScenario("504 Retry")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(504))
                    .willSetStateTo("Retried"));

            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/retry-504"))
                    .inScenario("504 Retry")
                    .whenScenarioStateIs("Retried")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"message\": \"recovered from 504\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/retry-504"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("recovered from 504")));

            verify(2, getRequestedFor(urlEqualTo("/api/retry-504")));
        }
    }

    @Nested
    @DisplayName("Ignore Exception Configuration Tests")
    class IgnoreExceptionConfigTests {

        @Test
        @DisplayName("Should NOT retry on 400 Bad Request - configured in ignoreExceptions")
        void shouldNotRetryOn400BadRequest() throws Exception {
            // 400 Bad Request triggers HttpClientErrorException.BadRequest which is in ignoreExceptions
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/no-retry-400"))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Bad Request\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/no-retry-400"))
                    .andExpect(status().isBadRequest());

            // Should NOT retry - only 1 request
            verify(1, getRequestedFor(urlEqualTo("/api/no-retry-400")));
        }

        @Test
        @DisplayName("Should NOT retry on 404 Not Found - configured in ignoreExceptions")
        void shouldNotRetryOn404NotFound() throws Exception {
            // 404 Not Found triggers HttpClientErrorException.NotFound which is in ignoreExceptions
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/no-retry-404"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Not Found\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/no-retry-404"))
                    .andExpect(status().isNotFound());

            // Should NOT retry - only 1 request
            verify(1, getRequestedFor(urlEqualTo("/api/no-retry-404")));
        }

        @Test
        @DisplayName("Should NOT retry on 401 Unauthorized - not in retryExceptions")
        void shouldNotRetryOn401Unauthorized() throws Exception {
            // 401 is not in retryExceptions, so it should not be retried
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/no-retry-401"))
                    .willReturn(aResponse()
                            .withStatus(401)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Unauthorized\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/no-retry-401"))
                    .andExpect(status().isUnauthorized());

            // Should NOT retry - only 1 request
            verify(1, getRequestedFor(urlEqualTo("/api/no-retry-401")));
        }

        @Test
        @DisplayName("Should NOT retry on 403 Forbidden - not in retryExceptions")
        void shouldNotRetryOn403Forbidden() throws Exception {
            // 403 is not in retryExceptions, so it should not be retried
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/no-retry-403"))
                    .willReturn(aResponse()
                            .withStatus(403)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Forbidden\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/no-retry-403"))
                    .andExpect(status().isForbidden());

            // Should NOT retry - only 1 request
            verify(1, getRequestedFor(urlEqualTo("/api/no-retry-403")));
        }

        @Test
        @DisplayName("Should NOT retry on 422 Unprocessable Entity - not in retryExceptions")
        void shouldNotRetryOn422UnprocessableEntity() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/no-retry-422"))
                    .willReturn(aResponse()
                            .withStatus(422)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Unprocessable Entity\"}")));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/no-retry-422"))
                    .andExpect(status().isUnprocessableEntity());

            // Should NOT retry - only 1 request
            verify(1, getRequestedFor(urlEqualTo("/api/no-retry-422")));
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Exception Recording Tests")
    class CircuitBreakerExceptionRecordingTests {

        @Test
        @DisplayName("Should record 5xx errors in circuit breaker - configured in recordExceptions")
        void shouldRecordServerErrorsInCircuitBreaker() throws Exception {
            // Make multiple failed requests to trigger circuit breaker
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/cb-record-500"))
                    .willReturn(aResponse().withStatus(500)));

            // The circuit-breaker-only endpoint uses "externalApi" instance
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("externalApi");
            circuitBreaker.reset();

            // Make enough failed calls to record failures (minimumNumberOfCalls = 3 in test config)
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/demo/circuit-breaker-only")
                        .param("url", "http://localhost:8089/api/cb-record-500"));
            }

            // Circuit breaker should have recorded failures
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            Assertions.assertTrue(metrics.getNumberOfFailedCalls() > 0,
                    "Circuit breaker should record 5xx errors as failures");
        }

        @Test
        @DisplayName("Should NOT record 400 Bad Request in circuit breaker - configured in ignoreExceptions")
        void shouldNotRecord400InCircuitBreaker() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/cb-ignore-400"))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withBody("{\"error\": \"Bad Request\"}")));

            // The circuit-breaker-only endpoint uses "externalApi" instance
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("externalApi");
            circuitBreaker.reset();

            // Make multiple requests with 400 errors
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/demo/circuit-breaker-only")
                        .param("url", "http://localhost:8089/api/cb-ignore-400"));
            }

            // Circuit breaker should NOT record 400 as failures (it's in ignoreExceptions)
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            Assertions.assertEquals(0, metrics.getNumberOfFailedCalls(),
                    "Circuit breaker should NOT record 400 errors as failures (ignoreExceptions)");
        }

        @Test
        @DisplayName("Circuit breaker should stay CLOSED after ignored exceptions")
        void circuitBreakerShouldStayClosedAfterIgnoredExceptions() throws Exception {
            wireMockServer.stubFor(WireMock.get(urlEqualTo("/api/cb-stay-closed"))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withBody("{\"error\": \"Bad Request\"}")));

            // The circuit-breaker-only endpoint uses "externalApi" instance
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("externalApi");
            circuitBreaker.reset();

            // Make many requests with 400 errors
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(get("/api/demo/circuit-breaker-only")
                        .param("url", "http://localhost:8089/api/cb-stay-closed"));
            }

            // Circuit breaker should remain CLOSED because 400 is ignored
            Assertions.assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState(),
                    "Circuit breaker should remain CLOSED after ignored exceptions (400)");
        }
    }
}