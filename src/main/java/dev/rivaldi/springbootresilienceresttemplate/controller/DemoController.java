package dev.rivaldi.springbootresilienceresttemplate.controller;

import dev.rivaldi.springbootresilienceresttemplate.service.ExternalApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo controller to test resilience patterns.
 * Provides endpoints to demonstrate different resilience configurations.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoController {

    private final ExternalApiService externalApiService;

    // Default test URL (httpbin is a free HTTP testing service)
    private static final String DEFAULT_URL = "https://httpbin.org/get";
    private static final String DELAY_URL = "https://httpbin.org/delay/2";
    private static final String ERROR_URL = "https://httpbin.org/status/500";

    /**
     * Test with default resilience (retry + circuit breaker)
     * GET /api/demo/default?url=https://example.com/api
     */
    @GetMapping("/default")
    public ResponseEntity<String> testDefaultResilience(
            @RequestParam(defaultValue = DEFAULT_URL) String url) {
        String result = externalApiService.callWithDefaultResilience(url);
        return ResponseEntity.ok(result);
    }

    /**
     * Test with named instance ('externalApi' configuration)
     * GET /api/demo/named?url=https://example.com/api
     */
    @GetMapping("/named")
    public ResponseEntity<String> testNamedInstance(
            @RequestParam(defaultValue = DEFAULT_URL) String url) {
        String result = externalApiService.callWithNamedInstance(url);
        return ResponseEntity.ok(result);
    }

    /**
     * Test with circuit breaker only (no retry)
     * GET /api/demo/circuit-breaker-only?url=https://example.com/api
     */
    @GetMapping("/circuit-breaker-only")
    public ResponseEntity<String> testCircuitBreakerOnly(
            @RequestParam(defaultValue = DEFAULT_URL) String url) {
        String result = externalApiService.callWithCircuitBreakerOnly(url);
        return ResponseEntity.ok(result);
    }

    /**
     * Test with retry only (no circuit breaker)
     * GET /api/demo/retry-only?url=https://example.com/api
     */
    @GetMapping("/retry-only")
    public ResponseEntity<String> testRetryOnly(
            @RequestParam(defaultValue = DEFAULT_URL) String url) {
        String result = externalApiService.callWithRetryOnly(url);
        return ResponseEntity.ok(result);
    }

    /**
     * Test without any resilience (bypass entirely)
     * GET /api/demo/no-resilience?url=https://example.com/api
     */
    @GetMapping("/no-resilience")
    public ResponseEntity<String> testNoResilience(
            @RequestParam(defaultValue = DEFAULT_URL) String url) {
        String result = externalApiService.callWithoutResilience(url);
        return ResponseEntity.ok(result);
    }

    /**
     * Test annotation-based approach with fallback
     * GET /api/demo/annotation?url=https://example.com/api
     */
    @GetMapping("/annotation")
    public ResponseEntity<String> testAnnotationBased(
            @RequestParam(defaultValue = DEFAULT_URL) String url) {
        String result = externalApiService.getDataWithAnnotations(url);
        return ResponseEntity.ok(result);
    }

    /**
     * Test with plain RestTemplate (completely bypasses resilience)
     * GET /api/demo/plain?url=https://example.com/api
     */
    @GetMapping("/plain")
    public ResponseEntity<String> testPlainRestTemplate(
            @RequestParam(defaultValue = DEFAULT_URL) String url) {
        String result = externalApiService.callWithPlainRestTemplate(url);
        return ResponseEntity.ok(result);
    }

    /**
     * POST test with resilience
     * POST /api/demo/post?url=https://httpbin.org/post
     */
    @PostMapping("/post")
    public ResponseEntity<String> testPost(
            @RequestParam(defaultValue = "https://httpbin.org/post") String url,
            @RequestBody(required = false) Map<String, Object> body) {
        ResponseEntity<String> result = externalApiService.postWithResilience(
                url,
                body != null ? body : Map.of("test", "data")
        );
        return result;
    }

    /**
     * Test endpoint that calls a slow service (to trigger timeouts/retries)
     * GET /api/demo/slow
     */
    @GetMapping("/slow")
    public ResponseEntity<String> testSlowService() {
        String result = externalApiService.callWithDefaultResilience(DELAY_URL);
        return ResponseEntity.ok(result);
    }

    /**
     * Test endpoint that calls a failing service (to trigger circuit breaker)
     * Call this multiple times to see the circuit breaker open.
     * GET /api/demo/fail
     */
    @GetMapping("/fail")
    public ResponseEntity<String> testFailingService() {
        String result = externalApiService.callWithDefaultResilience(ERROR_URL);
        return ResponseEntity.ok(result);
    }

    /**
     * Test endpoint that calls a failing service with annotation-based fallback
     * GET /api/demo/fail-with-fallback
     */
    @GetMapping("/fail-with-fallback")
    public ResponseEntity<String> testFailingServiceWithFallback() {
        String result = externalApiService.getDataWithAnnotations(ERROR_URL);
        return ResponseEntity.ok(result);
    }
}
