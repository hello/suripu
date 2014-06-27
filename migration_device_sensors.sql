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

CREATE TABLE device_sensors_par_default() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX uniq_device_ts_on_par_default on device_sensors_par_default(device_id, ts);
CREATE UNIQUE INDEX uniq_device_id_account_id_ts_on_par_default on device_sensors_par_default(device_id, account_id, ts);

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
CREATE TRIGGER devcei_sensors_master_insert_trigger
  BEFORE INSERT
  ON device_sensors_master
  FOR EACH ROW
  EXECUTE PROCEDURE device_sensors_master_insert_function();


-- Trigger function for device_sensors insert
CREATE OR REPLACE FUNCTION device_sensors_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
BEGIN
    INSERT INTO device_sensors_master (account_id, device_id, ambient_temp, ambient_light, ambient_humidity, ambient_air_quality, ts,
    	local_utc_ts, offset_millis) VALUES
        (NEW.account_id, NEW.device_id, NEW.ambient_temp, NEW.ambient_light, NEW.ambient_humidity, NEW.ambient_air_quality, NEW.ts,
        	to_timestamp(extract(epoch from NEW.ts) + NEW.offset_millis::float / 1000),
        	NEW.offset_millis);
    RETURN NEW;
END
$BODY$;

-- Create trigger which calls the trigger function
CREATE TRIGGER devcei_sensors_duplicate_insert_trigger
  BEFORE INSERT
  ON device_sensors
  FOR EACH ROW
  EXECUTE PROCEDURE device_sensors_insert_function();


INSERT INTO device_sensors (account_id, device_id, ambient_temp, ambient_light, ambient_humidity, ambient_air_quality, ts,
    	offset_millis) VALUES
        (-1, -1, 0, 0, 0, 0, current_timestamp AT TIME ZONE 'UTC', -25200000);

INSERT INTO device_sensors_master (account_id,
		device_id, ambient_temp, ambient_light, ambient_humidity,
		ambient_air_quality, ts, local_utc_ts, offset_millis)
SELECT device_sensors.account_id, device_sensors.device_id, device_sensors.ambient_temp,
	device_sensors.ambient_light, device_sensors.ambient_humidity, device_sensors.ambient_air_quality,
	device_sensors.ts,
	to_timestamp(extract(epoch from device_sensors.ts) + device_sensors.offset_millis::float / 1000),
	device_sensors.offset_millis FROM
		device_sensors WHERE device_sensors.ts < (SELECT device_sensors_par_default.ts FROM
										device_sensors_par_default ORDER BY device_sensors_par_default.ts ASC LIMIT 1);



