# CLAUDE.md — backend

Backend-specific conventions. Inherits everything in the root `../CLAUDE.md`
(repo layout, env-per-subproject, contract-first, English-only). Rules here add to
or refine those.

## Stack
Spring Boot 3.3.x / Java 21 / Maven. Spring Security + JWT (stateless), Spring Data
JPA + Flyway (Postgres in prod, H2 in tests), Redis, springdoc-openapi (Swagger UI +
`/v3/api-docs`). Build/quality via Checkstyle + SpotBugs + JaCoCo.

## Conventions (must follow)

### 1. No hand-written boilerplate — Lombok for entities, records for DTOs
Do not hand-write getters/setters/constructors.
- **JPA entities** → use **Lombok**. Entities are mutable and need a no-arg
  constructor, so they cannot be records. Prefer `@Getter` plus field-level
  `@Setter` on the *mutable* fields only (keep identity/audit fields such as `id`
  and `createdAt` read-only); add `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
  for JPA. **Do not use `@Data`/`@EqualsAndHashCode`/`@ToString` on entities** —
  generated `equals`/`hashCode` over mutable/lazy fields breaks JPA identity. Write
  domain constructors by hand when they carry logic.
- **DTOs / API request+response types** → use Java **records** (immutable, compact).
  Validation annotations (`@NotBlank`, `@Email`, …) go on record components.
- **Error/value types** → records.

Lombok is wired up in `pom.xml` (an `optional` dependency, excluded from the fat jar
by the spring-boot plugin). `lombok.config` sets `lombok.addLombokGeneratedAnnotation`
so SpotBugs and JaCoCo skip generated methods — don't remove it.

### 2. Contract-first: the API is generated from this code
This backend is the single source of truth for the API (root `../CLAUDE.md` §4).
Keep the OpenAPI output complete and correct so frontend codegen stays accurate:
- Annotate controllers with `@Tag`/`@Operation`; protected endpoints with
  `@SecurityRequirement(name = "bearerAuth")`.
- Put validation annotations on DTOs so they surface in the spec.
- `OpenApiSpecExportTest` writes `target/openapi.json` during `./mvnw verify` (H2, no
  running server/DB). If you change an endpoint, the frontend regenerates from this.

### 3. Security returns 401 (not 403) for unauthenticated requests
The API is stateless JWT. `SecurityConfig` wires an `HttpStatusEntryPoint(UNAUTHORIZED)`
so missing/invalid credentials yield **401**, not Spring's default 403. Keep
`/api/auth/**`, actuator health, and swagger/api-docs paths permitted; everything else
requires authentication. `AuthFlowIntegrationTest` guards this.

### 4. Persistence: Flyway owns the schema; JPA only validates it
Schema changes go in a new `src/main/resources/db/migration/V__*.sql` migration —
never rely on Hibernate `ddl-auto` to mutate prod (it is `validate`). Tests use H2
with `create-drop` and Flyway disabled.

### 5. Keep the quality gates green
`./mvnw verify` must pass: Checkstyle (`checkstyle.xml`), SpotBugs
(`spotbugs-exclude.xml` — only add exclusions for genuine false positives, e.g.
Spring DI `EI_EXPOSE_REP2`), and JaCoCo (line threshold `${jacoco.line.coverage}`,
gentle for P0; the core domain — bidding/settlement/payment — will be raised to 90%+).

## Commands
```bash
./mvnw verify        # tests + Checkstyle + SpotBugs + JaCoCo + export openapi.json
./mvnw spring-boot:run   # run locally (needs Postgres/Redis or the compose stack)
```
