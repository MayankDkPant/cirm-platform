# MOBILE_INTEGRATION_CONTEXT.md

Reference document for the React Native frontend team integrating with the CIRM platform backend.

**Base URL:** `http://<host>:8081`  
**Content-Type:** `application/json` for all requests and responses.

---

## 1. Complaint & Service Request APIs

CIRM has two parallel complaint surfaces. Prefer the versioned ServiceRequest API for new work.

### 1.1 ServiceRequest API (canonical, versioned)

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/service-requests` | Required | Create a service request |
| `GET` | `/api/v1/service-requests` | Required | List all (tenant-scoped) |
| `GET` | `/api/v1/service-requests/{id}` | Required | Get by ID |
| `PATCH` | `/api/v1/service-requests/{id}/status` | Required | Update status |

**Create request body (`ServiceRequestCreateRequest`):**
```json
{
  "type": "COMPLAINT",          // required — COMPLAINT | SERVICE | QUERY | EMERGENCY
  "title": "Broken streetlight", // required
  "description": "Street light at MG Road has been off for 3 days.", // required
  "category": "Infrastructure",  // optional
  "priority": "HIGH",            // optional (AI may derive this)
  "addressText": "MG Road, Ward 12", // optional — freetext address
  "latitude": 18.5204,           // optional — enables ward auto-resolution
  "longitude": 73.8567,          // optional
  "departmentId": "uuid",        // optional
  "aiConversationId": "uuid"     // optional — link to an AI session
}
```

**Response (`ServiceRequestResponse`):**
```json
{
  "id": "uuid",
  "type": "COMPLAINT",
  "title": "Broken streetlight",
  "description": "...",
  "category": "Infrastructure",
  "priority": "HIGH",
  "status": "OPEN",
  "addressText": "MG Road, Ward 12",
  "latitude": 18.5204,
  "longitude": 73.8567,
  "departmentId": "uuid",
  "wardId": "uuid",              // derived by backend — do not send
  "wardName": "Ward 12",         // derived by backend — do not send
  "createdAt": "2026-05-18T10:30:00Z"
}
```

**Status update request:**
```json
{ "status": "IN_PROGRESS" }
```

### 1.2 Legacy Complaint Compatibility API

`POST /api/v1/complaints` — accepts `LegacyComplaintCreateRequest`. Internally delegates to the ServiceRequest service with `type=COMPLAINT`. Use for backward compatibility only; prefer `/api/v1/service-requests`.

### 1.3 Legacy Complaint API (unversioned — being replaced)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/complaints/ai` | Submit complaint after AI conversation |
| `POST` | `/api/complaints/manual` | Submit complaint without AI |

These will be removed. New features should not depend on them.

### 1.4 Validation rules

| Field | Rule |
|---|---|
| `type` | Must be one of the `ServiceRequestType` enum values |
| `title` | Non-blank, required |
| `description` | Non-blank, required |
| `latitude` / `longitude` | Both required together for ward resolution; omit both if location unknown |
| `aiConversationId` | Must match an existing `AiConversation` ID if provided |

---

## 2. Authentication Flow

CIRM uses mobile number + OTP authentication. No passwords.

### 2.1 Step 1 — Request OTP

```
POST /api/v1/auth/request-otp
(no auth required)
```

Request:
```json
{
  "mobileNumber": "9876543210",  // Indian mobile format [6-9]\d{9}
  "deviceId": "device-uuid-or-string",
  "deviceName": "Pixel 7"
}
```

Response:
```json
{
  "otpReference": "uuid",   // use this in the verify step
  "otp": "123456"           // DEV ONLY — will be removed when SMS is wired
}
```

Constraints:
- Resend cooldown: **30 seconds**
- OTP validity: **2 minutes**
- Max verification attempts: **5**
- One active OTP per mobile number — previous OTP is invalidated on new request

### 2.2 Step 2 — Verify OTP (endpoint in progress)

