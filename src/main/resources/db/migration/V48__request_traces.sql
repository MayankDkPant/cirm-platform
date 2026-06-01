CREATE TABLE IF NOT EXISTS request_traces (
    id UUID PRIMARY KEY,
    correlation_id VARCHAR(100) NOT NULL,
    request_state VARCHAR(30) NOT NULL,
    request_timestamp TIMESTAMPTZ NOT NULL,
    endpoint VARCHAR(500),
    http_method VARCHAR(20),
    user_id UUID,
    ward_id UUID,
    governing_body_id UUID,
    jwt_subject VARCHAR(255),
    request_payload TEXT,
    response_status INTEGER,
    error_message TEXT,
    execution_time_ms BIGINT,
    source_platform VARCHAR(50),
    app_version VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_request_traces_correlation_id
    ON request_traces (correlation_id);

CREATE INDEX IF NOT EXISTS idx_request_traces_timestamp
    ON request_traces (request_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_request_traces_state_timestamp
    ON request_traces (request_state, request_timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_request_traces_user_timestamp
    ON request_traces (user_id, request_timestamp DESC)
    WHERE user_id IS NOT NULL;
