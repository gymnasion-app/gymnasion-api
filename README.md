# Gymnasion Backend (`tcc`) Blueprint

## System Overview
Gymnasion is a multi-tenant SaaS backend platform built with Java 21, Spring Boot, Spring Security (stateless HMAC-SHA256 JWT), Spring Data JPA, Flyway, PostgreSQL, and OpenPDF. It enables Personal Trainers to manage student invitations and approvals, sports modalities, custom performance metrics, classes (`Turma`), individualized or group workout routines (`RotinaTreino`), planned and executed sessions (`SessaoTreino`), Borg CR-10 RPE and metric performance tracking (`Desempenho`), audited performance corrections (`AuditoriaDesempenho`), evolution dashboards, consolidated PDF reports, notifications, immutable profile observations/feedbacks, and Strava API v3 OAuth 2.0 activity synchronization.

## Subdirectories & System Boundaries
- `src`: [./src/README.md](./src/README.md) - Maven standard source layout root containing main Java production packages and application resources.
  - `src/main`: [./src/main/README.md](./src/main/README.md) - Main production Java sources and classpath resources.
  - `src/main/java/com/gymnasion/tcc`: [./src/main/java/com/gymnasion/tcc/README.md](./src/main/java/com/gymnasion/tcc/README.md) - Root Spring Boot application package (`TccApplication`) and subsystem modules:
    - `controller`: [./src/main/java/com/gymnasion/tcc/controller/README.md](./src/main/java/com/gymnasion/tcc/controller/README.md) - REST API controllers under context path `/api` for Personal Trainers, Alunos, and Admins.
    - `service`: [./src/main/java/com/gymnasion/tcc/service/README.md](./src/main/java/com/gymnasion/tcc/service/README.md) - Domain business logic, metric validation, OpenPDF report generation, in-memory login rate limiting, SMTP email dispatch, and Strava API v3 client.
    - `repository`: [./src/main/java/com/gymnasion/tcc/repository/README.md](./src/main/java/com/gymnasion/tcc/repository/README.md) - Spring Data JPA repositories enforcing multi-tenant data isolation and entity graph queries.
    - `domain`: [./src/main/java/com/gymnasion/tcc/domain/README.md](./src/main/java/com/gymnasion/tcc/domain/README.md) & [./src/main/java/com/gymnasion/tcc/domain/enums/README.md](./src/main/java/com/gymnasion/tcc/domain/enums/README.md) - JPA entities and domain enumerations.
    - `dto`: [./src/main/java/com/gymnasion/tcc/dto/README.md](./src/main/java/com/gymnasion/tcc/dto/README.md) - Immutable Java `record` request/response DTOs and Jakarta Bean Validation constraints.
    - `infra`: [./src/main/java/com/gymnasion/tcc/infra/README.md](./src/main/java/com/gymnasion/tcc/infra/README.md), [./src/main/java/com/gymnasion/tcc/infra/config/README.md](./src/main/java/com/gymnasion/tcc/infra/config/README.md), & [./src/main/java/com/gymnasion/tcc/infra/security/README.md](./src/main/java/com/gymnasion/tcc/infra/security/README.md) - RFC 7807 `ProblemDetail` exception handling, OpenAPI/admin configuration, and stateless JWT security filter chain.
    - `event`: [./src/main/java/com/gymnasion/tcc/event/README.md](./src/main/java/com/gymnasion/tcc/event/README.md) - Post-commit `@Async` transactional event listeners for invitation, approval, and reminder emails.
    - `exceptions`: [./src/main/java/com/gymnasion/tcc/exceptions/README.md](./src/main/java/com/gymnasion/tcc/exceptions/README.md) - Domain and application exception classes.
    - `scheduler`: [./src/main/java/com/gymnasion/tcc/scheduler/README.md](./src/main/java/com/gymnasion/tcc/scheduler/README.md) - `@Scheduled` cron jobs for pending student reminders.
  - `src/main/resources/db/migration`: [./src/main/resources/db/migration/README.md](./src/main/resources/db/migration/README.md) - Versioned Flyway PostgreSQL schema migrations (`V1` through `V20`).

## Global Invariants
- **Multi-Tenant Isolation**: Personal Trainers can only read or mutate students (`Aluno`), classes (`Turma`), custom metrics (`Metrica`), training routines (`RotinaTreino`), sessions (`SessaoTreino`), performance records (`Desempenho`), and reports belonging to their own `personalTrainerId`; Students (`Role.ALUNO`) can only access their own profile, routines, sessions, metrics, notifications, and Strava integration.
- **Stateless JWT & Student Status Enforcement (`RN-014`)**: Every authenticated request is verified via HMAC-SHA256 JWT (`TokenService`, issuer `"auth-api"`); `SecurityFilter` and `AuthService` reject students whose `StatusConvite` is not `ATIVO` (`INATIVO`, `PENDENTE`, `RECUSADO`).
- **Authentication Rate Limiting & Password Policy (`RN-002`, `RN-010`)**: Passwords require 8+ characters with uppercase, lowercase, digit, and special character (`UsuarioService.validarPoliticaSenhaForte`); 5 consecutive failed login attempts lock authentication for 5 minutes (`LoginAttemptService`).
- **Routine & Metric Integrity (`RN-012`, `RN-015`, `RN-017`)**: A student may hold at most 1 active routine per sport modality; active routines require at least 1 linked student; metrics with historical data cannot change `TipoMetrica` and are soft-deleted (`ativo = false`) on deletion; metric values strictly follow `ValidadorValorMetrica` (`NUMERICO`, `TEMPO`, `FREQUENCIA_CARDIACA` 30..250 bpm, `TEXTO`).
- **Auditability & Immutability (`US-23`, `US-26`)**: Performance corrections by a Personal Trainer record `INCLUSAO` or `CORRECAO` entries in `auditoria_desempenho`; profile observations and feedbacks (`observacao_feedback`) are append-only (`updatable = false`).
- **RFC 7807 Error Contract**: All REST exceptions return `application/problem+json` (`ProblemDetail`) with type URIs under `https://api.gymnasion.com/errors/*`.

## Global Side Effects
- **PostgreSQL Database (`spring.datasource.*`)**: Flyway applies `V1..V20` DDL/DML migrations on startup (`spring.flyway.enabled=true`), `DataInitializer` seeds the default `ADMIN` account if absent, and JPA repositories persist domain state across 23 tables.
- **SMTP Email Server (`spring.mail.*`)**: `EmailService` and post-commit `@Async` listeners in `com.gymnasion.tcc.event` send transactional invitation, approval/rejection, and pending-student reminder emails via `JavaMailSender`.
- **Strava API v3 (`https://www.strava.com`)**: `StravaService` exchanges/refreshes OAuth 2.0 tokens, deauthorizes connections, and fetches athlete activities over HTTPS.
- **In-Memory PDF Rendering**: `RelatorioPdfGenerator` loads embedded Plus Jakarta Sans fonts and generates consolidated binary PDF reports (`Cache-Control: no-store`).

## System Verification
- Run: `./mvnw test`
