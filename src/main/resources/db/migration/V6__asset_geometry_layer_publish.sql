CREATE TABLE IF NOT EXISTS asset_geometry (
    asset_id VARCHAR(80) PRIMARY KEY REFERENCES asset (asset_id) ON DELETE CASCADE,
    mission_id VARCHAR(80) NOT NULL REFERENCES mission (mission_id) ON DELETE CASCADE,
    task_id VARCHAR(80) REFERENCES task (task_id) ON DELETE SET NULL,
    geom geometry(Polygon, 4326) NOT NULL,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_asset_geometry_mission ON asset_geometry (mission_id);
CREATE INDEX IF NOT EXISTS idx_asset_geometry_task ON asset_geometry (task_id);
CREATE INDEX IF NOT EXISTS idx_asset_geometry_geom ON asset_geometry USING GIST (geom);
CREATE INDEX IF NOT EXISTS idx_asset_geometry_properties_gin ON asset_geometry USING GIN (properties);

COMMENT ON TABLE asset_geometry IS 'PostGIS geometry records used by GeoServer layer publishing.';
COMMENT ON COLUMN asset_geometry.geom IS 'Asset boundary geometry in EPSG:4326.';
