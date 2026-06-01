# Module: governance

## Responsibility

Owns governance master data entities and the read-only hierarchy discovery API.
All entities are reference/master data seeded via Flyway migrations.
No write operations are exposed — governance data is changed only through controlled migrations.

## Hierarchy

```
state → district → city → governing_body → zone → ward
                                         → department
```

## Owned Entities

- `State`           — Indian states (e.g. Uttarakhand)
- `District`        — Administrative districts within a state
- `City`            — Cities/towns within a district (filterable by is_active)
- `GoverningBody`   — Civic authority administering a city (Municipal Corporation, Nagar Panchayat, etc.)
- `Ward`            — Atomic civic unit; belongs to exactly one governing body
- `Department`      — Functional department within a governing body (governing-body-scoped, not global)

## Exposed APIs

`GeographyController` — read-only hierarchy traversal:

```
GET /api/v1/states
GET /api/v1/states/{stateId}/districts
GET /api/v1/districts/{districtId}/cities
GET /api/v1/cities/{cityId}/governing-bodies
GET /api/v1/governing-bodies/{governingBodyId}/wards
GET /api/v1/governing-bodies/{governingBodyId}/departments
```

All endpoints return `{ "id": "<uuid>", "name": "<string>" }` items, alphabetically ordered.
404 means unknown parent. 200 + [] means known parent with no children.

## External Dependencies

- `common`
