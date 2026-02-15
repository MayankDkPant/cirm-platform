# CIRM – Standard AI Development Prompt Template

Before generating any code:

1. Read `/docs/ARCHITECTURE_MANIFESTO.md`.
2. Confirm you understand and will not violate its constraints.
3. Do not simplify architecture unless explicitly instructed.
4. Do not move business logic into incorrect layers.

---

## Task Overview

Feature Name:
[Clearly state feature]

Goal:
[Describe business objective in 2–3 lines]

---

## Layer Impacted

- [ ] Mobile (UI only)
- [ ] Backend (Domain / Service / Controller)
- [ ] Integration Adapter
- [ ] Database Schema
- [ ] AI Orchestration
- [ ] Navigation Layer

---

## Domain Context (Provide Only What’s Relevant)

Entities involved:
[Paste relevant entity definitions]

API contracts involved:
[Paste request/response structures]

Navigation targets:
[If frontend feature]

---

## Architectural Constraints

- Must preserve hexagonal architecture.
- Must not leak external vendor logic into domain.
- Must not block complaint persistence on external calls.
- Must preserve structured AI metadata model.
- Must not parse natural language for actions.

---

## Deliverables

Specify exactly what to generate:

- Controller?
- Service method?
- DTO?
- Migration?
- React component?
- Type definition?

---

## Output Format

- Provide only relevant files.
- Show complete code for changed files.
- Do not include explanation unless requested.
- Highlight schema changes clearly.
