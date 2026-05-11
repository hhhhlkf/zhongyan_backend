CREATE TABLE mission (
    mission_id VARCHAR(80) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    scenario_type VARCHAR(80) NOT NULL,
    region JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    description TEXT,
    CONSTRAINT ck_mission_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED', 'FAILED'))
);

CREATE INDEX idx_mission_status_created_at ON mission (status, created_at DESC);
CREATE INDEX idx_mission_scenario_type ON mission (scenario_type);
CREATE INDEX idx_mission_region_gin ON mission USING GIN (region);

CREATE TABLE task (
    task_id VARCHAR(80) PRIMARY KEY,
    mission_id VARCHAR(80) NOT NULL REFERENCES mission (mission_id) ON DELETE RESTRICT,
    task_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    device_id VARCHAR(120),
    model_id VARCHAR(120),
    config_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    input_asset_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    output_asset_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    progress NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    error_code VARCHAR(120),
    error_message TEXT,
    CONSTRAINT ck_task_status CHECK (status IN (
        'DRAFT', 'WAITING_APPROVAL', 'QUEUED', 'DISPATCHING', 'RUNNING',
        'WAITING_ASSET', 'POST_PROCESSING', 'COMPLETED', 'FAILED',
        'RETRYING', 'CANCELLED', 'TIMEOUT'
    )),
    CONSTRAINT ck_task_progress CHECK (progress >= 0 AND progress <= 100)
);

CREATE INDEX idx_task_mission_created_at ON task (mission_id, created_at DESC);
CREATE INDEX idx_task_status_priority ON task (status, priority DESC, created_at);
CREATE INDEX idx_task_device_id ON task (device_id);
CREATE INDEX idx_task_model_id ON task (model_id);
CREATE INDEX idx_task_config_snapshot_gin ON task USING GIN (config_snapshot);

CREATE TABLE task_attempt (
    attempt_id VARCHAR(80) PRIMARY KEY,
    task_id VARCHAR(80) NOT NULL REFERENCES task (task_id) ON DELETE CASCADE,
    attempt_no INTEGER NOT NULL,
    executor_node VARCHAR(160),
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    result VARCHAR(32) NOT NULL,
    error_code VARCHAR(120),
    error_message TEXT,
    raw_log_object_key VARCHAR(500),
    CONSTRAINT uq_task_attempt_no UNIQUE (task_id, attempt_no),
    CONSTRAINT ck_task_attempt_no CHECK (attempt_no > 0),
    CONSTRAINT ck_task_attempt_result CHECK (result IN ('RUNNING', 'SUCCESS', 'FAILED', 'TIMEOUT', 'CANCELLED'))
);

CREATE INDEX idx_task_attempt_task_no ON task_attempt (task_id, attempt_no);
CREATE INDEX idx_task_attempt_result ON task_attempt (result);

CREATE TABLE task_command (
    command_id VARCHAR(80) PRIMARY KEY,
    task_id VARCHAR(80) NOT NULL REFERENCES task (task_id) ON DELETE CASCADE,
    mission_id VARCHAR(80) NOT NULL REFERENCES mission (mission_id) ON DELETE RESTRICT,
    device_id VARCHAR(120),
    command_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    idempotency_key VARCHAR(160) NOT NULL,
    requested_by VARCHAR(120) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by VARCHAR(120),
    approved_at TIMESTAMPTZ,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    dispatched_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    reason TEXT,
    CONSTRAINT uq_task_command_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_task_command_risk CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_task_command_status CHECK (status IN (
        'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'PENDING_DISPATCH',
        'DISPATCHED', 'COMPLETED', 'FAILED', 'CANCELLED'
    ))
);

CREATE INDEX idx_task_command_task_created_at ON task_command (task_id, created_at DESC);
CREATE INDEX idx_task_command_mission_created_at ON task_command (mission_id, created_at DESC);
CREATE INDEX idx_task_command_status ON task_command (status);
CREATE INDEX idx_task_command_payload_gin ON task_command USING GIN (payload);

