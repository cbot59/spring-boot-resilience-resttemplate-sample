package dev.rivaldi.springbootresilienceresttemplate.service;

import dev.rivaldi.springbootresilienceresttemplate.resilience.ResilientRestTemplate;
import dev.rivaldi.springbootresilienceresttemplate.resilience.ResilienceOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalApiService Unit Tests")
class ExternalApiServiceTest {

    @Mock
    private ResilientRestTemplate resilientRestTemplate;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplate plainRestTemplate;

    private ExternalApiService externalApiService;

    @BeforeEach
    void setUp() throws Exception {
        externalApiService = new ExternalApiService(
                resilientRestTemplate,
                restTemplate,
                plainRestTemplate
        );
    }

    @Nested
    @DisplayName("Programmatic Approach - ResilientRestTemplate")
    class ProgrammaticApproachTests {

        @Test
        @DisplayName("callWithDefaultResilience should use default options")
        void shouldCallWithDefaultResilience() {
            String url = "http://test.com/api";
            when(resilientRestTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn("default-response");

            String result = externalApiService.callWithDefaultResilience(url);

            assertThat(result).isEqualTo("default-response");
            verify(resilientRestTemplate).getForObject(url, String.class);
        }

        @Test
        @DisplayName("callWithNamedInstance should use externalApi instance")
        void shouldCallWithNamedInstance() {
            String url = "http://test.com/api";
            when(resilientRestTemplate.getForObject(eq("externalApi"), anyString(), eq(String.class)))
                    .thenReturn("named-response");

            String result = externalApiService.callWithNamedInstance(url);

            assertThat(result).isEqualTo("named-response");
            verify(resilientRestTemplate).getForObject("externalApi", url, String.class);
        }

        @Test
        @DisplayName("callWithCircuitBreakerOnly should disable retry")
        void shouldCallWithCircuitBreakerOnly() {
            String url = "http://test.com/api";
            when(resilientRestTemplate.getForObject(
                    eq("externalApi"),
                    anyString(),
                    eq(String.class),
                    any(ResilienceOptions.class)
            )).thenReturn("cb-only-response");

            String result = externalApiService.callWithCircuitBreakerOnly(url);

            assertThat(result).isEqualTo("cb-only-response");

            ArgumentCaptor<ResilienceOptions> optionsCaptor = ArgumentCaptor.forClass(ResilienceOptions.class);
            verify(resilientRestTemplate).getForObject(
                    eq("externalApi"),
                    eq(url),
                    eq(String.class),
                    optionsCaptor.capture()
            );

            ResilienceOptions capturedOptions = optionsCaptor.getValue();
            assertThat(capturedOptions.isCircuitBreakerEnabled()).isTrue();
            assertThat(capturedOptions.isRetryEnabled()).isFalse();
        }

        @Test
        @DisplayName("callWithRetryOnly should disable circuit breaker")
        void shouldCallWithRetryOnly() {
            String url = "http://test.com/api";
            when(resilientRestTemplate.getForObject(
                    eq("externalApi"),
                    anyString(),
                    eq(String.class),
                    any(ResilienceOptions.class)
            )).thenReturn("retry-only-response");

            String result = externalApiService.callWithRetryOnly(url);

            assertThat(result).isEqualTo("retry-only-response");

            ArgumentCaptor<ResilienceOptions> optionsCaptor = ArgumentCaptor.forClass(ResilienceOptions.class);
            verify(resilientRestTemplate).getForObject(
                    eq("externalApi"),
                    eq(url),
                    eq(String.class),
                    optionsCaptor.capture()
            );

            ResilienceOptions capturedOptions = optionsCaptor.getValue();
            assertThat(capturedOptions.isRetryEnabled()).isTrue();
            assertThat(capturedOptions.isCircuitBreakerEnabled()).isFalse();
        }

        @Test
        @DisplayName("callWithoutResilience should disable all resilience")
        void shouldCallWithoutResilience() {
            String url = "http://test.com/api";
            when(resilientRestTemplate.getForObject(
                    eq("externalApi"),
                    anyString(),
                    eq(String.class),
                    any(ResilienceOptions.class)
            )).thenReturn("no-resilience-response");

            String result = externalApiService.callWithoutResilience(url);

            assertThat(result).isEqualTo("no-resilience-response");

            ArgumentCaptor<ResilienceOptions> optionsCaptor = ArgumentCaptor.forClass(ResilienceOptions.class);
            verify(resilientRestTemplate).getForObject(
                    eq("externalApi"),
                    eq(url),
                    eq(String.class),
                    optionsCaptor.capture()
            );

            ResilienceOptions capturedOptions = optionsCaptor.getValue();
            assertThat(capturedOptions.isRetryEnabled()).isFalse();
            assertThat(capturedOptions.isCircuitBreakerEnabled()).isFalse();
        }

        @Test
        @DisplayName("postWithResilience should use POST method with resilience")
        void shouldPostWithResilience() {
            String url = "http://test.com/api";
            Object requestBody = "test-request";
            ResponseEntity<String> expectedResponse = ResponseEntity.ok("post-response");

            when(resilientRestTemplate.exchange(
                    eq("externalApi"),
                    eq(url),
                    eq(HttpMethod.POST),
                    any(HttpEntity.class),
                    eq(String.class),
                    any(ResilienceOptions.class)
            )).thenReturn(expectedResponse);

            ResponseEntity<String> result = externalApiService.postWithResilience(url, requestBody);

            assertThat(result.getBody()).isEqualTo("post-response");
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

            ArgumentCaptor<HttpEntity<?>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(resilientRestTemplate).exchange(
                    eq("externalApi"),
                    eq(url),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(),
                    eq(String.class),
                    any(ResilienceOptions.class)
            );

            assertThat(entityCaptor.getValue().getBody()).isEqualTo(requestBody);
        }
    }

    @Nested
    @DisplayName("Annotation Approach Tests")
    class AnnotationApproachTests {

        @Test
        @DisplayName("getDataWithAnnotations should call RestTemplate directly")
        void shouldCallRestTemplateDirectly() {
            String url = "http://test.com/api";
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn("annotation-response");

            String result = externalApiService.getDataWithAnnotations(url);

            assertThat(result).isEqualTo("annotation-response");
            verify(restTemplate).getForObject(url, String.class);
            verifyNoInteractions(resilientRestTemplate);
        }
    }

    @Nested
    @DisplayName("Plain RestTemplate Tests")
    class PlainRestTemplateTests {

        @Test
        @DisplayName("callWithPlainRestTemplate should bypass resilience")
        void shouldBypassResilience() {
            String url = "http://test.com/api";
            when(plainRestTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn("plain-response");

            String result = externalApiService.callWithPlainRestTemplate(url);

            assertThat(result).isEqualTo("plain-response");
            verify(plainRestTemplate).getForObject(url, String.class);
            verifyNoInteractions(resilientRestTemplate);
        }
    }
}
