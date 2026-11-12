# Infrastructure Layer (`infra`)

## Overview
Root infrastructure package for the Gymnasion backend. Provides centralized RFC 7807 (`ProblemDetail`) exception handling across all REST controllers and organizes cross-cutting configuration and security subpackages.

## Interface
- `[RestExceptionHandler.java:RestExceptionHandler]` - `@RestControllerAdvice` component mapping domain, validation, security, and database exceptions to standardized RFC 7807 `ProblemDetail` responses.
- `[RestExceptionHandler.java:handleValidationException]` - Handles `MethodArgumentNotValidException` (`400 Bad Request`, type `erro-validacao`, populates `invalidFields`).
- `[RestExceptionHandler.java:handleIllegalArgumentException]` - Handles `IllegalArgumentException` (`400 Bad Request`, type `requisicao-invalida`).
- `[RestExceptionHandler.java:handleIllegalStateException]` - Handles `IllegalStateException` (`409 Conflict`, type `conflito-regra-negocio`).
- `[RestExceptionHandler.java:handleGenericException]` - Handles uncaught `Exception` (`500 Internal Server Error`, type `erro-interno`).
- `[RestExceptionHandler.java:handleAccessDeniedException]` - Handles `AccessDeniedException` (`403 Forbidden`, type `acesso-negado`).
- `[RestExceptionHandler.java:handleBadCredentialsException]` - Handles `BadCredentialsException` (`401 Unauthorized`, type `credenciais-invalidas`).
- `[RestExceptionHandler.java:handleNotFoundException]` - Handles `NotFoundException` (`404 Not Found`, type `not-found`).
- `[RestExceptionHandler.java:handleMetricasInvalidasException]` - Handles `MetricasInvalidasException` (`422 Unprocessable Entity`, type `metricas-invalidas`, populates `invalidFields`).
- `[RestExceptionHandler.java:handleContaBloqueadaException]` - Handles `ContaBloqueadaException` (`422 Unprocessable Entity`, type `conta-bloqueada`, populates `tentativas` and `minutosBloqueio`).
- `[RestExceptionHandler.java:handleRelatorioSemDadosException]` - Handles `RelatorioSemDadosException` (`422 Unprocessable Entity`, type `relatorio-sem-dados`, populates `periodoSugerido`).
- `[RestExceptionHandler.java:handleBusinessException]` - Handles `BusinessException` (`422 Unprocessable Entity`, type `regra-de-negocio`).
- `[RestExceptionHandler.java:handleDuplicateResourceException]` - Handles `DuplicateResourceException` (`409 Conflict`, type `conflito-dados`).
- `[RestExceptionHandler.java:handleDataIntegrityViolationException]` - Handles `DataIntegrityViolationException` (`409 Conflict`, type `conflito-dados`).
- Subdirectory: [config](./config/README.md) - Application configuration properties, OpenAPI/Swagger metadata, and startup administrator seeding.
- Subdirectory: [security](./security/README.md) - Stateless Spring Security filter chain, CORS configuration, JWT token service, and Bearer authentication filter.

## Invariants
- `[RestExceptionHandler.java:RestExceptionHandler]` - Every handled exception produces an RFC 7807 `ProblemDetail` response with a `type` URI under `https://api.gymnasion.com/errors/*` and a `timestamp` (`Instant.now()`) property.
- `[RestExceptionHandler.java:handleRelatorioSemDadosException]` - Populates `periodoSugerido` (`dataInicio` and `dataFim`) only when `ex.possuiPeriodoSugerido()` is `true` (`US-16 C3`), otherwise sets `periodoSugerido` to `null`.

## Side Effects
- `[RestExceptionHandler.java:RestExceptionHandler]` - Intercepts exceptions thrown across all Spring WebMVC controllers and writes structured `ProblemDetail` HTTP responses.

## Verification
- Run: `./mvnw test`
