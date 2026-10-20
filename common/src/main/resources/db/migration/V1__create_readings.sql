CREATE TABLE readings (
    id          BIGSERIAL        PRIMARY KEY,
    sensor_id   VARCHAR(64)      NOT NULL,
    metric      VARCHAR(64)      NOT NULL,
    value       DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uq_reading UNIQUE (sensor_id, metric, recorded_at)
);

CREATE INDEX idx_readings_metric_time ON readings (metric, recorded_at);