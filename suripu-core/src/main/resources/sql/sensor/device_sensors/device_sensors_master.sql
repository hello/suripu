--DROP TABLE IF EXISTS device_sensors;
--DROP TABLE IF EXISTS accounts;
--DROP TABLE IF EXISTS oauth_applications;
--DROP TABLE IF EXISTS oauth_tokens;
--DROP TABLE IF EXISTS account_device_map;



CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;



--
-- MASTER TABLE
--
CREATE TABLE device_sensors_master (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    device_id BIGINT,
    ambient_temp INTEGER,
    ambient_light INTEGER,
    ambient_light_variance INTEGER,
    ambient_light_peakiness INTEGER,
    ambient_humidity INTEGER,
    ambient_air_quality INTEGER,
    ambient_air_quality_raw INTEGER, -- raw counts
    ambient_dust_variance INTEGER, -- raw counts
    ambient_dust_min INTEGER, -- raw counts
    ambient_dust_max INTEGER, -- raw counts
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

-- add new columns for additional light values (10/16/2014)
ALTER TABLE device_sensors_master ADD COLUMN ambient_light_variance INTEGER DEFAULT 0;
ALTER TABLE device_sensors_master ADD COLUMN ambient_light_peakiness INTEGER DEFAULT 0;

-- store int instead of float (10/16/2014)
ALTER TABLE device_sensors_master ALTER COLUMN ambient_temp SET DATA TYPE INTEGER;
ALTER TABLE device_sensors_master ALTER COLUMN ambient_light SET DATA TYPE INTEGER;
ALTER TABLE device_sensors_master ALTER COLUMN ambient_humidity SET DATA TYPE INTEGER;
ALTER TABLE device_sensors_master ALTER COLUMN ambient_air_quality SET DATA TYPE INTEGER;

-- additional dust stats (10/22/2014)
ALTER TABLE device_sensors_master ADD COLUMN ambient_air_quality_raw INTEGER DEFAULT 0; -- save raw counts
ALTER TABLE device_sensors_master ADD COLUMN ambient_dust_variance INTEGER DEFAULT 0;
ALTER TABLE device_sensors_master ADD COLUMN ambient_dust_min INTEGER DEFAULT 0;
ALTER TABLE device_sensors_master ADD COLUMN ambient_dust_max INTEGER DEFAULT 0;

-- additional firmware info (11/10/2014)
ALTER TABLE device_sensors_master ADD COLUMN firmware_version INTEGER DEFAULT 0;

-- additional guesture info (11/21/2014)
ALTER TABLE device_sensors_master ADD COLUMN wave_count INTEGER DEFAULT 0;
ALTER TABLE device_sensors_master ADD COLUMN hold_count INTEGER DEFAULT 0;

-- add sound data (01/21/2015)
ALTER TABLE device_sensors_master ADD COLUMN audio_num_disturbances INTEGER DEFAULT 0;
ALTER TABLE device_sensors_master ADD COLUMN audio_peak_disturbances_db INTEGER DEFAULT 0; -- converted to DB
ALTER TABLE device_sensors_master ADD COLUMN audio_peak_background_db INTEGER DEFAULT 0;

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


