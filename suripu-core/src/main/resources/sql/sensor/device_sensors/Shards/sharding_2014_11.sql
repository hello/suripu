-- NOVEMBER

CREATE TABLE device_sensors_par_2014_11() INHERITS (device_sensors_master);
GRANT ALL PRIVILEGES ON device_sensors_par_2014_11 TO ingress_user;

CREATE UNIQUE INDEX uniq_device_ts_on_par_2014_11 on device_sensors_par_2014_11(device_id, ts);
CREATE UNIQUE INDEX uniq_device_id_account_id_ts_on_par_2014_11 on device_sensors_par_2014_11(device_id, account_id, ts);



ALTER TABLE device_sensors_par_2014_11 ADD CHECK (local_utc_ts >= '2014-11-01 00:00:00' AND local_utc_ts < '2014-12-01 00:00:00');


-- Trigger function for master insert
CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
	table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2014-11-01 00:00:00' AND NEW.local_utc_ts < '2014-12-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2014_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-10-01 00:00:00' AND NEW.local_utc_ts < '2014-11-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2014_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-09-01 00:00:00' AND NEW.local_utc_ts < '2014-10-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2014_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-08-01 00:00:00' AND NEW.local_utc_ts < '2014-09-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2014_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-07-01 00:00:00' AND NEW.local_utc_ts < '2014-08-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2014_07 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;