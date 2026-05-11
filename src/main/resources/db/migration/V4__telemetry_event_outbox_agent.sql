CREATE TABLE uav_telemetry (
    telemetry_id VARCHAR(120) PRIMARY KEY,
    uav_id VARCHAR(120) NOT NULL,
    mission_id VARCHAR(80) REFERENCES mission (mission_id) ON DELETE SET NULL,
    task_id VARCHAR(80) REFERENCES task (task_id) ON DELETE SET NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    altitude_meters DOUBLE PRECISION,
    heading_degrees DOUBLE PRECISION,
    speed_mps DOUBLE PRECISION,
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    geom GEOMETRY(Point, 4326)
);

CREATE INDEX idx_uav_telemetry_uav_recorded_at ON uav_telemetry (uav_id, recorded_at DESC);
CREATE INDEX idx_uav_telemetry_mission_recorded_at ON uav_telemetry (mission_id, recorded_at DESC);
CREATE INDEX idx_uav_telemetry_geom ON uav_telemetry USING GIST (geom);
CREATE INDEX idx_uav_telemetry_raw_payload_gin ON uav_telemetry USING GIN (raw_payload);

CREATE TABLE event_outbox (
    event_id VARCHAR(120) PRIMARY KEY,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    topic VARCHAR(120) NOT NULL,
    message_key VARCHAR(160) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    CONSTRAINT ck_event_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_event_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_event_outbox_status_created_at ON event_outbox (status, created_at);
CREATE INDEX idx_event_outbox_aggregate ON event_outbox (aggregate_type, aggregate_id, created_at DESC);
CREATE INDEX idx_event_outbox_payload_gin ON event_outbox USING GIN (payload);

CREATE TABLE agent_session (
    session_id VARCHAR(120) PRIMARY KEY,
    mission_id VARCHAR(80) REFERENCES mission (mission_id) ON DELETE SET NULL,
    task_id VARCHAR(80) REFERENCES task (task_id) ON DELETE SET NULL,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_agent_session_mission_created_at ON agent_session (mission_id, created_at DESC);
CREATE INDEX idx_agent_session_task_created_at ON agent_session (task_id, created_at DESC);

CREATE TABLE agent_message (
    message_id VARCHAR(120) PRIMARY KEY,
    session_id VARCHAR(120) NOT NULL REFERENCES agent_session (session_id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    created_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_agent_message_session_created_at ON agent_message (session_id, created_at);

CREATE TABLE agent_tool_call (
    tool_call_id VARCHAR(120) PRIMARY KEY,
    session_id VARCHAR(120) REFERENCES agent_session (session_id) ON DELETE SET NULL,
    message_id VARCHAR(120) REFERENCES agent_message (message_id) ON DELETE SET NULL,
    tool_name VARCHAR(160) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    status VARCHAR(40) NOT NULL,
    arguments JSONB NOT NULL DEFAULT '{}'::jsonb,
    result JSONB NOT NULL DEFAULT '{}'::jsonb,
    requested_by VARCHAR(120) NOT NULL,
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    CONSTRAINT ck_agent_tool_call_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_agent_tool_call_session_created_at ON agent_tool_call (session_id, created_at DESC);
CREATE INDEX idx_agent_tool_call_status ON agent_tool_call (status, created_at DESC);
CREATE INDEX idx_agent_tool_call_arguments_gin ON agent_tool_call USING GIN (arguments);
