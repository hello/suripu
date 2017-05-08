

--
-- MASTER TABLE
--
CREATE TABLE tracker_motion_master(
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    tracker_id BIGINT,
    svm_no_gravity INTEGER,
    ts TIMESTAMP,
    offset_millis INTEGER,
    local_utc_ts TIMESTAMP
);

GRANT ALL PRIVILEGES ON tracker_motion_master TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE tracker_motion_master_id_seq TO ingress_user;

CREATE UNIQUE INDEX uniq_account_tracker_ts on tracker_motion_master(account_id, tracker_id, ts);


ALTER TABLE tracker_motion_master ADD COLUMN motion_range BIGINT;
ALTER TABLE tracker_motion_master ADD COLUMN kickoff_counts INTEGER;
ALTER TABLE tracker_motion_master ADD COLUMN on_duration_seconds INTEGER;

--
-- ALWAYS CREATE A DEFAULT TABLE
--
CREATE TABLE tracker_motion_par_default() INHERITS (tracker_motion_master);
GRANT ALL PRIVILEGES ON tracker_motion_par_default TO ingress_user;



-- Create trigger which calls the trigger function
CREATE TRIGGER tracker_motion_master_insert_trigger
  BEFORE INSERT
  ON tracker_motion_master
  FOR EACH ROW
  EXECUTE PROCEDURE tracker_motion_master_insert_function();


-- Trigger function for tracker_motion_master_insert_trigger
CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
	table_name text;
BEGIN
    INSERT INTO tracker_motion_par_default VALUES (NEW.*);

    RETURN NULL;
END
$BODY$;