The `VerifyOtpRequest` DTO is defined but the `/api/v1/auth/verify-otp` endpoint is not yet exposed. Use the mock login endpoint during development:

```
POST /auth/mock-login?email=<email>
```

Returns `{ "accessToken": "...", "refreshToken": "..." }`.

Production verify flow (once implemented):
```json
// POST /api/v1/auth/verify-otp
{
  "otpReference": "uuid",   // from step 1
  "otp": "123456"
}
```

Expected response (`AuthTokenResponse`):
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "tokenId.eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 1800          // seconds (30 minutes)
}
```

### 2.3 JWT handling

- Token type: **HS256** (symmetric, signed by backend)
- Access token lifetime: **30 minutes**
- Refresh token lifetime: **30 days**

JWT claims included in the access token:
```json
{
  "sub": "user-uuid",
  "phone": "9876543210",
  "roles": ["CITIZEN"],
  "governing_body_id": "uuid",   // tenant identifier
  "token_type": "ACCESS",
  "iat": 1716000000,
  "exp": 1716001800
}
```

Only tokens with `token_type = ACCESS` are accepted on protected endpoints. Do **not** use the refresh token to call API endpoints — it will be rejected.

### 2.4 Token refresh

Refresh token format: `{tokenId}.{rawJwt}` — store the full string and send it as-is. Token rotation is applied on every refresh — the old refresh token is revoked and a new one is issued. The refresh endpoint is not yet exposed; implement proactive token renewal on the client (refresh ~5 minutes before expiry).

### 2.5 Sending the access token

```
Authorization: Bearer eyJ...
```

Every request to `/api/**` requires this header.

### 2.6 Public vs protected endpoints

| Path pattern | Auth required |
|---|---|
| `/auth/**` | No |
| `/api/v1/auth/**` | No |
| `/actuator/health` | No |
| `/api/**` (everything else) | Yes |

---

## 3. Multi-Tenancy

CIRM is multi-tenant per `governing_body` (municipality). **The mobile app does not send a tenant header.** Tenant identity is derived entirely from the JWT claim `governing_body_id`.

- The filter `JwtAuthenticationFilter` extracts `governing_body_id` from the access token and binds it to `TenantContext` for the duration of the request.
- Every data query is automatically scoped to the tenant from the token — the app can only ever see data for its own municipality.
- If a resource is requested for an ID that belongs to a different tenant, the API returns `404` (not `403`) to avoid leaking cross-tenant existence.

No additional headers needed. Token = tenant.

---

## 4. Complaint Lifecycle

### 4.1 ServiceRequest statuses

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
               └→ REJECTED
               └→ DUPLICATE
```

| Status | Meaning |
|---|---|
| `OPEN` | Submitted, awaiting assignment |
| `IN_PROGRESS` | Being worked on |
| `RESOLVED` | Work completed |
| `CLOSED` | Formally closed (resolvedAt is set) |
| `REJECTED` | Not actionable |
| `DUPLICATE` | Duplicate of an existing request |

`resolvedAt` is set automatically by the backend when status transitions to `RESOLVED` or `CLOSED`. Do not send it from the client.

### 4.2 Legacy Complaint statuses

The older `Complaint` domain uses a narrower set: `OPEN`, `REJECTED`, `DUPLICATE`, `CLOSED`. This is for the unversioned `/api/complaints` endpoints only.

### 4.3 Priority values

`LOW` | `MEDIUM` | `HIGH` — backend assigns via AI if not provided.

### 4.4 Salesforce sync status (read-only metadata)

Not currently returned in the public API response, but internally tracked as `PENDING → SUCCESS | FAILED`. External sync happens asynchronously in the background. The mobile app does not need to poll or handle this.

---

## 5. File Upload Architecture

**Not yet implemented.** Cloudflare R2 signed-URL upload and attachment metadata flow are planned but no endpoints exist. This section will be populated when the feature is built. Do not build client-side file upload logic against production until the contract is defined.

---

## 6. GIS / Ward Resolution Flow

### 6.1 What the app sends

```json
{
  "latitude": 18.5204,    // WGS-84 decimal degrees, Double
  "longitude": 73.8567,   // WGS-84 decimal degrees, Double
  "addressText": "..."    // optional human-readable fallback
}
```

Always send both `latitude` and `longitude` together or omit both. Sending one without the other will result in incomplete routing.

### 6.2 What the backend derives

The backend runs `LocationIntelligenceService` synchronously during complaint/service-request creation:
1. Reverse geocodes coordinates → `formattedAddress`
2. Looks up which ward the coordinates fall in → `wardId`, `wardName`
3. Resolves the governing body (municipality) from the ward

### 6.3 Derived fields in response

```json
{
  "wardId": "uuid",
  "wardName": "Ward 12 - Shivajinagar",
  "formattedAddress": "MG Road, Shivajinagar, Pune, Maharashtra 411005"
}
```

Do not pre-fill or hardcode `wardId` on the client — always let the backend derive it from coordinates. The ward snapshot at creation time is persisted for audit and immutable afterwards.

**Current state:** Ward lookup uses a mock implementation (`MockGeocodingClient`, `MockWardLookupService`). Real GIS integration is not yet connected. Expect placeholder values in non-production environments.

---

## 7. AI Enrichment Flow

### 7.1 Analyze before submission (synchronous)

Use this to get AI classification suggestions before the user confirms submission.

```
POST /api/v1/ai/service-requests/analyze   (authenticated)
```

Request:
```json
{
  "text": "There is a large pothole on the main road near my house.",
  "latitude": 18.5204,    // optional
  "longitude": 73.8567    // optional
}
```

Response (`AiServiceRequestAnalyzeResponse`):
```json
{
  "type": "COMPLAINT",         // ServiceRequestType enum
  "category": "Roads",
  "priority": "HIGH",
  "confidence": 0.87           // 0–1 float; display as percentage
}
```

This is a **read-only** call. It does not create any record. Use it to pre-fill the submission form and show the user AI suggestions with the confidence score. The user must confirm before submission.

### 7.2 AI-assisted submission

After the user confirms the AI-suggested details, submit with `aiConversationId` included:

```json
{
  "type": "COMPLAINT",
  "title": "Pothole on main road",
  "description": "...",
  "latitude": 18.5204,
  "longitude": 73.8567,
  "aiConversationId": "uuid-from-ai-session"
}
```

Including `aiConversationId` sets `source = AI_ASSISTED` on the record.

### 7.3 What AI can and cannot do

| AI can | AI cannot |
|---|---|
| Suggest type, category, priority | Change status of a complaint |
| Return navigation actions as metadata | Close or escalate autonomously |
| Generate structured draft text | Modify database state directly |

AI `actions` in future chat responses will look like:
```json
{
  "type": "navigate",
  "target": "complaint-detail",
  "label": "View your complaint",
  "params": { "id": "uuid" }
}
```

**Never parse AI text for navigation.** Only act on the `actions[]` array. If `actions` is empty or absent, render no navigation.

### 7.4 Chat endpoint (experimental)

`POST /ai/chat` exists but returns a mock reply. Do not build production flows against it.

`POST /api/ai/complaint/analyze` is defined but throws `UnsupportedOperationException` — do not call it.

---

## 8. API Conventions

### 8.1 Error format

All errors return a consistent `ApiError` envelope:

```json
{
  "timestamp": "2026-05-18T10:30:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Mobile number must be a valid Indian mobile number",
  "path": "/api/v1/auth/request-otp"
}
```

| Status | When |
|---|---|
| `400` | Validation failure, business rule violation, OTP cooldown |
| `401` | Missing or invalid/expired access token |
| `404` | Resource not found (or cross-tenant access attempt) |
| `500` | Unexpected backend error (message is generic) |

### 8.2 Pagination

Not currently implemented. All list endpoints (`GET /api/v1/service-requests`) return flat arrays. Implement client-side limiting until server-side pagination is added.

### 8.3 Response envelopes

There is no wrapper envelope. Successful responses return the resource or array directly — no `{ "data": ... }` wrapper.

### 8.4 Idempotency

The legacy `ComplaintService.createComplaint` path supports idempotency via an `Idempotency-Key` header (UUID string). Duplicate submissions within **24 hours** using the same key and tenant return the cached response without creating a new record.

The `/api/v1/service-requests` endpoint does not yet have idempotency headers — generate a unique request on each user tap and rely on UI-level duplicate prevention for now.

### 8.5 Timestamps

All timestamps are returned as **ISO-8601 UTC** strings (`Instant.toString()` format, e.g. `2026-05-18T10:30:00Z`). Parse with a timezone-aware library. Do not display raw epoch values.

---

## 9. Recommended Frontend DTO Alignment

```typescript
// Enums — keep in sync with backend
type ServiceRequestType = 'COMPLAINT' | 'SERVICE' | 'QUERY' | 'EMERGENCY';
type ServiceRequestStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'REJECTED' | 'DUPLICATE';
type Priority = 'LOW' | 'MEDIUM' | 'HIGH';

interface ServiceRequestCreateRequest {
  type: ServiceRequestType;
  title: string;
  description: string;
  category?: string;
  priority?: Priority;
  addressText?: string;
  latitude?: number;
  longitude?: number;
  departmentId?: string;      // UUID
  aiConversationId?: string;  // UUID
}

interface ServiceRequestResponse {
  id: string;
  type: ServiceRequestType;
  title: string;
  description: string;
  category?: string;
  priority?: Priority;
  status: ServiceRequestStatus;
  addressText?: string;
  latitude?: number;
  longitude?: number;
  departmentId?: string;
  wardId?: string;
  wardName?: string;
  createdAt: string;          // ISO-8601
}

interface AiAnalyzeResponse {
  type: ServiceRequestType;
  category: string;
  priority: Priority;
  confidence: number;         // 0–1, display as Math.round(confidence * 100) + '%'
}

interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

---

## 10. End-to-End Complaint Creation (AI-Assisted Flow)

### Step 1 — User types a description; call AI analyze

```
POST /api/v1/ai/service-requests/analyze
Authorization: Bearer eyJ...

{ "text": "Big pothole near MG Road bus stop, very dangerous at night", "latitude": 18.5204, "longitude": 73.8567 }
```

Response:
```json
{ "type": "COMPLAINT", "category": "Roads", "priority": "HIGH", "confidence": 0.91 }
```

Show the user the suggestions and confidence ("91% confident") and allow edits.

### Step 2 — User confirms; submit the service request

```
POST /api/v1/service-requests
Authorization: Bearer eyJ...

{
  "type": "COMPLAINT",
  "title": "Pothole on MG Road near bus stop",
  "description": "Big pothole near MG Road bus stop, very dangerous at night",
  "category": "Roads",
  "priority": "HIGH",
  "addressText": "MG Road Bus Stop, Pune",
  "latitude": 18.5204,
  "longitude": 73.8567
}
```

Response (`201` or `200`):
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "type": "COMPLAINT",
  "title": "Pothole on MG Road near bus stop",
  "description": "Big pothole near MG Road bus stop, very dangerous at night",
  "category": "Roads",
  "priority": "HIGH",
  "status": "OPEN",
  "addressText": "MG Road Bus Stop, Pune",
  "latitude": 18.5204,
  "longitude": 73.8567,
  "departmentId": null,
  "wardId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "wardName": "Ward 12 - Shivajinagar",
  "createdAt": "2026-05-18T10:31:05.123Z"
}
```

Navigate the user to the complaint detail screen using `id`. Backend will sync to Salesforce asynchronously — no polling needed.
