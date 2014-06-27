--DROP TABLE IF EXISTS device_sensors;
--DROP TABLE IF EXISTS accounts;
--DROP TABLE IF EXISTS oauth_applications;
--DROP TABLE IF EXISTS oauth_tokens;
--DROP TABLE IF EXISTS account_device_map;



CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;

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

-- creating a 3 element index because account_id <-> ts is not sufficient if we want to support multiple devices
-- in the future
CREATE UNIQUE INDEX uniq_device_id_account_id_ts on device_sensors(device_id, account_id, ts);

GRANT ALL PRIVILEGES ON device_sensors TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE device_sensors_id_seq TO ingress_user;

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

GRANT ALL PRIVILEGES ON accounts TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE accounts_id_seq TO ingress_user;


CREATE TABLE oauth_applications (
    id SERIAL PRIMARY KEY,
    name VARCHAR (100),
    client_id VARCHAR (100),
    client_secret VARCHAR (100),
    redirect_uri VARCHAR (255),
    scopes int[],
    dev_account_id BIGINT,
    description VARCHAR(255),
    published boolean,
    created TIMESTAMP default current_timestamp,
    grant_type INTEGER,
    internal_only BOOLEAN default false
);

CREATE UNIQUE INDEX uniq_client_id on oauth_applications(client_id);
CREATE INDEX dev_account_id_idx on oauth_applications(dev_account_id);

-- alter table oauth_applications add column internal_only BOOLEAN DEFAULT FALSE;
-- alter table oauth_applications add column grant_type INTEGER DEFAULT 2;


GRANT ALL PRIVILEGES ON oauth_applications TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE oauth_applications_id_seq TO ingress_user;


CREATE TABLE oauth_tokens(
    id BIGSERIAL PRIMARY KEY,
    access_token UUID,
    refresh_token UUID,
    expires_in INTEGER,
    created_at TIMESTAMP,
    app_id INTEGER,
    account_id INTEGER,
    scopes int[]
);


CREATE UNIQUE INDEX uniq_access_token on oauth_tokens(access_token);
CREATE UNIQUE INDEX uniq_refresh_token on oauth_tokens(refresh_token);


GRANT ALL PRIVILEGES ON oauth_tokens TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE oauth_tokens_id_seq TO ingress_user;

