# Gymnasion Backend Application (`com.gymnasion.tcc`)

## Overview
Root Java package of the Gymnasion SaaS backend (`com.gymnasion.tcc`). Contains the Spring Boot application entry point (`TccApplication`) and groups the REST controllers, domain entities, DTO records, transactional events, exceptions, infrastructure/security layer, JPA repositories, scheduled jobs, and business services.

## Interface
- `[TccApplication.java:TccApplication]` - `@SpringBootApplication`, `@EnableAsync`, `@EnableScheduling`, and `@EnableConfigurationProperties(AlunosPendentesProperties.class)` entry point class.
- `[TccApplication.java:main]` - Boots the Spring application context via `SpringApplication.run(TccApplication.class, args)`.
- Subdirectory: [controller](./controller/README.md) - REST API controllers for Personal Trainers, Alunos, and Admins.
- Subdirectory: [domain](./domain/README.md) - JPA domain entities and enumerations (`./domain/enums/README.md`).
- Subdirectory: [dto](./dto/README.md) - Immutable Java `record` request/response Data Transfer Objects and Jakarta Validation contracts.
- Subdirectory: [event](./event/README.md) - Domain events and `@Async` post-commit email listeners.
- Subdirectory: [exceptions](./exceptions/README.md) - Business, validation, and authentication runtime exceptions.
- Subdirectory: [infra](./infra/README.md) - RFC 7807 `ProblemDetail` exception handler (`RestExceptionHandler`), configuration (`./infra/config/README.md`), and JWT security (`./infra/security/README.md`).
- Subdirectory: [repository](./repository/README.md) - Spring Data JPA repositories with multi-tenant query isolation.
- Subdirectory: [scheduler](./scheduler/README.md) - `@Scheduled` background jobs for pending student reminders.
- Subdirectory: [service](./service/README.md) - Core business services, metric validation, OpenPDF report generation, SMTP email dispatch, and Strava API v3 integration.

## Invariants
- `[TccApplication.java:TccApplication]` Enables asynchronous event execution (`@EnableAsync`), cron scheduling (`@EnableScheduling`), and strongly-typed binding of `AlunosPendentesProperties` across the application context.

## Side Effects
- `[TccApplication.java:main]` Initializes the Spring Boot WebMVC container, Flyway database migrations, JPA entity manager, and background task schedulers.

## Verification
- Run: `./mvnw test`
