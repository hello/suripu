CREATE TABLE pill_status (
    id BIGSERIAL PRIMARY KEY,
    pill_id BIGINT,
    firmware_version VARCHAR(10),
    battery_level INTEGER,
    last_updated TIMESTAMP
);

GRANT ALL PRIVILEGES ON pill_status TO ingress_user;
GRANT ALL PRIVILEGES ON pill_status_id_seq TO ingress_user;

ALTER TABLE pill_status ADD COLUMN uptime BIGINT;
ALTER TABLE pill_status ADD COLUMN fw_version INTEGER;
