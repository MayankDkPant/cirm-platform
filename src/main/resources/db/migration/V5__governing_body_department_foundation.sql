CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE state (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    code VARCHAR(10) NOT NULL,
    name VARCHAR(150) NOT NULL,
    country_code VARCHAR(10) NOT NULL DEFAULT 'IN',

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),

    CONSTRAINT uq_state_code UNIQUE (code, country_code),
    CONSTRAINT chk_state_code_uppercase CHECK (code = UPPER(code))
);

CREATE TABLE governing_body (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    state_id UUID NOT NULL,
    governance_sub_type_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),

    CONSTRAINT fk_gb_state
        FOREIGN KEY (state_id)
        REFERENCES state(id),

    CONSTRAINT fk_gb_sub_type
        FOREIGN KEY (governance_sub_type_id)
        REFERENCES governance_sub_type(id),

    CONSTRAINT uq_governing_body_code UNIQUE (state_id, code)
);

CREATE TABLE department (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    governing_body_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),

    CONSTRAINT fk_department_gb
        FOREIGN KEY (governing_body_id)
        REFERENCES governing_body(id),

    CONSTRAINT uq_department_code UNIQUE (governing_body_id, code)
);

CREATE TABLE department_unit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    department_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),

    CONSTRAINT fk_dept_unit_department
        FOREIGN KEY (department_id)
        REFERENCES department(id),

    CONSTRAINT uq_department_unit_code UNIQUE (department_id, code)
);
