--
-- PostgreSQL database dump
--

\restrict 75ELKxi6u8nRwOjBsA0L6Z25JxiWTTwsv4y7UL91qD4E1UI9KFEdF9oMChgtDUH

-- Dumped from database version 15.15 (Debian 15.15-1.pgdg13+1)
-- Dumped by pg_dump version 15.15 (Debian 15.15-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


--
-- Name: enforce_complaint_event_immutable(); Type: FUNCTION; Schema: public; Owner: cirm_user
--

CREATE FUNCTION public.enforce_complaint_event_immutable() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'complaint_event is immutable: UPDATE is not allowed';
    ELSIF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'complaint_event is immutable: DELETE is not allowed';
    END IF;

    RETURN OLD;
END;
$$;


ALTER FUNCTION public.enforce_complaint_event_immutable() OWNER TO cirm_user;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: ai_conversation; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.ai_conversation (
    id uuid NOT NULL,
    conversation_type character varying(30) NOT NULL,
    user_id uuid NOT NULL,
    governing_body_id uuid NOT NULL,
    status character varying(20) NOT NULL,
    started_at timestamp without time zone NOT NULL,
    last_message_at timestamp without time zone NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


ALTER TABLE public.ai_conversation OWNER TO cirm_user;

--
-- Name: TABLE ai_conversation; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON TABLE public.ai_conversation IS 'Stores AI chat sessions for citizens and employees. Multi-tenant and audit compliant.';


--
-- Name: COLUMN ai_conversation.conversation_type; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_conversation.conversation_type IS 'Type of user initiating the chat: CITIZEN or EMPLOYEE';


--
-- Name: COLUMN ai_conversation.user_id; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_conversation.user_id IS 'Reference to platform user initiating the conversation';


--
-- Name: COLUMN ai_conversation.governing_body_id; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_conversation.governing_body_id IS 'Tenant isolation identifier. Ensures conversations are scoped per municipality';


--
-- Name: COLUMN ai_conversation.status; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_conversation.status IS 'Conversation lifecycle state: ACTIVE or CLOSED';


--
-- Name: COLUMN ai_conversation.started_at; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_conversation.started_at IS 'Timestamp when the conversation session started';


--
-- Name: COLUMN ai_conversation.last_message_at; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_conversation.last_message_at IS 'Timestamp of the most recent message for analytics and cleanup jobs';


--
-- Name: ai_message; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.ai_message (
    id uuid NOT NULL,
    conversation_id uuid NOT NULL,
    role character varying(20) NOT NULL,
    content text NOT NULL,
    model character varying(50),
    prompt_tokens integer,
    completion_tokens integer,
    total_tokens integer,
    response_time_ms integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    is_active boolean DEFAULT true NOT NULL
);


ALTER TABLE public.ai_message OWNER TO cirm_user;

--
-- Name: TABLE ai_message; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON TABLE public.ai_message IS 'Stores full AI chat history including user prompts and AI responses.';


--
-- Name: COLUMN ai_message.role; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_message.role IS 'Origin of the message: USER, ASSISTANT, or SYSTEM';


--
-- Name: COLUMN ai_message.model; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_message.model IS 'AI model used to generate the response (e.g., llama3). Used for analytics and billing';


--
-- Name: COLUMN ai_message.prompt_tokens; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_message.prompt_tokens IS 'Number of tokens in the input prompt sent to the model';


--
-- Name: COLUMN ai_message.completion_tokens; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_message.completion_tokens IS 'Number of tokens generated by the model';


--
-- Name: COLUMN ai_message.total_tokens; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_message.total_tokens IS 'Total tokens consumed for the request (prompt + completion)';


--
-- Name: COLUMN ai_message.response_time_ms; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ai_message.response_time_ms IS 'Time taken by AI service to generate the response in milliseconds';


--
-- Name: ai_model; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.ai_model (
    id uuid NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    model_type character varying(50) NOT NULL,
    hosting_type character varying(50) NOT NULL,
    version character varying(50),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone,
    created_by character varying(255),
    updated_at timestamp with time zone,
    updated_by character varying(255)
);


ALTER TABLE public.ai_model OWNER TO cirm_user;

--
-- Name: auth_otp; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.auth_otp (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    otp_reference character varying(50) NOT NULL,
    mobile_number character varying(20) NOT NULL,
    otp_hash character varying(255) NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    verified_at timestamp without time zone,
    attempt_count integer DEFAULT 0 NOT NULL,
    max_attempts integer DEFAULT 5 NOT NULL,
    ip_address character varying(45),
    user_agent text,
    request_id character varying(100),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.auth_otp OWNER TO cirm_user;

--
-- Name: complaint_event; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.complaint_event (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    complaint_id uuid NOT NULL,
    event_type character varying(100) NOT NULL,
    old_value text,
    new_value text,
    triggered_by character varying(100) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    is_active boolean DEFAULT true,
    governing_body_id uuid NOT NULL,
    triggered_by_user_id uuid
);


ALTER TABLE public.complaint_event OWNER TO cirm_user;

--
-- Name: complaint_reference_seq; Type: SEQUENCE; Schema: public; Owner: cirm_user
--

CREATE SEQUENCE public.complaint_reference_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.complaint_reference_seq OWNER TO cirm_user;

--
-- Name: SEQUENCE complaint_reference_seq; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON SEQUENCE public.complaint_reference_seq IS 'Global sequence for complaint reference numbers.';


--
-- Name: complaints; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.complaints (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    title character varying(255) NOT NULL,
    description character varying(255),
    department character varying(255),
    priority character varying(255),
    status character varying(255) NOT NULL,
    latitude double precision,
    longitude double precision,
    external_system character varying(100),
    external_reference character varying(150),
    external_sync_status character varying(50) DEFAULT 'PENDING'::character varying NOT NULL,
    resolved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(100),
    updated_at timestamp with time zone,
    updated_by character varying(100),
    created_by_user_id uuid,
    created_via character varying(30),
    ai_conversation_id uuid,
    is_active boolean DEFAULT true,
    municipality_id uuid,
    state_id uuid,
    address_text text,
    ai_suggested_department character varying(255),
    ai_suggested_priority character varying(50),
    ai_suggested_category character varying(255),
    original_complaint_id uuid,
    is_duplicate boolean DEFAULT false NOT NULL,
    reported_location_text text,
    municipality_name character varying(255),
    ward_id uuid,
    ward_name character varying(255),
    formatted_address character varying(500),
    governing_body_id uuid NOT NULL,
    external_sync_attempts integer DEFAULT 0 NOT NULL,
    external_last_sync_at timestamp without time zone
);


ALTER TABLE public.complaints OWNER TO cirm_user;

--
-- Name: department; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.department (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    governing_body_id uuid NOT NULL,
    "Code__c" character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(100) DEFAULT 'SYSTEM'::character varying NOT NULL,
    updated_at timestamp with time zone,
    updated_by character varying(100)
);


ALTER TABLE public.department OWNER TO cirm_user;

--
-- Name: department_unit; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.department_unit (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    department_id uuid NOT NULL,
    "Code__c" character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(100) DEFAULT 'SYSTEM'::character varying NOT NULL,
    updated_at timestamp with time zone,
    updated_by character varying(100),
    state_id uuid,
    district_id uuid,
    governing_body_id uuid
);


ALTER TABLE public.department_unit OWNER TO cirm_user;

--
-- Name: district; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.district (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    state_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    "Code__c" character varying(50)
);


ALTER TABLE public.district OWNER TO cirm_user;

--
-- Name: TABLE district; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON TABLE public.district IS 'Administrative district. Parent of all local governing bodies.';


--
-- Name: COLUMN district.state_id; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.district.state_id IS 'State to which the district belongs.';


--
-- Name: external_integration_config; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.external_integration_config (
    id uuid NOT NULL,
    governing_scope character varying(30) NOT NULL,
    governing_uuid uuid,
    entity_type character varying(100) NOT NULL,
    event_type character varying(50) NOT NULL,
    external_system_id uuid NOT NULL,
    execution_order integer DEFAULT 1 NOT NULL,
    is_mandatory boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone,
    created_by character varying(255),
    updated_at timestamp with time zone,
    updated_by character varying(255)
);


ALTER TABLE public.external_integration_config OWNER TO cirm_user;

--
-- Name: external_reference; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.external_reference (
    id uuid NOT NULL,
    external_system_id uuid NOT NULL,
    state_uuid uuid,
    entity_type character varying(100) NOT NULL,
    entity_uuid uuid NOT NULL,
    external_id character varying(255) NOT NULL,
    master_system character varying(20),
    last_synced_at timestamp with time zone,
    created_at timestamp with time zone,
    created_by character varying(255),
    governing_body_id uuid
);


ALTER TABLE public.external_reference OWNER TO cirm_user;

--
-- Name: external_system; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.external_system (
    id uuid NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    scope character varying(20) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone,
    created_by character varying(255),
    updated_at timestamp with time zone,
    updated_by character varying(255)
);


ALTER TABLE public.external_system OWNER TO cirm_user;

--
-- Name: external_system_state; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.external_system_state (
    id uuid NOT NULL,
    external_system_id uuid NOT NULL,
    state_uuid uuid NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone,
    created_by character varying(255),
    governing_body_id uuid
);


ALTER TABLE public.external_system_state OWNER TO cirm_user;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO cirm_user;

--
-- Name: governance_sub_type; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.governance_sub_type (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    governance_type_id uuid NOT NULL,
    "Code__c" character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    country_code character varying(10) DEFAULT 'IN'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone,
    updated_by character varying(100),
    CONSTRAINT chk_gst_code_uppercase CHECK ((("Code__c")::text = upper(("Code__c")::text)))
);


ALTER TABLE public.governance_sub_type OWNER TO cirm_user;

--
-- Name: governance_type; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.governance_type (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "Code__c" character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    description text,
    country_code character varying(10) DEFAULT 'IN'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone,
    updated_by character varying(100),
    CONSTRAINT chk_governance_type_code_uppercase CHECK ((("Code__c")::text = upper(("Code__c")::text)))
);


ALTER TABLE public.governance_type OWNER TO cirm_user;

--
-- Name: governing_body; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.governing_body (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    state_id uuid NOT NULL,
    governance_sub_type_id uuid NOT NULL,
    "Code__c" character varying(50) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(100) DEFAULT 'SYSTEM'::character varying NOT NULL,
    updated_at timestamp with time zone,
    updated_by character varying(100),
    district_id uuid
);


ALTER TABLE public.governing_body OWNER TO cirm_user;

--
-- Name: COLUMN governing_body.district_id; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.governing_body.district_id IS 'District in which the governing body operates.';


--
-- Name: idempotency_request; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.idempotency_request (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    idempotency_key character varying(255) NOT NULL,
    request_path character varying(255) NOT NULL,
    response_status integer NOT NULL,
    response_body text NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    governing_body_id uuid,
    request_hash character varying(128),
    response_payload text
);


ALTER TABLE public.idempotency_request OWNER TO cirm_user;

--
-- Name: TABLE idempotency_request; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON TABLE public.idempotency_request IS 'Stores idempotent API responses to prevent duplicate complaint creation.';


--
-- Name: COLUMN idempotency_request.expires_at; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.idempotency_request.expires_at IS 'Record validity window (typically 24 hours).';


--
-- Name: identity_type; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.identity_type (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    code character varying(50) NOT NULL,
    description character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.identity_type OWNER TO cirm_user;

--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.refresh_tokens (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    token_hash character varying(500) NOT NULL,
    user_id uuid NOT NULL,
    expiry_date timestamp with time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    is_active boolean DEFAULT true,
    device_info character varying(255),
    ip_address character varying(50),
    token_id uuid NOT NULL
);


ALTER TABLE public.refresh_tokens OWNER TO cirm_user;

--
-- Name: state; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.state (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    "Code__c" character varying(10) NOT NULL,
    name character varying(150) NOT NULL,
    country_code character varying(10) DEFAULT 'IN'::character varying NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(100) DEFAULT 'SYSTEM'::character varying NOT NULL,
    updated_at timestamp with time zone,
    updated_by character varying(100),
    CONSTRAINT chk_state_code_uppercase CHECK ((("Code__c")::text = upper(("Code__c")::text)))
);


ALTER TABLE public.state OWNER TO cirm_user;

--
-- Name: user_identity; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.user_identity (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    user_id uuid NOT NULL,
    identity_type_id uuid NOT NULL,
    identity_value character varying(255) NOT NULL,
    is_verified boolean DEFAULT false,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone
);


ALTER TABLE public.user_identity OWNER TO cirm_user;

--
-- Name: user_profile; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.user_profile (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    full_name character varying(255),
    phone_number character varying(50),
    state_uuid uuid,
    municipality_uuid uuid,
    ward_uuid uuid,
    zone_uuid uuid,
    address_line1 character varying(255),
    address_line2 character varying(255),
    postal_code character varying(20),
    created_at timestamp with time zone,
    created_by character varying(255),
    updated_at timestamp with time zone,
    updated_by character varying(255),
    is_phone_verified boolean DEFAULT false,
    is_email_verified boolean DEFAULT false
);


ALTER TABLE public.user_profile OWNER TO cirm_user;

--
-- Name: users; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.users (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    email character varying(255) NOT NULL,
    role character varying(50) NOT NULL,
    enabled boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone,
    is_active boolean DEFAULT true,
    last_login_at timestamp without time zone,
    failed_login_attempts integer DEFAULT 0,
    account_locked_until timestamp without time zone,
    CONSTRAINT chk_users_email_lowercase CHECK (((email)::text = lower((email)::text)))
);


ALTER TABLE public.users OWNER TO cirm_user;

--
-- Name: ward; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.ward (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    governing_body_id uuid NOT NULL,
    zone_id uuid,
    name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    "Code__c" character varying(50) NOT NULL
);


ALTER TABLE public.ward OWNER TO cirm_user;

--
-- Name: TABLE ward; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON TABLE public.ward IS 'Electoral and administrative ward. Universal unit for rural and urban areas.';


--
-- Name: COLUMN ward.zone_id; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON COLUMN public.ward.zone_id IS 'Nullable. Present only when wards belong to an urban zone.';


--
-- Name: zone; Type: TABLE; Schema: public; Owner: cirm_user
--

CREATE TABLE public.zone (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    governing_body_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    "Code__c" character varying(50)
);


ALTER TABLE public.zone OWNER TO cirm_user;

--
-- Name: TABLE zone; Type: COMMENT; Schema: public; Owner: cirm_user
--

COMMENT ON TABLE public.zone IS 'Administrative grouping of wards (primarily urban).';


--
-- Name: ai_conversation ai_conversation_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ai_conversation
    ADD CONSTRAINT ai_conversation_pkey PRIMARY KEY (id);


--
-- Name: ai_message ai_message_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ai_message
    ADD CONSTRAINT ai_message_pkey PRIMARY KEY (id);


--
-- Name: ai_model ai_model_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ai_model
    ADD CONSTRAINT ai_model_pkey PRIMARY KEY (id);


--
-- Name: auth_otp auth_otp_otp_reference_key; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.auth_otp
    ADD CONSTRAINT auth_otp_otp_reference_key UNIQUE (otp_reference);


--
-- Name: auth_otp auth_otp_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.auth_otp
    ADD CONSTRAINT auth_otp_pkey PRIMARY KEY (id);


--
-- Name: complaint_event complaint_event_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaint_event
    ADD CONSTRAINT complaint_event_pkey PRIMARY KEY (id);


--
-- Name: complaints complaints_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT complaints_pkey PRIMARY KEY (id);


--
-- Name: department department_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT department_pkey PRIMARY KEY (id);


--
-- Name: department_unit department_unit_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department_unit
    ADD CONSTRAINT department_unit_pkey PRIMARY KEY (id);


--
-- Name: district district_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.district
    ADD CONSTRAINT district_pkey PRIMARY KEY (id);


--
-- Name: external_integration_config external_integration_config_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_integration_config
    ADD CONSTRAINT external_integration_config_pkey PRIMARY KEY (id);


--
-- Name: external_reference external_reference_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_reference
    ADD CONSTRAINT external_reference_pkey PRIMARY KEY (id);


--
-- Name: external_system external_system_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_system
    ADD CONSTRAINT external_system_pkey PRIMARY KEY (id);


--
-- Name: external_system_state external_system_state_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_system_state
    ADD CONSTRAINT external_system_state_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: governance_sub_type governance_sub_type_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governance_sub_type
    ADD CONSTRAINT governance_sub_type_pkey PRIMARY KEY (id);


--
-- Name: governance_type governance_type_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governance_type
    ADD CONSTRAINT governance_type_pkey PRIMARY KEY (id);


--
-- Name: governing_body governing_body_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governing_body
    ADD CONSTRAINT governing_body_pkey PRIMARY KEY (id);


--
-- Name: idempotency_request idempotency_request_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.idempotency_request
    ADD CONSTRAINT idempotency_request_pkey PRIMARY KEY (id);


--
-- Name: identity_type identity_type_code_key; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.identity_type
    ADD CONSTRAINT identity_type_code_key UNIQUE (code);


--
-- Name: identity_type identity_type_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.identity_type
    ADD CONSTRAINT identity_type_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: state state_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.state
    ADD CONSTRAINT state_pkey PRIMARY KEY (id);


--
-- Name: department uk_department_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT uk_department_code UNIQUE ("Code__c");


--
-- Name: department_unit uk_department_unit_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department_unit
    ADD CONSTRAINT uk_department_unit_code UNIQUE ("Code__c");


--
-- Name: governance_sub_type uk_governance_sub_type_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governance_sub_type
    ADD CONSTRAINT uk_governance_sub_type_code UNIQUE ("Code__c");


--
-- Name: governance_type uk_governance_type_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governance_type
    ADD CONSTRAINT uk_governance_type_code UNIQUE ("Code__c");


--
-- Name: governing_body uk_governing_body_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governing_body
    ADD CONSTRAINT uk_governing_body_code UNIQUE ("Code__c");


--
-- Name: state uk_state_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.state
    ADD CONSTRAINT uk_state_code UNIQUE ("Code__c");


--
-- Name: department uq_department_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT uq_department_code UNIQUE (governing_body_id, "Code__c");


--
-- Name: department_unit uq_department_unit_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department_unit
    ADD CONSTRAINT uq_department_unit_code UNIQUE (department_id, "Code__c");


--
-- Name: district uq_district_state_name; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.district
    ADD CONSTRAINT uq_district_state_name UNIQUE (state_id, name);


--
-- Name: governance_type uq_governance_type_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governance_type
    ADD CONSTRAINT uq_governance_type_code UNIQUE ("Code__c", country_code);


--
-- Name: governing_body uq_governing_body_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governing_body
    ADD CONSTRAINT uq_governing_body_code UNIQUE (state_id, "Code__c");


--
-- Name: governance_sub_type uq_gst_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governance_sub_type
    ADD CONSTRAINT uq_gst_code UNIQUE (governance_type_id, "Code__c", country_code);


--
-- Name: idempotency_request uq_idempotency_request_tenant_key; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.idempotency_request
    ADD CONSTRAINT uq_idempotency_request_tenant_key UNIQUE (governing_body_id, idempotency_key);


--
-- Name: user_identity uq_identity; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.user_identity
    ADD CONSTRAINT uq_identity UNIQUE (identity_type_id, identity_value);


--
-- Name: state uq_state_code; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.state
    ADD CONSTRAINT uq_state_code UNIQUE ("Code__c", country_code);


--
-- Name: users uq_users_email; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);


--
-- Name: ward uq_ward_body_name; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ward
    ADD CONSTRAINT uq_ward_body_name UNIQUE (governing_body_id, name);


--
-- Name: zone uq_zone_body_name; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.zone
    ADD CONSTRAINT uq_zone_body_name UNIQUE (governing_body_id, name);


--
-- Name: user_identity user_identity_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.user_identity
    ADD CONSTRAINT user_identity_pkey PRIMARY KEY (id);


--
-- Name: user_profile user_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT user_profile_pkey PRIMARY KEY (id);


--
-- Name: user_profile user_profile_user_id_key; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT user_profile_user_id_key UNIQUE (user_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: ward ward_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ward
    ADD CONSTRAINT ward_pkey PRIMARY KEY (id);


--
-- Name: zone zone_pkey; Type: CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.zone
    ADD CONSTRAINT zone_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_ai_conv_last_message; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_ai_conv_last_message ON public.ai_conversation USING btree (last_message_at);


--
-- Name: idx_ai_conv_municipality; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_ai_conv_municipality ON public.ai_conversation USING btree (governing_body_id);


--
-- Name: idx_ai_conv_user; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_ai_conv_user ON public.ai_conversation USING btree (user_id);


--
-- Name: idx_ai_conversation_governing_body; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_ai_conversation_governing_body ON public.ai_conversation USING btree (governing_body_id);


--
-- Name: idx_ai_msg_conversation_created; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_ai_msg_conversation_created ON public.ai_message USING btree (conversation_id, created_at DESC);


--
-- Name: idx_auth_otp_expires_at; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_auth_otp_expires_at ON public.auth_otp USING btree (expires_at);


--
-- Name: idx_auth_otp_mobile_created; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_auth_otp_mobile_created ON public.auth_otp USING btree (mobile_number, created_at);


--
-- Name: idx_auth_otp_mobile_reference; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_auth_otp_mobile_reference ON public.auth_otp USING btree (mobile_number, otp_reference);


--
-- Name: idx_complaint_ai_conversation; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaint_ai_conversation ON public.complaints USING btree (ai_conversation_id);


--
-- Name: idx_complaint_created_by; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaint_created_by ON public.complaints USING btree (created_by_user_id);


--
-- Name: idx_complaint_department; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaint_department ON public.complaints USING btree (department);


--
-- Name: idx_complaint_event_governing_body; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaint_event_governing_body ON public.complaint_event USING btree (governing_body_id);


--
-- Name: idx_complaint_status; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaint_status ON public.complaints USING btree (status);


--
-- Name: idx_complaints_gb_created; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaints_gb_created ON public.complaints USING btree (governing_body_id, created_at);


--
-- Name: idx_complaints_gb_status; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaints_gb_status ON public.complaints USING btree (governing_body_id, status);


--
-- Name: idx_complaints_gb_ward; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaints_gb_ward ON public.complaints USING btree (governing_body_id, ward_id);


--
-- Name: idx_complaints_is_duplicate; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaints_is_duplicate ON public.complaints USING btree (is_duplicate);


--
-- Name: idx_complaints_municipality; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaints_municipality ON public.complaints USING btree (municipality_id);


--
-- Name: idx_complaints_original; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaints_original ON public.complaints USING btree (original_complaint_id);


--
-- Name: idx_complaints_state; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_complaints_state ON public.complaints USING btree (state_id);


--
-- Name: idx_du_district; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_du_district ON public.department_unit USING btree (district_id);


--
-- Name: idx_du_governing_body; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_du_governing_body ON public.department_unit USING btree (governing_body_id);


--
-- Name: idx_du_state; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_du_state ON public.department_unit USING btree (state_id);


--
-- Name: idx_event_complaint; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_event_complaint ON public.complaint_event USING btree (complaint_id);


--
-- Name: idx_event_triggered_user; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_event_triggered_user ON public.complaint_event USING btree (triggered_by_user_id);


--
-- Name: idx_ext_integration_lookup; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_ext_integration_lookup ON public.external_integration_config USING btree (governing_scope, governing_uuid, entity_type, event_type, is_active);


--
-- Name: idx_external_reference_governing_body; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_external_reference_governing_body ON public.external_reference USING btree (governing_body_id);


--
-- Name: idx_external_system_state_governing_body; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_external_system_state_governing_body ON public.external_system_state USING btree (governing_body_id);


--
-- Name: idx_governance_type_active; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_governance_type_active ON public.governance_type USING btree (is_active) WHERE (is_deleted = false);


--
-- Name: idx_gst_type_lookup; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_gst_type_lookup ON public.governance_sub_type USING btree (governance_type_id) WHERE (is_deleted = false);


--
-- Name: idx_idempotency_expires_at; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_idempotency_expires_at ON public.idempotency_request USING btree (expires_at);


--
-- Name: idx_idempotency_request_tenant_expires; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_idempotency_request_tenant_expires ON public.idempotency_request USING btree (governing_body_id, expires_at);


--
-- Name: idx_refresh_expiry; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_refresh_expiry ON public.refresh_tokens USING btree (expiry_date);


--
-- Name: idx_refresh_tokens_token_id; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE UNIQUE INDEX idx_refresh_tokens_token_id ON public.refresh_tokens USING btree (token_id);


--
-- Name: idx_refresh_user; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_refresh_user ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_user_identity_user; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_user_identity_user ON public.user_identity USING btree (user_id);


--
-- Name: idx_user_identity_value; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_user_identity_value ON public.user_identity USING btree (identity_value);


--
-- Name: idx_user_profile_user; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_user_profile_user ON public.user_profile USING btree (user_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: uq_ai_model_code; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE UNIQUE INDEX uq_ai_model_code ON public.ai_model USING btree (code);


--
-- Name: uq_ext_system_state; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE UNIQUE INDEX uq_ext_system_state ON public.external_system_state USING btree (external_system_id, state_uuid);


--
-- Name: uq_external_integration_rule; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE UNIQUE INDEX uq_external_integration_rule ON public.external_integration_config USING btree (governing_scope, governing_uuid, entity_type, event_type, external_system_id);


--
-- Name: uq_external_reference_unique; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE UNIQUE INDEX uq_external_reference_unique ON public.external_reference USING btree (external_system_id, state_uuid, entity_type, external_id);


--
-- Name: uq_external_system_code; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE UNIQUE INDEX uq_external_system_code ON public.external_system USING btree (code);


--
-- Name: ux_ward_code; Type: INDEX; Schema: public; Owner: cirm_user
--

CREATE UNIQUE INDEX ux_ward_code ON public.ward USING btree ("Code__c");


--
-- Name: complaint_event trg_complaint_event_immutable; Type: TRIGGER; Schema: public; Owner: cirm_user
--

CREATE TRIGGER trg_complaint_event_immutable BEFORE DELETE OR UPDATE ON public.complaint_event FOR EACH ROW EXECUTE FUNCTION public.enforce_complaint_event_immutable();


--
-- Name: district district_state_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.district
    ADD CONSTRAINT district_state_id_fkey FOREIGN KEY (state_id) REFERENCES public.state(id);


--
-- Name: ai_conversation fk_ai_conversation_governing_body; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ai_conversation
    ADD CONSTRAINT fk_ai_conversation_governing_body FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id) ON DELETE RESTRICT;


--
-- Name: ai_conversation fk_ai_conversation_user; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ai_conversation
    ADD CONSTRAINT fk_ai_conversation_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: ai_message fk_ai_message_conversation; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ai_message
    ADD CONSTRAINT fk_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES public.ai_conversation(id);


--
-- Name: complaints fk_complaint_ai_conversation; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT fk_complaint_ai_conversation FOREIGN KEY (ai_conversation_id) REFERENCES public.ai_conversation(id);


--
-- Name: complaints fk_complaint_created_by; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT fk_complaint_created_by FOREIGN KEY (created_by_user_id) REFERENCES public.users(id);


--
-- Name: complaint_event fk_complaint_event_governing_body; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaint_event
    ADD CONSTRAINT fk_complaint_event_governing_body FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id) ON DELETE RESTRICT;


--
-- Name: complaints fk_complaints_governing_body; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaints
    ADD CONSTRAINT fk_complaints_governing_body FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id) ON DELETE RESTRICT;


--
-- Name: department fk_department_gb; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT fk_department_gb FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id);


--
-- Name: department_unit fk_dept_unit_department; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department_unit
    ADD CONSTRAINT fk_dept_unit_department FOREIGN KEY (department_id) REFERENCES public.department(id);


--
-- Name: department_unit fk_du_district; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department_unit
    ADD CONSTRAINT fk_du_district FOREIGN KEY (district_id) REFERENCES public.district(id);


--
-- Name: department_unit fk_du_governing_body; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department_unit
    ADD CONSTRAINT fk_du_governing_body FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id);


