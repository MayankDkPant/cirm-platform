/* 
====================================================================
MIGRATION: V15__complaint_lifecycle_enrichment.sql
AUTHOR: CIRM Platform
DATE: 2026-02-20

PURPOSE
--------------------------------------------------------------------
This migration upgrades the complaints table to support the full 
enterprise Complaint Lifecycle architecture.

This is a foundational multi-tenant and AI-enablement migration.

WHY THIS CHANGE IS REQUIRED
--------------------------------------------------------------------
The initial complaints table was created during the platform bootstrap
phase and lacks key fields required for:

• Multi-tenant SaaS support (state + municipality ownership)
• Future duplicate detection and clustering
• Human readable address storage
• AI classification analytics and auditability
• Integration routing and data partitioning readiness

This migration is NON-DESTRUCTIVE and backward compatible.

====================================================================
*/


/* ================================================================
   1. MULTI-TENANT OWNERSHIP FIELDS
   ----------------------------------------------------------------
   Every complaint must belong to:
   • State (for integration routing)
   • Municipality (for tenant isolation)

   This enables:
   - External system orchestration by state
   - Data partitioning & analytics
   - Multi-city SaaS rollout
================================================================ */

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS municipality_id UUID;

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS state_id UUID;


/* ================================================================
   2. HUMAN READABLE ADDRESS
   ----------------------------------------------------------------
   Latitude/Longitude alone is not user-friendly.

   address_text stores reverse geocoded or user-provided address
   from the mobile app.

   This avoids future costly GIS migrations.
================================================================ */

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS address_text TEXT;


/* ================================================================
   3. DUPLICATE COMPLAINT HOOK
   ----------------------------------------------------------------
   Placeholder for future AI duplicate detection and clustering.

   Example future use:
   Complaint B -> duplicate_of_complaint_id -> Complaint A

   Adding now avoids future data migrations.
================================================================ */

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS duplicate_of_complaint_id UUID;


/* ================================================================
   4. AI CLASSIFICATION STORAGE
   ----------------------------------------------------------------
   AI classification always runs on complaint creation.

   IMPORTANT:
   - User input may override AI decisions.
   - We STILL store AI suggestions for:
       • Model evaluation
       • Training datasets
       • Analytics dashboards
       • Audit trail

   These fields store FINAL AI OUTPUTS (not raw tokens).
================================================================ */

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS ai_suggested_department VARCHAR(255);

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS ai_suggested_priority VARCHAR(50);

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS ai_suggested_category VARCHAR(255);


/* ================================================================
   5. INDEXES FOR FUTURE SCALE
   ----------------------------------------------------------------
   Add indexes now while table is small.
   This prevents downtime later.
================================================================ */

CREATE INDEX IF NOT EXISTS idx_complaints_municipality 
ON complaints (municipality_id);

CREATE INDEX IF NOT EXISTS idx_complaints_state 
ON complaints (state_id);

CREATE INDEX IF NOT EXISTS idx_complaints_duplicate 
ON complaints (duplicate_of_complaint_id);


/* ================================================================
   END OF MIGRATION
================================================================ */