CREATE TABLE account_device_map(
    id SERIAL PRIMARY KEY,
    account_id INTEGER,
    device_id VARCHAR(100),
    created_at TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_account_device on account_device_map(account_id, device_id);

GRANT ALL PRIVILEGES ON account_device_map TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_device_map_id_seq TO ingress_user;


CREATE TABLE device_sound(
    id BIGSERIAL PRIMARY KEY,
    device_id INTEGER,
    amplitude INTEGER,
    ts TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_device_ts_sound on device_sound(device_id, ts);

GRANT ALL PRIVILEGES ON device_sound TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE device_sound_id_seq TO ingress_user;


CREATE TABLE device_scores(
    id BIGSERIAL PRIMARY KEY,
    device_id INTEGER,
    ambient_temp INTEGER,
    ambient_air_quality INTEGER,
    ambient_humidity INTEGER,
    ambient_light INTEGER,
    ts TIMESTAMP,
    offset_millis INTEGER
);

CREATE TABLE account_scores(
    id BIGSERIAL PRIMARY KEY,
    account_id INTEGER,
    ambient_temp INTEGER,
    ambient_humidity INTEGER,
    ambient_air_quality INTEGER,
    ambient_light INTEGER,
    ts TIMESTAMP,
    offset_millis INTEGER
);


CREATE UNIQUE INDEX uniq_account_id_ts_account_scores on account_scores(account_id, ts);

GRANT ALL PRIVILEGES ON account_scores TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_scores_id_seq TO ingress_user;

CREATE TABLE sleep_label(
    id SERIAL PRIMARY KEY,
    account_id INTEGER,
    date_utc TIMESTAMP,
    rating INTEGER,
    sleep_at_utc TIMESTAMP,
    wakeup_at_utc TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_account_target_date on sleep_label(account_id, date_utc);

GRANT ALL PRIVILEGES ON sleep_label TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_label_id_seq TO ingress_user;

-- Assume for now we only support one tracker each account for sleep cycle tracking
CREATE TABLE motion(
    id BIGSERIAL PRIMARY KEY,
    account_id INTEGER,
    tracker_id VARCHAR(64),
    svm_no_gravity INTEGER,
    ts TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_tracker_id_ts ON motion(account_id, ts);
GRANT ALL PRIVILEGES ON motion TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE motion_id_seq TO ingress_user;

CREATE TABLE event(
    id BIGSERIAL PRIMARY KEY,
    event_type INTEGER, --{MOTION, NOISE, SNORING}
    account_id INTEGER,
    start_time_utc TIMESTAMP,
    end_time_utc TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_event_account_type_starttime ON event(account_id, event_type, start_time_utc);
GRANT ALL PRIVILEGES ON event TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE event_seq TO ingress_user;


-- 2014/05/08
-- NEW CHANGES
ALTER TABLE device_sensors ADD COLUMN account_id INTEGER;
-- to populate the account_id execute the following query:
-- UPDATE device_sensors SET account_id = account_device_map.account_id
--    FROM account_device_map
--    WHERE device_sensors.device_id = account_device_map.id;


-- 2014/06/26
-- Add Local timestamp that set to UTC time
ALTER TABLE device_sensors ADD COLUMN local_utc_ts TIMESTAMP;
ALTER TABLE device_sensors ALTER COLUMN device_id TYPE VARCHAR(255) USING device_id::BIGINT::TEXT::VARCHAR(255);

-- Fill the new column using following query.
-- UPDATE device_sensors SET offset_millis = -25200000 WHERE offset_millis = 0;  -- Assume all the existing data are in PST
-- UPDATE device_sensors SET local_utc_ts = to_timestamp(extract(epoch from device_sensors.ts) + device_sensors.offset_millis::float / 1000);

-- 2014/06/26
-- Alter the device_sensors table to master table, drop all indexes on device_sensors table
DROP INDEX uniq_device_ts;
DROP INDEX uniq_device_id_account_id_ts;

-- 2014/06/26
-- Create default partition
CREATE TABLE device_sensors_par_default() INHERITS (device_sensors);
CREATE UNIQUE INDEX uniq_device_id_account_id_ts on device_sensors_par_default(device_id, account_id, ts);

-- 2014/06/26
-- Add trigger to master table

-- The default trigger function
-- This function use dynamic table name and exception fallback, which is extremely expensive.
-- please replace it with the actual partition function generate by gen_par_script command.
CREATE OR REPLACE FUNCTION device_sensors_insert_function() RETURNS TRIGGER AS
$BODY$
DECLARE
    table_name TEXT;
BEGIN
    table_name := 'device_sensors_par_' || to_char(NEW.local_utc_ts, 'YYYY_MM');

    EXECUTE format('INSERT INTO %I VALUES ($1.*)', table_name) USING NEW;

    RETURN NULL;
EXCEPTION WHEN UNDEFINED_TABLE THEN  -- 42P01 	UNDEFINED TABLE: http://www.postgresql.org/docs/8.0/static/errcodes-appendix.html
    INSERT INTO device_sensors_par_default VALUES (NEW.*);
    RETURN NULL;
END
$BODY$
LANGUAGE plpgsql;

-- Create trigger which calls the trigger function
CREATE TRIGGER devcei_sensors_insert_trigger
  BEFORE INSERT
  ON device_sensors
  FOR EACH ROW
  EXECUTE PROCEDURE device_sensors_insert_function();





CREATE TABLE device_sensors_batch (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    ambient_temp int[],
    ambient_light int[],
    ambient_humidity int[],
    ambient_air_quality int[],
    ts TIMESTAMP,
    offset_millis INTEGER
);
