# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot 2.7 sample application demonstrating Resilience4j integration with RestTemplate. It provides a `ResilientRestTemplate` wrapper that adds configurable retry and circuit breaker patterns to HTTP calls. The `ResilientRestTemplateFactory` allows wrapping any RestTemplate instance with resilience patterns.

## Build & Test Commands

```bash
# Build the project
./mvnw clean compile

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=DemoControllerIntegrationTest

# Run a specific test method
./mvnw test -Dtest=DemoControllerIntegrationTest#shouldReturnSuccessWhenServiceSucceeds

# Run the application
./mvnw spring-boot:run

# Package the application
./mvnw package
```

**Note:** Requires Java 17. Use `sdk use java 17.0.17-librca` if using SDKMAN.

## Architecture

### Core Resilience Pattern

The key abstraction is `ResilientRestTemplate` which wraps Spring's `RestTemplate` with Resilience4j decorators:

```
Request → Retry (outer) → CircuitBreaker (inner) → RestTemplate → External Service
```

**`ResilienceOptions`** controls per-request behavior:
- `ResilienceOptions.defaults()` - Both retry and circuit breaker enabled
- `ResilienceOptions.circuitBreakerOnly()` - No retry
- `ResilienceOptions.retryOnly()` - No circuit breaker
- `ResilienceOptions.none()` - Bypass resilience entirely

### Key Components

| Component | Purpose |
|-----------|---------|
| `ResilientRestTemplate` | Programmatic resilience wrapper for RestTemplate |
| `ResilientRestTemplateFactory` | Factory to wrap any RestTemplate instance with resilience |
| `ResilienceOptions` | Per-request configuration (enable/disable retry/CB) |
| `ExternalApiService` | Demo service showing programmatic, annotation, and factory approaches |
| `ResilienceExceptionHandler` | Global exception handler for resilience failures |

### Three Approaches to Resilience

1. **Programmatic** (via `ResilientRestTemplate`):
   ```java
   resilientRestTemplate.getForObject("externalApi", url, String.class, ResilienceOptions.retryOnly());
   ```

2. **Factory** (wrap any RestTemplate with `ResilientRestTemplateFactory`):
   ```java
   RestTemplate customRestTemplate = builder.setReadTimeout(Duration.ofSeconds(60)).build();
   ResilientRestTemplate resilient = factory.wrap(customRestTemplate);
   resilient.getForObject("externalApi", url, String.class);
   ```

3. **Annotation-based** (on service methods):
   ```java
   @CircuitBreaker(name = "externalApi", fallbackMethod = "fallback")
   @Retry(name = "externalApi")
   public String getData(String url) { ... }
   ```

### Configuration

Resilience4j is configured in `application.yml` with named instances:
- `default` - Base configuration
- `externalApi` - For external API calls
- `paymentService` - Custom config with 5 retries, 30s CB wait

## Test Structure

- **`DemoControllerIntegrationTest`** - `@WebMvcTest` with mocked service (fast, isolated)
- **`SpringBootResilienceResttemplateApplicationTests`** - Full E2E tests with WireMock and `@ActiveProfiles("test")`
- **`ResilientRestTemplateTest`** - Unit tests for the resilience wrapper
- **`ExternalApiServiceTest`** - Service layer unit tests (includes factory usage tests)

Test profile (`application-test.yml`) uses shorter timeouts (3s read timeout) for faster test execution.

## Actuator Endpoints

Resilience metrics exposed at:
- `/actuator/health` - Health with circuit breaker status
- `/actuator/circuitbreakers` - Circuit breaker states
- `/actuator/retries` - Retry metrics
