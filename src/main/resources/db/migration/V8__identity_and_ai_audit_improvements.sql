-- =====================================================
-- IDENTITY + AI AUDIT HARDENING MIGRATION
-- =====================================================
-- This migration strengthens audit, identity and AI traceability.
--
-- Goals:
-- 1) Introduce user_profile (business profile separate from auth)
-- 2) Link complaints to the user who created them
-- 3) Enforce FK between AI conversations and users
-- 4) Track whether complaints were created via AI or manually
-- 5) Link complaints to AI conversations for model training
-- 6) Introduce AI model registry
-- =====================================================



-- =====================================================
-- USER PROFILE (RESIDENT / EMPLOYEE BUSINESS DATA)
-- =====================================================

CREATE TABLE user_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,

    full_name VARCHAR(255),
    phone_number VARCHAR(50),

    -- Governance linkage (used for permissions & auto-location)
    state_uuid UUID,
    municipality_uuid UUID,
    ward_uuid UUID,
    zone_uuid UUID,

    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    postal_code VARCHAR(20),

    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(255),

    CONSTRAINT fk_user_profile_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_user_profile_user
ON user_profile(user_id);



-- =====================================================
-- COMPLAINT → USER RELATIONSHIP
-- =====================================================

ALTER TABLE complaints
ADD COLUMN created_by_user_id UUID;

ALTER TABLE complaints
ADD CONSTRAINT fk_complaint_created_by
FOREIGN KEY (created_by_user_id)
REFERENCES users(id);



-- =====================================================
-- COMPLAINT → AI CREATION TRACKING
-- =====================================================

ALTER TABLE complaints
ADD COLUMN created_via VARCHAR(30);
-- MANUAL_FORM | AI_ASSISTANT | IMPORT | API



-- =====================================================
-- COMPLAINT → AI CONVERSATION LINK
-- =====================================================

ALTER TABLE complaints
ADD COLUMN ai_conversation_id UUID;

ALTER TABLE complaints
ADD CONSTRAINT fk_complaint_ai_conversation
FOREIGN KEY (ai_conversation_id)
REFERENCES ai_conversation(id);



-- =====================================================
-- ENFORCE FK: AI CONVERSATION → USER
-- =====================================================

ALTER TABLE ai_conversation
ADD CONSTRAINT fk_ai_conversation_user
FOREIGN KEY (user_id)
REFERENCES users(id);



-- =====================================================
-- HELPFUL INDEXES
-- =====================================================

CREATE INDEX idx_complaint_created_by
ON complaints(created_by_user_id);

CREATE INDEX idx_complaint_ai_conversation
ON complaints(ai_conversation_id);



-- =====================================================
-- AI MODEL REGISTRY
-- =====================================================
-- Central registry of AI/ML models supported by the platform.
-- Enables multiple LLMs, vision models, embeddings, etc.
-- =====================================================

CREATE TABLE ai_model (
    id UUID PRIMARY KEY,

    -- Unique model code used in application
    -- Examples: LLAMA3_CHAT, LLAMA3_CLASSIFIER, CLIP_VISION
    code VARCHAR(100) NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Model capability
    -- LLM_CHAT | CLASSIFICATION | EMBEDDING | VISION | SPEECH
    model_type VARCHAR(50) NOT NULL,

    -- Where the model is hosted
    -- LOCAL | AWS | AZURE | OPENAI
    hosting_type VARCHAR(50) NOT NULL,

    version VARCHAR(50),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX uq_ai_model_code
ON ai_model(code);
