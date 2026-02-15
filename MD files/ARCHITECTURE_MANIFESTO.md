# CIRM – Architecture Manifesto
Version: 1.0
Status: Non-Negotiable

---

## 1. System Identity

CIRM (Citizen Relationship Management) is a Civic Intelligence & Engagement Platform.

It is:
- A System of Engagement
- An AI enrichment layer
- An integration-friendly civic interface

It is NOT:
- A replacement for Salesforce or any external case system
- A workflow engine substitute
- A chatbot experiment

External systems remain the System of Record.

---

## 2. Architectural Principles

### 2.1 Separation of Concerns

Mobile:
- UI rendering only
- No business logic
- No integration logic
- No classification logic

Backend (Spring Boot):
- Owns domain logic
- Owns persistence
- Owns integration orchestration
- Owns AI orchestration

Database:
- Durable storage
- Event tracking
- Audit foundation

AI:
- Enrichment only
- Structured output only
- Never lifecycle authority

---

### 2.2 Hexagonal Architecture (Ports & Adapters)

Domain layer:
- Must not import Salesforce classes
- Must not depend on external vendor SDKs

External integration:
- Implemented via adapters
- Bound through interfaces (e.g., ExternalCasePort)

No vendor leakage into domain layer.

---

### 2.3 Local-First Durability

Complaint flow must always:

1. Save locally
2. Log ComplaintEvent
3. Publish domain event
4. Trigger external sync asynchronously

External system availability must NEVER block complaint intake.

---

### 2.4 Eventual Consistency

CIRM operates under eventual consistency with external systems.

Strong consistency is not required at submission stage.

Retry mechanisms must exist for external sync failures.

---

## 3. Domain Model Invariants

### Complaint

Required properties:
- id
- title
- description
- ward
- category
- priority
- status
- externalSystem
- externalReferenceId
- externalSyncStatus

Status values:
- OPEN
- IN_PROGRESS
- RESOLVED
- REJECTED

Complaint must always be stored before sync.

---

### ComplaintEvent

Tracks lifecycle changes:
- CREATED
- STATUS_CHANGED
- SYNCED
- RESOLVED

All state transitions must create an event record.

---

### AIConversation / AIMessage

- Conversations persisted permanently
- UI displays session-level only
- AI responses must include structured metadata

---

## 4. AI Output Contract

AI must return structured JSON:

{
  content: string,
  confidence: number,
  actions: Action[]
}

Action format:

{
  type: "navigate",
  target: string,
  label: string
}

Frontend must NEVER parse natural language to determine navigation.

Navigation must be metadata-driven.

---

## 5. Governance Constraints

AI:
- Cannot close complaints
- Cannot change complaint status autonomously
- Cannot escalate without rule-based validation

AI is assistive, not authoritative.

Confidence score must always be exposed if available.

---

## 6. Security Constraints

- JWT required for all protected endpoints
- One-device-per-user enforcement
- Token version invalidation supported
- No business logic in mobile layer

---

## 7. Non-Negotiables

The following are forbidden:

- Hardcoding Salesforce logic inside domain entities
- Moving integration logic into controllers
- Synchronous external calls that block complaint save
- Parsing AI text to trigger navigation
- Removing ComplaintEvent tracking
- Mixing mobile and backend responsibilities

If simplification is suggested, architecture integrity must be preserved.

---

## 8. Expansion Readiness

All new features must:

- Preserve hexagonal boundaries
- Preserve event-driven sync
- Preserve AI metadata structure
- Maintain vendor neutrality
- Maintain governance alignment

No architectural shortcuts for speed.

---

End of Manifesto.