--
-- Name: department_unit fk_du_state; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.department_unit
    ADD CONSTRAINT fk_du_state FOREIGN KEY (state_id) REFERENCES public.state(id);


--
-- Name: complaint_event fk_event_complaint; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaint_event
    ADD CONSTRAINT fk_event_complaint FOREIGN KEY (complaint_id) REFERENCES public.complaints(id) ON DELETE CASCADE;


--
-- Name: complaint_event fk_event_user; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.complaint_event
    ADD CONSTRAINT fk_event_user FOREIGN KEY (triggered_by_user_id) REFERENCES public.users(id);


--
-- Name: external_integration_config fk_ext_integration_system; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_integration_config
    ADD CONSTRAINT fk_ext_integration_system FOREIGN KEY (external_system_id) REFERENCES public.external_system(id) ON DELETE CASCADE;


--
-- Name: external_system_state fk_ext_state_system; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_system_state
    ADD CONSTRAINT fk_ext_state_system FOREIGN KEY (external_system_id) REFERENCES public.external_system(id) ON DELETE CASCADE;


--
-- Name: external_reference fk_external_reference_governing_body; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_reference
    ADD CONSTRAINT fk_external_reference_governing_body FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id) ON DELETE RESTRICT;


--
-- Name: external_reference fk_external_reference_system; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_reference
    ADD CONSTRAINT fk_external_reference_system FOREIGN KEY (external_system_id) REFERENCES public.external_system(id) ON DELETE CASCADE;


