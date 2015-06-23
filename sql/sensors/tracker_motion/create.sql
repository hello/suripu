

-- Keeping track of trackers for each users
-- Normally should only be one / account
-- but might want to support multiple for debug/admin purposes
CREATE TABLE account_tracker_map(
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    device_id VARCHAR(100),
    created_at TIMESTAMP default current_timestamp
);

-- CREATE UNIQUE INDEX uniq_account_tracker on account_tracker_map(account_id, device_id);
CREATE UNIQUE INDEX uniq_account_tracker_active on account_tracker_map(account_id, device_id, active);
CREATE INDEX pill_id_active ON account_tracker_map(device_id, active);


GRANT ALL PRIVILEGES ON account_tracker_map TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_tracker_map_id_seq TO ingress_user;

ALTER TABLE account_tracker_map ADD COLUMN active BOOLEAN DEFAULT TRUE;
ALTER TABLE account_tracker_map ADD COLUMN last_updated TIMESTAMP default current_timestamp;

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


CREATE TABLE pill_status (
    id BIGSERIAL PRIMARY KEY,
    pill_id BIGINT,
    firmware_version VARCHAR(10),
    battery_level INTEGER,
    last_updated TIMESTAMP
);

GRANT ALL PRIVILEGES ON pill_status TO ingress_user;
GRANT ALL PRIVILEGES ON pill_status_id_seq TO ingress_user;

ALTER TABLE pill_status ADD COLUMN uptime BIGINT;
ALTER TABLE pill_status ADD COLUMN fw_version INTEGER;


CREATE TABLE pill_battery_monitor (
    id BIGSERIAL PRIMARY KEY,
    internal_pill_id BIGINT,
    pill_id VARCHAR(255),
    last_update TIMESTAMP,
    pill_status INTEGER,
    max_d2d_drop INTEGER
);
CREATE INDEX pill_battery_monitor_id_status on pill_battery_monitor(internal_pill_id, last_update);
CREATE UNIQUE INDEX pill_battery_monitor_id_unique on pill_battery_monitor(internal_pill_id);