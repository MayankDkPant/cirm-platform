# CIRM – Domain Model Guide
Version: 1.0
Status: Strict Governance

---

# 1. Purpose

This document governs how domain entities are designed, extended, and migrated.

AI-generated code must follow these rules.

Domain model stability is more important than feature velocity.

---

# 2. Domain Design Principles

## 2.1 Domain First, Integration Later

Domain entities must:

- Be independent of external vendor logic
- Contain only business-relevant properties
- Not include transport-specific annotations unless required

No Salesforce-specific fields beyond:
- externalSystem
- externalReferenceId
- externalSyncStatus

Never embed vendor SDK types inside domain entities.

---

## 2.2 Persistence Discipline

All domain entities must:

- Have id
- Have createdAt
- Have updatedAt
- Be auditable

Timestamps must be backend-generated.

Never rely on client timestamps.

---

## 2.3 Explicit State Modeling

Statuses must always be enums.

Example:

ComplaintStatus:
- OPEN
- IN_PROGRESS
- RESOLVED
- REJECTED

Never use free-text status.

Never allow dynamic string states.

---

# 3. Complaint Entity Rules

Complaint is the core aggregate root.

It must:

- Be saved before external sync
- Log ComplaintEvent on creation
- Log ComplaintEvent on status change
- Not contain business logic for sync

Required fields:

- id
- title
- description
- ward
- category
- priority
- status
- latitude (optional)
- longitude (optional)
- externalSystem
- externalReferenceId
- externalSyncStatus
- createdAt
- updatedAt
- resolvedAt (optional)

externalSyncStatus must be enum:
- PENDING
- SUCCESS
- FAILED

---

# 4. Event Tracking Model

ComplaintEvent must:

- Reference complaintId
- Store eventType
- Store timestamp
- Be immutable

Event types must be enum.

Event table must never be deleted or rewritten.

This is governance audit foundation.

---

# 5. AI Domain Entities

AIConversation:

- id
- userId
- sessionId
- createdAt

AIMessage:

- id
- conversationId
- role (USER / ASSISTANT)
- content
- confidence
- metadata (JSON)
- createdAt

metadata must store structured action data.

Never store raw navigation logic outside metadata.

---

# 6. Migration Rules

When modifying schema:

1. Do not delete existing columns without migration plan.
2. Do not rename fields without backward compatibility review.
3. Provide migration scripts explicitly.
4. Ensure existing data integrity.
5. Avoid destructive migrations in early phases.

AI-generated migrations must be reviewed manually.

---

# 7. Aggregate Boundaries

Complaint is an aggregate root.

Other entities must not:

- Modify Complaint state directly
- Bypass service layer
- Trigger sync outside defined domain events

All state transitions must occur through service layer.

---

# 8. Service Layer Discipline

Controllers must:

- Be thin
- Delegate to services
- Not contain business logic

Services must:

- Enforce invariants
- Trigger events
- Handle transaction boundaries

Repositories must:

- Only persist and fetch
- Not contain business logic

---

# 9. Naming Conventions

- Entities: PascalCase
- Tables: snake_case
- Enums: UPPERCASE
- Fields: camelCase
- DTOs must not be reused as entities

Avoid ambiguous names.

Example:
Use:
ComplaintPriority
Not:
Level

Use:
externalSyncStatus
Not:
sync

---

# 10. Backward Compatibility Policy

Once mobile consumes an API:

- Do not remove fields silently.
- Add new optional fields only.
- Version API if breaking change required.

Frontend types must be updated alongside backend contract.

---

# 11. Anti-Patterns (Forbidden)

The following are not allowed:

- Business logic inside entity class
- Direct database updates bypassing service layer
- Sync logic inside controller
- Hardcoding external system behavior
- AI modifying complaint state directly
- Free-text status or priority values
- JSON blobs replacing structured columns unnecessarily

---

# 12. Evolution Protocol

When extending domain:

1. Update this guide if needed.
2. Define invariant clearly.
3. Add enum if new state introduced.
4. Create migration script.
5. Update tests.
6. Review integration impact.
7. Validate against ARCHITECTURE_MANIFESTO.md.

No silent schema drift allowed.

---

End of Domain Model Guide.
