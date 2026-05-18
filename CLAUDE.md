# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CXP (Civic Exchange Platform) is a Spring Boot 3.3 / Java 17 backend for a civic exchange platform. Citizens submit complaints/service requests; the platform enriches them with AI, routes them to the correct ward, and syncs them asynchronously to other systems. External systems remain the System of Record — CXP is the engagement and enrichment layer.

## Commands

### Local development

Start the database and AI service (required before running the app):
```bash
docker compose up postgres ollama ai -d
```

Run the application:
```bash
./mvnw spring-boot:run
```

The server starts on port **8081** (configured in `application.yml`).

### Build

```bash
./mvnw clean package          # full build with tests
./mvnw clean package -DskipTests   # skip tests
```

### Tests

```bash
./mvnw test                                    # all tests
./mvnw test -Dtest=ArchitectureTest            # single test class
./mvnw test -Dtest=ArchitectureTest#aiMustNotDependOnComplaintRepository  # single method
```

Tests use H2 in-memory DB (`src/test/resources/application.properties`). Architecture rules are enforced via ArchUnit in `ArchitectureTest`.

### Database migrations

Flyway runs automatically on startup. Migration scripts live in `src/main/resources/db/migration/`. Hibernate is set to `ddl-auto: validate` — **never** create or alter tables through JPA, only through Flyway.

## Architecture

### Hexagonal / DDD module layout

Each domain module follows the same internal structure:

```
<module>/
  domain/          # entities, enums — no external dependencies
  application/     # services orchestrating use cases
  api/ or web/     # controllers and DTOs (HTTP layer)
  port/            # interfaces for external adapters
  repository/      # Spring Data repositories
  infrastructure/  # adapter implementations (Salesforce, OTP, etc.)
```

Current modules: `complaint`, `servicerequest`, `ai`, `auth`, `governance`, `location`, `integration/salesforce`, `common`.

### ArchUnit-enforced module boundaries

Cross-module repository access is forbidden — enforced by `ArchitectureTest`:
- `complaint` must not access `ai.repository`
- `ai` must not access `complaint.repository`
- External modules must not access `complaint.repository` or `ai.repository` directly

Violations will fail the test suite.

### Complaint / ServiceRequest creation flow

Both follow the same invariant — **external sync must never block intake**:

1. Resolve location routing (geocoding + ward lookup via `LocationIntelligenceService`)
2. AI classifies priority via `AiClassificationService` → `AiClient` (Python sidecar)
3. Save entity locally (`complaintRepository.save`)
4. Record an event (`ComplaintEvent` / `complaint_event` table — immutable, never delete)
5. Publish a Spring `ApplicationEvent` (`ComplaintCreatedEvent`)
6. `SalesforceCaseSyncWorker` picks it up on a 60-second scheduled poll, retries up to 3 times with backoff

### Multi-tenancy

`TenantContext` (ThreadLocal) carries the `governingBodyId` (UUID) for the current request. It is populated from the JWT claims and must be set before any repository call. Every service-layer query must be tenant-scoped — use `findByIdAndGoverningBodyId` / `findByGoverningBodyId*` variants, never bare `findAll` or `findById`.

### Authentication

OTP-based (mobile number + device ID). Flow:
- `POST /api/v1/auth/otp/request` → generates OTP, stores only the hash, never plaintext
- `POST /api/v1/auth/otp/verify` → issues short-lived JWT access token + long-lived refresh token
- `JwtAuthenticationFilter` validates bearer tokens and populates `SecurityContext` + `TenantContext`
- Public endpoints: `/auth/**`, `/api/v1/auth/**`, `/actuator/health`

JWT keys: RSA keypair in `src/main/resources/keys/`. Salesforce integration uses a separate JWT service (`SalesforceJwtService`) with its own private key.

### AI integration

`AiClient` (in `ai/port/`) calls an external Python/Ollama sidecar (`AI_SERVICE_URL`, default `http://localhost:8000`). The AI layer is **enrichment only** — it cannot change complaint status, close records, or trigger escalations autonomously. AI output must always include a confidence score.

### Idempotency

`ComplaintService.createComplaint` enforces tenant-scoped idempotency via `IdempotencyRequest` — callers pass an `Idempotency-Key` header; duplicate requests within 24 hours return the cached response.

## Key configuration

| Property | Default | Notes |
|---|---|---|
| `server.port` | 8081 | |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/cxp` | Override via env |
| `AI_SERVICE_URL` | `http://localhost:8000` | Python sidecar |
| JWT access token expiry | 30 minutes | |
| JWT refresh token expiry | 30 days | |

## Non-negotiables from the Architecture Manifesto

- Domain entities must not import Salesforce or vendor SDK classes
- Synchronous external calls that block complaint/service-request save are forbidden
- `ComplaintEvent` records are immutable — never delete or rewrite
- AI must not autonomously change lifecycle state
- Navigation in the mobile app must be metadata-driven (AI returns `actions[]` JSON), never parsed from natural language
- All new schema changes must go through a Flyway migration script; never rely on `ddl-auto`