--
-- Name: external_system_state fk_external_system_state_governing_body; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.external_system_state
    ADD CONSTRAINT fk_external_system_state_governing_body FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id) ON DELETE RESTRICT;


--
-- Name: governing_body fk_gb_state; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governing_body
    ADD CONSTRAINT fk_gb_state FOREIGN KEY (state_id) REFERENCES public.state(id);


--
-- Name: governing_body fk_gb_sub_type; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governing_body
    ADD CONSTRAINT fk_gb_sub_type FOREIGN KEY (governance_sub_type_id) REFERENCES public.governance_sub_type(id);


--
-- Name: governing_body fk_governing_body_district; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governing_body
    ADD CONSTRAINT fk_governing_body_district FOREIGN KEY (district_id) REFERENCES public.district(id);


--
-- Name: governance_sub_type fk_gst_governance_type; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.governance_sub_type
    ADD CONSTRAINT fk_gst_governance_type FOREIGN KEY (governance_type_id) REFERENCES public.governance_type(id);


--
-- Name: idempotency_request fk_idempotency_request_governing_body; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.idempotency_request
    ADD CONSTRAINT fk_idempotency_request_governing_body FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id) ON DELETE RESTRICT;


--
-- Name: user_identity fk_identity_type; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.user_identity
    ADD CONSTRAINT fk_identity_type FOREIGN KEY (identity_type_id) REFERENCES public.identity_type(id);


--
-- Name: user_identity fk_identity_user; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.user_identity
    ADD CONSTRAINT fk_identity_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: refresh_tokens fk_refresh_user; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_profile fk_user_profile_user; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.user_profile
    ADD CONSTRAINT fk_user_profile_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: ward ward_governing_body_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ward
    ADD CONSTRAINT ward_governing_body_id_fkey FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id);


--
-- Name: ward ward_zone_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.ward
    ADD CONSTRAINT ward_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES public.zone(id);


--
-- Name: zone zone_governing_body_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cirm_user
--

ALTER TABLE ONLY public.zone
    ADD CONSTRAINT zone_governing_body_id_fkey FOREIGN KEY (governing_body_id) REFERENCES public.governing_body(id);


--
-- PostgreSQL database dump complete
--

\unrestrict 75ELKxi6u8nRwOjBsA0L6Z25JxiWTTwsv4y7UL91qD4E1UI9KFEdF9oMChgtDUH

