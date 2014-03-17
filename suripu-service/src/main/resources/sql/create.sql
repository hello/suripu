CREATE TABLE sensor_samples (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT,
    sensor_id INT,
    ts TIMESTAMP,
    val INT
);

CREATE UNIQUE INDEX uniq_sample on sensor_samples(device_id, ts);

GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;
GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;

CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;