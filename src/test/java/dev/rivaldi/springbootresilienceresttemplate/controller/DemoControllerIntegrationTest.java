package dev.rivaldi.springbootresilienceresttemplate.controller;

import dev.rivaldi.springbootresilienceresttemplate.service.ExternalApiService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpServerErrorException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DemoController.class)
@DisplayName("DemoController Tests")
class DemoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalApiService externalApiService;

    @BeforeEach
    void resetMocks() {
        reset(externalApiService);
    }

    @Nested
    @DisplayName("GET /api/demo/default")
    class DefaultResilienceTests {

        @Test
        @DisplayName("Should return success response when service call succeeds")
        void shouldReturnSuccessWhenServiceSucceeds() throws Exception {
            when(externalApiService.callWithDefaultResilience(anyString()))
                    .thenReturn("{\"message\": \"success\"}");

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("success")));

            verify(externalApiService).callWithDefaultResilience("http://localhost:8089/api/test");
        }

        @Test
        @DisplayName("Should propagate error when service throws exception")
        void shouldPropagateErrorOnServiceFailure() throws Exception {
            when(externalApiService.callWithDefaultResilience(anyString()))
                    .thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

            mockMvc.perform(get("/api/demo/default")
                            .param("url", "http://localhost:8089/api/test"))
                    .andExpect(status().is5xxServerError());

            verify(externalApiService).callWithDefaultResilience("http://localhost:8089/api/test");
        }
    }

    @Nested
    @DisplayName("GET /api/demo/circuit-breaker-only")
    class CircuitBreakerOnlyTests {

        @Test
        @DisplayName("Should return success when service succeeds")
        void shouldReturnSuccessWhenServiceSucceeds() throws Exception {
            when(externalApiService.callWithCircuitBreakerOnly(anyString()))
                    .thenReturn("{\"message\": \"cb only success\"}");

            mockMvc.perform(get("/api/demo/circuit-breaker-only")
                            .param("url", "http://localhost:8089/api/cb-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("cb only success")));

            verify(externalApiService).callWithCircuitBreakerOnly("http://localhost:8089/api/cb-test");
        }

        @Test
        @DisplayName("Should propagate error when service fails")
        void shouldPropagateErrorWhenServiceFails() throws Exception {
            when(externalApiService.callWithCircuitBreakerOnly(anyString()))
                    .thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

            mockMvc.perform(get("/api/demo/circuit-breaker-only")
                            .param("url", "http://localhost:8089/api/cb-test"))
                    .andExpect(status().is5xxServerError());

            verify(externalApiService).callWithCircuitBreakerOnly("http://localhost:8089/api/cb-test");
        }
    }

    @Nested
    @DisplayName("GET /api/demo/retry-only")
    class RetryOnlyTests {

        @Test
        @DisplayName("Should return success when service succeeds")
        void shouldReturnSuccessWhenServiceSucceeds() throws Exception {
            when(externalApiService.callWithRetryOnly(anyString()))
                    .thenReturn("{\"message\": \"retry success\"}");

            mockMvc.perform(get("/api/demo/retry-only")
                            .param("url", "http://localhost:8089/api/retry-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("retry success")));

            verify(externalApiService).callWithRetryOnly("http://localhost:8089/api/retry-test");
        }
    }

    @Nested
    @DisplayName("GET /api/demo/no-resilience")
    class NoResilienceTests {

        @Test
        @DisplayName("Should return success when service succeeds")
        void shouldReturnSuccessWhenServiceSucceeds() throws Exception {
            when(externalApiService.callWithoutResilience(anyString()))
                    .thenReturn("{\"message\": \"no resilience success\"}");

            mockMvc.perform(get("/api/demo/no-resilience")
                            .param("url", "http://localhost:8089/api/plain-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("no resilience success")));

            verify(externalApiService).callWithoutResilience("http://localhost:8089/api/plain-test");
        }

        @Test
        @DisplayName("Should propagate error when service fails")
        void shouldPropagateErrorWhenServiceFails() throws Exception {
            when(externalApiService.callWithoutResilience(anyString()))
                    .thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

            mockMvc.perform(get("/api/demo/no-resilience")
                            .param("url", "http://localhost:8089/api/plain-test"))
                    .andExpect(status().is5xxServerError());

            verify(externalApiService).callWithoutResilience("http://localhost:8089/api/plain-test");
        }
    }

    @Nested
    @DisplayName("GET /api/demo/annotation")
    class AnnotationBasedTests {

        @Test
        @DisplayName("Should return fallback response when service uses fallback")
        void shouldReturnFallbackOnFailure() throws Exception {
            when(externalApiService.getDataWithAnnotations(anyString()))
                    .thenReturn("{\"message\": \"fallback\"}");

            mockMvc.perform(get("/api/demo/annotation")
                            .param("url", "http://localhost:8089/api/annotation-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("fallback")));

            verify(externalApiService).getDataWithAnnotations("http://localhost:8089/api/annotation-test");
        }

        @Test
        @DisplayName("Should return actual response when service succeeds")
        void shouldReturnActualResponseOnSuccess() throws Exception {
            when(externalApiService.getDataWithAnnotations(anyString()))
                    .thenReturn("{\"message\": \"annotation success\"}");

            mockMvc.perform(get("/api/demo/annotation")
                            .param("url", "http://localhost:8089/api/annotation-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("annotation success")));

            verify(externalApiService).getDataWithAnnotations("http://localhost:8089/api/annotation-test");
        }
    }

    @Nested
    @DisplayName("POST /api/demo/post")
    class PostTests {

        @Test
        @DisplayName("Should post data successfully")
        void shouldPostSuccessfully() throws Exception {
            when(externalApiService.postWithResilience(anyString(), anyMap()))
                    .thenReturn(ResponseEntity.ok("{\"result\": \"posted\"}"));

            mockMvc.perform(post("/api/demo/post")
                            .param("url", "http://localhost:8089/api/post-test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"data\": \"test\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("posted")));

            verify(externalApiService).postWithResilience(eq("http://localhost:8089/api/post-test"), anyMap());
        }

        @Test
        @DisplayName("Should propagate error when POST fails")
        void shouldPropagateErrorOnPostFailure() throws Exception {
            when(externalApiService.postWithResilience(anyString(), anyMap()))
                    .thenThrow(new HttpServerErrorException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

            mockMvc.perform(post("/api/demo/post")
                            .param("url", "http://localhost:8089/api/post-test")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"data\": \"test\"}"))
                    .andExpect(status().is5xxServerError());

            verify(externalApiService).postWithResilience(eq("http://localhost:8089/api/post-test"), anyMap());
        }
    }

    @Nested
    @DisplayName("GET /api/demo/named")
    class NamedInstanceTests {

        @Test
        @DisplayName("Should use named instance configuration")
        void shouldUseNamedInstanceConfig() throws Exception {
            when(externalApiService.callWithNamedInstance(anyString()))
                    .thenReturn("{\"message\": \"named instance success\"}");

            mockMvc.perform(get("/api/demo/named")
                            .param("url", "http://localhost:8089/api/named-test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("named instance success")));

            verify(externalApiService).callWithNamedInstance("http://localhost:8089/api/named-test");
        }
    }

    @Nested
    @DisplayName("GET /api/demo/plain")
    class PlainRestTemplateTests {

        @Test
        @DisplayName("Should use plain RestTemplate")
        void shouldUsePlainRestTemplate() throws Exception {
            when(externalApiService.callWithPlainRestTemplate(anyString()))
                    .thenReturn("{\"message\": \"plain success\"}");

            mockMvc.perform(get("/api/demo/plain")
                            .param("url", "http://localhost:8089/api/plain"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("plain success")));

            verify(externalApiService).callWithPlainRestTemplate("http://localhost:8089/api/plain");
        }
    }
}