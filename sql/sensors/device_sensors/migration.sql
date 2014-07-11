
-- TO MIGRATE DUPLICATES WRITES FROM OLD TABLE TO NEW ONE WITH A TRIGGER + FUNCTION

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
CREATE TRIGGER device_sensors_duplicate_insert_trigger
  BEFORE INSERT
  ON device_sensors
  FOR EACH ROW
  EXECUTE PROCEDURE device_sensors_insert_function();



-- MIGRATE OLD DATA
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




INSERT INTO device_sensors_master (account_id,
		device_id, ambient_temp, ambient_light, ambient_humidity,
		ambient_air_quality, ts, local_utc_ts, offset_millis)
SELECT device_sensors.account_id, device_sensors.device_id, device_sensors.ambient_temp,
	device_sensors.ambient_light, device_sensors.ambient_humidity, device_sensors.ambient_air_quality,
	device_sensors.ts,
	to_timestamp(extract(epoch from device_sensors.ts) + device_sensors.offset_millis::float / 1000),
	device_sensors.offset_millis FROM
		device_sensors;


