# Infra Config (`infra/config`)

## Overview
Provides Spring Boot configuration properties bindings (`@ConfigurationProperties`), OpenAPI/Swagger documentation metadata with Bearer JWT security scheme definitions, and startup initialization for seeding the default system administrator account.

## Interface
- `[AlunosPendentesProperties.java:AlunosPendentesProperties]` - `@ConfigurationProperties(prefix = "app.notificacoes.alunos-pendentes")` record configuring reminder scheduling (`enabled`, `tempoLimite`, `intervaloLembrete`, `cron`).
- `[DataInitializer.java:DataInitializer]` - `@Component` implementing `CommandLineRunner` to initialize the default `Role.ADMIN` account on startup.
- `[DataInitializer.java:run]` - Executes startup check and persists the default administrator `Usuario` if absent.
- `[OpenApiConfig.java:OpenApiConfig]` - `@Configuration` class declaring `@OpenAPIDefinition` (`title = "Gymnasion API"`, `version = "1.0.0"`) and `@SecurityScheme` (`name = "bearerAuth"`, `type = SecuritySchemeType.HTTP`, `scheme = "bearer"`, `bearerFormat = "JWT"`).
- `[StravaProperties.java:StravaProperties]` - `@ConfigurationProperties(prefix = "strava")` component binding `clientId`, `clientSecret`, `redirectUri`, and `mockEnabled`.
- `[StravaProperties.java:isConfigured]` - Returns `true` when both `clientId` and `clientSecret` are non-null and non-blank.

## Invariants
- `[DataInitializer.java:run]` - Creates the default `Role.ADMIN` user only when `!usuarioRepository.existsByEmail(adminEmail)`.
- `[DataInitializer.java:adminEmail]` / `[DataInitializer.java:adminPassword]` - Requires `${admin.email}` and `${admin.password}` configuration properties to be resolvable at application startup.
- `[StravaProperties.java:isConfigured]` - Strava credentials are treated as configured only if both `clientId` and `clientSecret` are non-null and non-blank.

## Side Effects
- `[DataInitializer.java:run]` - Queries `UsuarioRepository.existsByEmail`, encodes `adminPassword` via `PasswordEncoder`, saves a new `Usuario` with `Role.ADMIN` (`cpf = "00000000000"`, `celular = "00000-0000"`) via `usuarioRepository.save(admin)`, and emits an SLF4J `INFO` log entry on startup.

## Verification
- Run: `./mvnw test`
