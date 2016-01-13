-- 1. Create the new table and its new indices

CREATE TABLE tracker_motion_par_2014_11() INHERITS (tracker_motion_master);
GRANT ALL PRIVILEGES ON tracker_motion_par_2014_11 TO ingress_user;

CREATE UNIQUE INDEX uniq_tracker_ts_on_par_2014_11 on tracker_motion_par_2014_11(tracker_id, ts);
CREATE UNIQUE INDEX uniq_tracker_id_account_id_ts_on_par_2014_11 on tracker_motion_par_2014_11(tracker_id, account_id, ts);



ALTER TABLE tracker_motion_par_2014_11 ADD CHECK (local_utc_ts >= '2014-11-01 00:00:00' AND local_utc_ts < '2014-12-01 00:00:00');
-- 2. Update the trigger function

-- I don't know if this has to be re-created for every function update

--CREATE TRIGGER tracker_motion_master_insert_trigger
--  BEFORE INSERT
--  ON tracker_motion_master
--  FOR EACH ROW
--  EXECUTE PROCEDURE tracker_motion_master_insert_function();

-- Trigger function for master insert
CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
	table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2014-11-01 00:00:00' AND NEW.local_utc_ts < '2014-12-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2014_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-10-01 00:00:00' AND NEW.local_utc_ts < '2014-11-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2014_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-09-01 00:00:00' AND NEW.local_utc_ts < '2014-10-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2014_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-07-01 00:00:00' AND NEW.local_utc_ts < '2014-08-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2014_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2014-08-01 00:00:00' AND NEW.local_utc_ts < '2014-09-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2014_08 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;

-- 3. Add to your calendar the day you need to update this script. Usually 15 days before you run out of tables.