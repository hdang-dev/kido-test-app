# Backend conventions

Spring Boot 3.5 · Java 17 · base package `com.kido.testapp`

- Organize packages by feature (`auth/`, `user/`, `admin/`). Shared code goes in `common/`, wiring goes in `config/`.
- The API is JSON only, under `/api/**`. There are no server-rendered views.
- Controllers stay thin: `@Valid` request DTO → service → response DTO. Controllers never touch repositories directly.
- DTOs are Java `record`s. Never accept or return an entity, so there is no mass assignment of `role` or `enabled` and no hash leaks.
- Business rules live in `*Service`. Writes are `@Transactional`. Throw specific exceptions that `common/GlobalExceptionHandler` maps to `ErrorResponse`.
- Use constructor injection (no Lombok, no field `@Autowired`).
- Table names are singular `snake_case` (`app_user`), matching the library tables (`spring_session`, `flyway_schema_history`). Avoid Postgres reserved words such as `user`.
- Take the current user's identity from `Authentication` or `@AuthenticationPrincipal`, never from a request parameter.
- Every new endpoint needs a rule in `SecurityConfig` and MockMvc tests for the allowed role, the denied role (403), and anonymous (401).
- Spring tests are annotated `@IntegrationTest` (Testcontainers + `test` profile), never bare `@SpringBootTest`. Send CSRF tokens on mutating requests with `SpaCsrf.token(mvc)`. **Never use spring-security-test's `csrf()`**: it swaps the shared `CsrfFilter` repository and silently breaks `XSRF-TOKEN` cookies for every later test.
- For business-rule failures, throw `ApiException`, or `ApiException.field(status, field, msg)` for form errors. Never put submitted values in messages.
