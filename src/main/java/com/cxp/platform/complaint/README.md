# Module: complaint

## Responsibility
Owns complaint intake, lifecycle tracking, and immutable complaint audit events.

## Owned Entities
- Complaint
- ComplaintEvent

## Exposed Services
- ComplaintService
- ComplaintEventService

## External Dependencies
- ai (classification)
- location (routing)
- governance (ward lookup)
- integration (external sync ports)