CREATE TABLE task_event (
    event_id VARCHAR(80) PRIMARY KEY,
    task_id VARCHAR(80) NOT NULL REFERENCES task (task_id) ON DELETE CASCADE,
    event_type VARCHAR(64) NOT NULL,
    status_before VARCHAR(32),
    status_after VARCHAR(32),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_task_event_task_created_at ON task_event (task_id, created_at);
CREATE INDEX idx_task_event_type_created_at ON task_event (event_type, created_at DESC);
CREATE INDEX idx_task_event_payload_gin ON task_event USING GIN (payload);

CREATE TABLE task_dependency (
    task_id VARCHAR(80) NOT NULL REFERENCES task (task_id) ON DELETE CASCADE,
    depends_on_task_id VARCHAR(80) NOT NULL REFERENCES task (task_id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (task_id, depends_on_task_id),
    CONSTRAINT ck_task_dependency_not_self CHECK (task_id <> depends_on_task_id)
);

CREATE TABLE task_resource_lease (
    lease_id VARCHAR(80) PRIMARY KEY,
    task_id VARCHAR(80) NOT NULL REFERENCES task (task_id) ON DELETE CASCADE,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(160) NOT NULL,
    owner_node VARCHAR(160),
    leased_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    released_at TIMESTAMPTZ
);

CREATE INDEX idx_task_resource_lease_task ON task_resource_lease (task_id);
CREATE INDEX idx_task_resource_lease_resource ON task_resource_lease (resource_type, resource_id);
CREATE UNIQUE INDEX uq_active_resource_lease ON task_resource_lease (resource_type, resource_id) WHERE released_at IS NULL;

CREATE TABLE asset (
    asset_id VARCHAR(80) PRIMARY KEY,
    mission_id VARCHAR(80) NOT NULL REFERENCES mission (mission_id) ON DELETE RESTRICT,
    task_id VARCHAR(80) REFERENCES task (task_id) ON DELETE SET NULL,
    asset_type VARCHAR(40) NOT NULL,
    asset_role VARCHAR(40) NOT NULL,
    status VARCHAR(32) NOT NULL,
    geo_status VARCHAR(32) NOT NULL,
    name VARCHAR(240) NOT NULL,
    object_key VARCHAR(500),
    content_type VARCHAR(120),
    size_bytes BIGINT NOT NULL DEFAULT 0,
    checksum VARCHAR(160),
    preview_object_key VARCHAR(500),
    layer_url VARCHAR(800),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_asset_size CHECK (size_bytes >= 0),
    CONSTRAINT ck_asset_type CHECK (asset_type IN ('IMAGE', 'VIDEO', 'MODEL_RESULT', 'ATTEMPT_LOG', 'REPORT', 'ATTACHMENT', 'GEOMETRY', 'LAYER')),
    CONSTRAINT ck_asset_role CHECK (asset_role IN ('RAW', 'INPUT', 'OUTPUT', 'PREVIEW', 'LOG', 'REPORT', 'ATTACHMENT')),
    CONSTRAINT ck_asset_status CHECK (status IN ('CREATED', 'UPLOADING', 'AVAILABLE', 'FAILED', 'DELETED')),
    CONSTRAINT ck_asset_geo_status CHECK (geo_status IN ('NOT_REQUIRED', 'PENDING', 'CALCULATED', 'FAILED'))
);

CREATE INDEX idx_asset_mission_created_at ON asset (mission_id, created_at DESC);
CREATE INDEX idx_asset_task_created_at ON asset (task_id, created_at DESC);
CREATE INDEX idx_asset_type_status ON asset (asset_type, status);
CREATE INDEX idx_asset_geo_status ON asset (geo_status);
CREATE INDEX idx_asset_metadata_gin ON asset USING GIN (metadata);

CREATE TABLE task_asset (
    task_id VARCHAR(80) NOT NULL REFERENCES task (task_id) ON DELETE CASCADE,
    asset_id VARCHAR(80) NOT NULL REFERENCES asset (asset_id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    bound_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (task_id, asset_id, role),
    CONSTRAINT ck_task_asset_role CHECK (role IN ('RAW', 'INPUT', 'OUTPUT', 'PREVIEW', 'LOG', 'REPORT', 'ATTACHMENT'))
);

CREATE INDEX idx_task_asset_asset ON task_asset (asset_id);
