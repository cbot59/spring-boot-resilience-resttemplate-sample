# Spring Boot Resilience RestTemplate

A Spring Boot sample application demonstrating how to integrate [Resilience4j](https://resilience4j.readme.io/) with `RestTemplate` for building fault-tolerant HTTP clients. Features a `ResilientRestTemplate` wrapper that provides configurable retry and circuit breaker patterns on a per-request basis.

## Features

- **ResilientRestTemplate** - Drop-in wrapper for RestTemplate with built-in resilience
- **Per-request configuration** - Enable/disable retry and circuit breaker per request
- **Named instances** - Configure different resilience settings for different services
- **Two approaches** - Programmatic (ResilientRestTemplate) and annotation-based (@Retry, @CircuitBreaker)
- **Actuator integration** - Monitor circuit breaker states and retry metrics
- **Global exception handling** - Graceful error responses for resilience failures

## Requirements

- Java 17+
- Maven 3.6+

## Quick Start

```bash
# Clone the repository
git clone https://github.com/cbot59/spring-boot-resilience-resttemplate-sample.git
cd spring-boot-resilience-resttemplate-sample

# Build and run
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

## Usage

### Programmatic Approach (ResilientRestTemplate)

Inject `ResilientRestTemplate` and use it like a regular RestTemplate:

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final ResilientRestTemplate resilientRestTemplate;

    // Default: retry + circuit breaker enabled
    public String getData(String url) {
        return resilientRestTemplate.getForObject(url, String.class);
    }

    // Named instance with custom config
    public String getDataFromPaymentService(String url) {
        return resilientRestTemplate.getForObject("paymentService", url, String.class);
    }

    // Circuit breaker only (no retry)
    public String getDataNoRetry(String url) {
        return resilientRestTemplate.getForObject(
            "default", url, String.class,
            ResilienceOptions.circuitBreakerOnly()
        );
    }

    // Bypass resilience entirely
    public String getDataDirect(String url) {
        return resilientRestTemplate.getForObject(
            "default", url, String.class,
            ResilienceOptions.none()
        );
    }
}
```

### Annotation-Based Approach

Use Resilience4j annotations directly on service methods:

```java
@Service
public class MyService {
    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallback")
    @Retry(name = "externalApi")
    public String getData(String url) {
        return restTemplate.getForObject(url, String.class);
    }

    private String fallback(String url, Throwable t) {
        return "{\"status\": \"fallback\", \"message\": \"Service unavailable\"}";
    }
}
```

### ResilienceOptions

Control resilience behavior per request:

| Method | Retry | Circuit Breaker |
|--------|-------|-----------------|
| `ResilienceOptions.defaults()` | Yes | Yes |
| `ResilienceOptions.retryOnly()` | Yes | No |
| `ResilienceOptions.circuitBreakerOnly()` | No | Yes |
| `ResilienceOptions.none()` | No | No |

## Configuration

Configure resilience settings in `application.yml`:

```yaml
# RestTemplate timeouts
rest-template:
  connect-timeout: 5000
  read-timeout: 30000

# Resilience4j
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        recordExceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
    instances:
      externalApi:
        baseConfig: default
      paymentService:
        baseConfig: default
        waitDurationInOpenState: 30s
        failureRateThreshold: 30

  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.HttpServerErrorException
    instances:
      externalApi:
        baseConfig: default
      paymentService:
        baseConfig: default
        maxAttempts: 5
        waitDuration: 500ms
```

## Demo Endpoints

The application exposes demo endpoints to test different resilience configurations:

| Endpoint | Description |
|----------|-------------|
| `GET /api/demo/default?url=...` | Default resilience (retry + circuit breaker) |
| `GET /api/demo/circuit-breaker-only?url=...` | Circuit breaker only |
| `GET /api/demo/retry-only?url=...` | Retry only |
| `GET /api/demo/no-resilience?url=...` | No resilience |
| `GET /api/demo/annotation?url=...` | Annotation-based with fallback |
| `GET /api/demo/plain?url=...` | Plain RestTemplate |
| `POST /api/demo/post?url=...` | POST with resilience |

Example:
```bash
curl "http://localhost:8080/api/demo/default?url=https://httpbin.org/get"
```

## Monitoring

Actuator endpoints for monitoring resilience:

```bash
# Health with circuit breaker status
curl http://localhost:8080/actuator/health

# Circuit breaker states
curl http://localhost:8080/actuator/circuitbreakers

# Circuit breaker events
curl http://localhost:8080/actuator/circuitbreakerevents

# Retry metrics
curl http://localhost:8080/actuator/retries
```

## Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=SpringBootResilienceResttemplateApplicationTests

# Run specific test method
./mvnw test -Dtest=DemoControllerIntegrationTest#shouldReturnSuccessWhenServiceSucceeds
```

The project includes:
- **Unit tests** - For ResilientRestTemplate and ResilienceOptions
- **Controller tests** - WebMvcTest with mocked services
- **E2E tests** - Full integration tests with WireMock

## Project Structure

```
src/main/java/dev/rivaldi/springbootresilienceresttemplate/
├── config/
│   ├── ResilienceConfig.java      # Resilience4j bean configuration
│   └── RestTemplateConfig.java    # RestTemplate bean configuration
├── controller/
│   └── DemoController.java        # Demo REST endpoints
├── exception/
│   └── ResilienceExceptionHandler.java  # Global exception handler
├── resilience/
│   ├── ResilientRestTemplate.java # Core resilience wrapper
│   └── ResilienceOptions.java     # Per-request options
└── service/
    └── ExternalApiService.java    # Example service usage
```

## How It Works

The resilience decorator chain:

```
Request → Retry (outer) → CircuitBreaker (inner) → RestTemplate → External Service
```

1. **Retry** (outer decorator) - Retries failed requests based on configuration
2. **Circuit Breaker** (inner decorator) - Prevents calls when failure threshold is reached
3. **RestTemplate** - Makes the actual HTTP call

When both are enabled, retry will attempt the request multiple times, and each attempt goes through the circuit breaker.

## License

MIT License

## Author

[Rivaldi](https://github.com/cbot59)