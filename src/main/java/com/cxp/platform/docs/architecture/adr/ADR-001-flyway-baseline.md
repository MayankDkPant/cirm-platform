# ADR-001 — Flyway Baseline Migration Strategy

## Status
Accepted

## Context

The CIRM platform uses Flyway for database schema versioning.

When Flyway was introduced, the database had no prior migration history.
We needed a clean starting point for version-controlled schema evolution.

Flyway provides a CLI "baseline" command, but this approach stores the
baseline state outside the code repository and makes environment setup
less reproducible.

We want a fully reproducible setup where any developer or CI pipeline can
create the database from scratch using only the code repository.

## Decision

We created an empty migration file:

V1__baseline.sql

This file serves as the starting point of database version history.

All future schema changes will be implemented using sequential Flyway
migrations (V2, V3, V4, ...).

Migration files must never be modified or deleted once applied.

## Consequences

### Positive
- Fully reproducible environments (dev, QA, prod)
- No manual Flyway baseline command required
- All database history stored in Git
- CI/CD pipelines can build database from scratch
- Clear starting point for schema evolution

### Negative
- The first migration file does not contain schema changes
- Requires discipline to never modify past migrations

## Alternatives Considered

### Use Flyway CLI baseline command
Rejected because:
- Baseline would live outside version control
- Harder onboarding for new developers
- CI/CD pipelines would require manual baseline step

## Decision Date
2026-02-15
