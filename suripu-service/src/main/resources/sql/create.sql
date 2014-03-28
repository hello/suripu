CREATE TABLE device_sensors (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT,
    ambient_temp FLOAT,
    ambient_light FLOAT,
    ambient_humidity FLOAT,
    ambient_air_quality FLOAT,
    ts TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_device_ts on device_sensors(device_id, ts);

GRANT ALL PRIVILEGES ON device_sensors TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE device_sensors_id_seq TO ingress_user;

CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;

CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    firstname VARCHAR (100),
    lastname VARCHAR (100),
    username VARCHAR (100),
    email VARCHAR (255),
    password_hash CHAR (60),
    created TIMESTAMP,
    height SMALLINT,
    weight SMALLINT,
    age SMALLINT,
    tz VARCHAR (100)
);

CREATE UNIQUE INDEX uniq_email on accounts(email);

--CREATE TABLE sensor_samples (
--    id BIGSERIAL PRIMARY KEY,
--    device_id BIGINT,
--    sensor_id INT,
--    ts TIMESTAMP,
--    val INT
--);
--
--CREATE UNIQUE INDEX uniq_sample on sensor_samples(device_id, ts);
--
--GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;
--GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;