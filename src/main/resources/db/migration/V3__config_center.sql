CREATE TABLE device_config (
    device_id VARCHAR(120) PRIMARY KEY,
    device_name VARCHAR(200) NOT NULL,
    device_type VARCHAR(80) NOT NULL,
    host VARCHAR(255),
    connection JSONB NOT NULL DEFAULT '{}'::jsonb,
    capabilities JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_device_config_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_device_config_type_status ON device_config (device_type, status);
CREATE INDEX idx_device_config_connection_gin ON device_config USING GIN (connection);
CREATE INDEX idx_device_config_capabilities_gin ON device_config USING GIN (capabilities);

CREATE TABLE camera_config (
    camera_config_id VARCHAR(120) NOT NULL,
    version INTEGER NOT NULL,
    camera_type VARCHAR(80) NOT NULL,
    fov JSONB NOT NULL DEFAULT '{}'::jsonb,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (camera_config_id, version),
    CONSTRAINT ck_camera_config_version CHECK (version > 0),
    CONSTRAINT ck_camera_config_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_camera_config_active ON camera_config (camera_config_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_camera_config_latest ON camera_config (camera_config_id, version DESC);
CREATE INDEX idx_camera_config_type_status ON camera_config (camera_type, status);
CREATE INDEX idx_camera_config_fov_gin ON camera_config USING GIN (fov);
CREATE INDEX idx_camera_config_parameters_gin ON camera_config USING GIN (parameters);

CREATE TABLE model_config (
    model_config_id VARCHAR(120) NOT NULL,
    version INTEGER NOT NULL,
    model_type VARCHAR(100) NOT NULL,
    runtime_type VARCHAR(80) NOT NULL,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (model_config_id, version),
    CONSTRAINT ck_model_config_version CHECK (version > 0),
    CONSTRAINT ck_model_config_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_model_config_active ON model_config (model_config_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_model_config_latest ON model_config (model_config_id, version DESC);
CREATE INDEX idx_model_config_type_status ON model_config (model_type, status);
CREATE INDEX idx_model_config_parameters_gin ON model_config USING GIN (parameters);

CREATE TABLE transfer_config (
    transfer_config_id VARCHAR(120) NOT NULL,
    version INTEGER NOT NULL,
    transfer_type VARCHAR(80) NOT NULL,
    endpoint JSONB NOT NULL DEFAULT '{}'::jsonb,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (transfer_config_id, version),
    CONSTRAINT ck_transfer_config_version CHECK (version > 0),
    CONSTRAINT ck_transfer_config_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED'))
);

CREATE UNIQUE INDEX uq_transfer_config_active ON transfer_config (transfer_config_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_transfer_config_latest ON transfer_config (transfer_config_id, version DESC);
CREATE INDEX idx_transfer_config_type_status ON transfer_config (transfer_type, status);
CREATE INDEX idx_transfer_config_endpoint_gin ON transfer_config USING GIN (endpoint);
CREATE INDEX idx_transfer_config_parameters_gin ON transfer_config USING GIN (parameters);

CREATE TABLE model_artifact (
    artifact_id VARCHAR(120) PRIMARY KEY,
    model_config_id VARCHAR(120) NOT NULL,
    artifact_type VARCHAR(80) NOT NULL,
    object_key VARCHAR(500),
    checksum VARCHAR(160),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_model_artifact_status CHECK (status IN ('DRAFT', 'ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_model_artifact_model_config ON model_artifact (model_config_id, created_at DESC);
CREATE INDEX idx_model_artifact_metadata_gin ON model_artifact USING GIN (metadata);

CREATE TABLE config_validation (
    validation_id VARCHAR(120) PRIMARY KEY,
    config_type VARCHAR(40) NOT NULL,
    config_id VARCHAR(120) NOT NULL,
    status VARCHAR(32) NOT NULL,
    errors JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_config_validation_type CHECK (config_type IN ('DEVICE', 'CAMERA', 'MODEL', 'TRANSFER')),
    CONSTRAINT ck_config_validation_status CHECK (status IN ('PASSED', 'FAILED'))
);

CREATE INDEX idx_config_validation_config ON config_validation (config_type, config_id, created_at DESC);
CREATE INDEX idx_config_validation_status ON config_validation (status, created_at DESC);
