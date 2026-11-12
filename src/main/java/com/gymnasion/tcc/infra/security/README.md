# Infra Security (`infra/security`)

## Overview
Implements stateless JWT authentication and role-based access control for the Gymnasion API using Spring Security and Auth0 `java-jwt`. Configures CORS policies, password hashing, JWT issuance/verification, and per-request Bearer token filtering with student active-status enforcement (`RN-014`).

## Interface
- `[SecurityConfig.java:SecurityConfig]` - `@Configuration` and `@EnableMethodSecurity` class defining the HTTP security filter chain, CORS rules, authentication manager, and password encoder beans.
- `[SecurityConfig.java:corsConfigurationSource]` - `@Bean` configuring `CorsConfigurationSource` for `${app.frontend.url:http://localhost:5173}`, `http://localhost:[*]`, and `http://127.0.0.1:[*]` with credentials and `Authorization`/`Content-Disposition` exposed headers.
- `[SecurityConfig.java:securityFilterChain]` - `@Bean` configuring `SecurityFilterChain` with stateless sessions, route authorization rules, and `SecurityFilter` registration before `UsernamePasswordAuthenticationFilter`.
- `[SecurityConfig.java:authenticationManager]` - `@Bean` exposing Spring Security's `AuthenticationManager`.
- `[SecurityConfig.java:passwordEncoder]` - `@Bean` providing `BCryptPasswordEncoder`.
- `[SecurityFilter.java:SecurityFilter]` - `@Component` extending `OncePerRequestFilter` to authenticate requests carrying a `Bearer` JWT header.
- `[SecurityFilter.java:doFilterInternal]` - Validates the JWT token, verifies user account state and `RN-014` student status, and populates `SecurityContextHolder`.
- `[TokenService.java:TokenService]` - `@Service` handling HMAC-SHA256 JWT generation, validation, and claim extraction.
- `[TokenService.java:generateToken]` - Issues a signed JWT for a `Usuario` with issuer `"auth-api"`, subject `usuario.getEmail()`, and claims `"UsuarioId"` and `"role"`.
- `[TokenService.java:validateToken]` - Verifies a JWT token and returns the subject email, or `""` if verification fails.
- `[TokenService.java:extractRole]` - Extracts the `"role"` claim from a valid JWT.
- `[TokenService.java:extractUsuarioId]` - Extracts and parses the `"UsuarioId"` claim as a `UUID` from a valid JWT.

## Invariants
- `[SecurityConfig.java:securityFilterChain]` - Enforces `SessionCreationPolicy.STATELESS` with CSRF protection disabled.
- `[SecurityConfig.java:securityFilterChain]` - Permits unauthenticated access to `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`, `POST /auth/**`, and `GET /modalidades/**`; restricts `/personal-trainers/**` to `ROLE_PERSONAL_TRAINER`, `/alunos/**` to `ROLE_ALUNO`, non-GET `/modalidades/**` to `ROLE_ADMIN`, and requires authentication on all other endpoints.
- `[SecurityFilter.java:doFilterInternal]` - Enforces `RN-014`: authenticated users with `Role.ALUNO` must resolve to an `Aluno` entity with `StatusConvite.ATIVO`; inactive (`StatusConvite.INATIVO`) or non-active students are rejected immediately with HTTP `401 Unauthorized`.
- `[TokenService.java:generateToken]` / `[TokenService.java:decodeToken]` - All tokens are signed and verified using `Algorithm.HMAC256(${security.jwt.secret})` with issuer `"auth-api"` and expiration offset `${security.jwt.expiration-ms}`.

## Side Effects
- `[SecurityFilter.java:doFilterInternal]` - Executes database lookups via `UsuarioRepository.findByEmail` and (for `Role.ALUNO`) `AlunoRepository.findByUsuario` on every request with a `Bearer` token.
- `[SecurityFilter.java:doFilterInternal]` - Mutates thread-local `SecurityContextHolder.getContext().setAuthentication(...)` with an authenticated `UsernamePasswordAuthenticationToken`.
- `[SecurityFilter.java:handleUnauthorizedResponse]` - Serializes an RFC 7807 `ProblemDetail` (`application/problem+json`, status `401`) directly to the `HttpServletResponse` writer when token or account validation fails.

## Verification
- Run: `./mvnw test`
