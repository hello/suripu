CREATE TABLE device_sensors (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT,
    ambient_temp FLOAT,
    ambient_light FLOAT,
    ambient_humidity FLOAT,
    ambient_air_quality FLOAT,
    ts TIMESTAMP
);

CREATE UNIQUE INDEX uniq_device_ts on device_sensors(device_id, ts);

GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;
GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;

CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;