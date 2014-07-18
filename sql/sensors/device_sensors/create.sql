--DROP TABLE IF EXISTS device_sensors;
--DROP TABLE IF EXISTS accounts;
--DROP TABLE IF EXISTS oauth_applications;
--DROP TABLE IF EXISTS oauth_tokens;
--DROP TABLE IF EXISTS account_device_map;



CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;


CREATE TABLE account_device_map(
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    device_id VARCHAR(100),
    created_at TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_account_device on account_device_map(account_id, device_id);

GRANT ALL PRIVILEGES ON account_device_map TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_device_map_id_seq TO ingress_user;

CREATE TABLE device_sound(
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT,
    amplitude INTEGER,
    ts TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_device_ts_sound on device_sound(device_id, ts);

GRANT ALL PRIVILEGES ON device_sound TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE device_sound_id_seq TO ingress_user;


--
-- MASTER TABLE
--
CREATE TABLE device_sensors_master (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    device_id BIGINT,
    ambient_temp FLOAT,
    ambient_light FLOAT,
    ambient_humidity FLOAT,
    ambient_air_quality FLOAT,
    ts TIMESTAMP,
    local_utc_ts TIMESTAMP,
    offset_millis INTEGER
);

GRANT ALL PRIVILEGES ON device_sensors_master TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE device_sensors_master_id_seq TO ingress_user;

CREATE TABLE device_sensors_par_default() INHERITS (device_sensors_master);
GRANT ALL PRIVILEGES ON device_sensors_par_default TO ingress_user;

CREATE UNIQUE INDEX uniq_device_ts_on_par_default on device_sensors_par_default(device_id, ts);
CREATE UNIQUE INDEX uniq_device_id_account_id_ts_on_par_default on device_sensors_par_default(device_id, account_id, ts);


ALTER TABLE accounts ADD COLUMN last_modified TIMESTAMP;

-- Trigger function for master insert
CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
	table_name text;
BEGIN
	--table_name := 'device_sensors_par_' || to_char(NEW.local_utc_ts, 'YYYY_MM');

    --EXECUTE format('INSERT INTO %I VALUES ($1.*)', table_name) USING NEW;
    INSERT INTO device_sensors_par_default VALUES (NEW.*);

    RETURN NULL;
END
$BODY$;

-- Create trigger which calls the trigger function
CREATE TRIGGER device_sensors_master_insert_trigger
  BEFORE INSERT
  ON device_sensors_master
  FOR EACH ROW
  EXECUTE PROCEDURE device_sensors_master_insert_function();


