# CIRM – Backend Task Template

## Feature Name
[Example: Deep link AI actions to ComplaintDetail]

---

## Objective

Describe what this backend change must achieve.

---

## Domain Layer Impact

Entities impacted:
[List entities]

New fields?
[Yes/No – If yes, list]

Does this require:
- New domain event?
- New repository method?
- New service?
- Adapter change?

---

## Integration Impact

Does this affect:
- ExternalCasePort?
- SalesforceAdapter?
- Sync logic?
- Retry mechanism?

Must remain asynchronous unless explicitly required.

---

## AI Contract Impact

Does this modify AI structured output?

If yes, define new JSON contract clearly.

Example:

{
  content: string,
  confidence: number,
  actions: [
    {
      type: "navigate",
      target: "ComplaintDetail",
      params: { complaintId: string },
      label: string
    }
  ]
}

---

## API Changes

Endpoint:
Method:
Request DTO:
Response DTO:

Must remain JWT protected if applicable.

---

## Database Changes

Migration required?
Provide migration script.

Must preserve existing data.

---

## Non-Negotiables

- Complaint must be saved before external sync.
- Domain must not depend on vendor SDK.
- No business logic inside controller.
- No synchronous external blocking calls.

---

## Deliverables

- Updated entity (if any)
- Migration script
- Service logic
- Controller
- DTOs
- Adapter changes (if needed)
