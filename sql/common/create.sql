CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;

ALTER ROLE ingress_user REPLICATION;

--
-- ACCOUNTS
--
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



--
-- UPDATES TO ACCOUNT TABLE 2014-07-16
--

ALTER TABLE accounts ADD COLUMN name VARCHAR (255);
UPDATE accounts SET name = firstname || ' ' || lastname;
ALTER TABLE accounts ALTER COLUMN name set NOT NULL;
ALTER TABLE accounts ALTER COLUMN weight SET DATA TYPE INTEGER;



--
-- UPDATES TO ACCOUNT TABLE 2014-07-17
--

ALTER TABLE accounts ADD COLUMN tz_offset INTEGER;
ALTER TABLE accounts ADD COLUMN last_modified BIGINT;
-- ALTER TABLE accounts DROP COLUMN tz;
ALTER TABLE accounts ADD COLUMN dob TIMESTAMP;



ALTER TABLE accounts ADD COLUMN gender VARCHAR(50);

--
-- OAUTH
--
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

GRANT ALL PRIVILEGES ON oauth_applications TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE oauth_applications_id_seq TO ingress_user;


CREATE TABLE oauth_tokens(
    id BIGSERIAL PRIMARY KEY,
    access_token UUID,
    refresh_token UUID,
    expires_in INTEGER,
    created_at TIMESTAMP default current_timestamp,
    app_id INTEGER,
    account_id INTEGER,
    scopes int[]
);


CREATE UNIQUE INDEX uniq_access_token on oauth_tokens(access_token);
CREATE UNIQUE INDEX uniq_refresh_token on oauth_tokens(refresh_token);


GRANT ALL PRIVILEGES ON oauth_tokens TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE oauth_tokens_id_seq TO ingress_user;


--
-- SCORES
--

CREATE TABLE account_scores(
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
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


--
-- SLEEP
--
CREATE TABLE sleep_label(
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    date_utc TIMESTAMP,
    rating INTEGER,
    sleep_at_utc TIMESTAMP,
    wakeup_at_utc TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_account_target_date on sleep_label(account_id, date_utc);

GRANT ALL PRIVILEGES ON sleep_label TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_label_id_seq TO ingress_user;

CREATE TABLE sleep_score (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    device_id BIGINT,
    pill_id BIGINT, -- pill id
    date_bucket_utc TIMESTAMP, -- Y-m-d H:00 in UTC
    offset_millis INTEGER,
    sleep_duration INTEGER, -- no. of minutes of data used to compute scores
    custom BOOLEAN default false, -- true if the scores are user-customized
    bucket_score INTEGER, -- score for this time period, updated as we get more data
    agitation_num INTEGER, -- agitation score, number of times pill value > -1
    agitation_tot INTEGER, -- agitation score, area under curve
    updated TIMESTAMP default current_timestamp  -- server time when record was last updated
);

CREATE UNIQUE INDEX unique_account_id_date_hour_utc on sleep_score(account_id, date_bucket_utc);

GRANT ALL PRIVILEGES ON sleep_score TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_score_id_seq TO ingress_user;

--
-- UPDATE LAST_MODIFIED COLUMN
--
--CREATE OR REPLACE FUNCTION update_modified_column() RETURNS TRIGGER LANGUAGE plpgsql AS
--$BODY$
--BEGIN
--    NEW.last_modified = now();
--    RETURN NEW;
--END;
--$BODY$;
--
--
--CREATE TRIGGER update_modified_column_trigger
--BEFORE UPDATE
--ON accounts
--FOR EACH ROW
--EXECUTE PROCEDURE update_modified_column();


CREATE TABLE sleep_feedback(
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    day VARCHAR (100),
    hour VARCHAR (100),
    correct BOOLEAN
);

GRANT ALL PRIVILEGES ON sleep_feedback TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_feedback_id_seq TO ingress_user;

CREATE TABLE account_location (
    id SERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    ip_address INET,
    city varchar(100),
    country varchar(2), -- see http://en.wikipedia.org/wiki/ISO_3166-1
    created TIMESTAMP default current_timestamp
);

GRANT ALL PRIVILEGES ON account_location TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_location_id_seq TO ingress_user;

-- for data science-y stuff

CREATE TYPE USER_LABEL_TYPE AS ENUM ('make_bed', 'went_to_bed', 'fall_asleep',
    'awake', 'out_of_bed', 'awake_in_bed',
    'sound_disturbance', 'got_up_at_night', 'other_disturbance');


-- table to save user-annotation for timeline

CREATE TABLE user_labels (
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    email VARCHAR (255),
    label USER_LABEL_TYPE,
    night_date TIMESTAMP,
    utc_ts TIMESTAMP,
    local_utc_ts TIMESTAMP,
    tz_offset int,
    created TIMESTAMP default current_timestamp
);

GRANT ALL PRIVILEGES ON user_labels TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE user_labels_id_seq TO ingress_user;
CREATE INDEX user_labels_account_id_night ON user_labels(account_id, night_date);

ALTER TABLE user_labels ADD COLUMN duration INT DEFAULT 0;
ALTER TABLE user_labels ADD COLUMN note VARCHAR(255) DEFAULT '';


CREATE TABLE onboarding_logs (
    id SERIAL PRIMARY KEY,
    sense_id VARCHAR (24),
    utc_ts TIMESTAMP,
    pill_id VARCHAR (24),
    account_id BIGINT,
    info TEXT,
    result VARCHAR(255),
    operation VARCHAR(20),
    created_at TIMESTAMP default current_timestamp,
    ip VARCHAR(25)
);

GRANT ALL PRIVILEGES ON onboarding_logs TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE onboarding_logs_id_seq TO ingress_user;
CREATE UNIQUE INDEX unique_sense_id_time ON onboarding_logs(sense_id, account_id, utc_ts);
CREATE INDEX onboarding_logs_result_time ON onboarding_logs(result, utc_ts);
CREATE INDEX onboarding_logs_account_id_time ON onboarding_logs(account_id, utc_ts);


CREATE TABLE sense_colors (id SERIAL PRIMARY KEY, sense_id VARCHAR(64),color VARCHAR(64));
CREATE UNIQUE index sense_id_color on sense_colors(sense_id);

GRANT ALL PRIVILEGES ON sense_colors TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sense_colors_id_seq TO ingress_user;



CREATE TABLE tracking (id SERIAL PRIMARY KEY, sense_id VARCHAR(255), internal_sense_id BIGINT, account_id BIGINT, category SMALLINT, created_at TIMESTAMP);
CREATE UNIQUE index tracking_uniq_device_id_category on tracking(internal_sense_id, category);


GRANT ALL PRIVILEGES ON tracking TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE tracking_id_seq TO ingress_user;



CREATE TABLE store_feedback (id SERIAL PRIMARY KEY, account_id BIGINT NOT NULL, likes VARCHAR(255), review BOOLEAN, created_at TIMESTAMP);

GRANT ALL PRIVILEGES ON store_feedback TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE store_feedback_id_seq TO ingress_user;


-- TIMELINE ANALYTICS

CREATE TABLE timeline_analytics (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    date_of_night VARCHAR,
    algorithm INTEGER,
    error INTEGER,
    created_at TIMESTAMP
);

CREATE UNIQUE INDEX uniq_per_night ON timeline_analytics(account_id, date_of_night, algorithm, error);
CREATE INDEX date_of_night_idx ON timeline_analytics(date_of_night);

GRANT ALL PRIVILEGES ON timeline_analytics TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE timeline_analytics_id_seq TO ingress_user;

--
-- UPDATES TO timeline_analytics TABLE 2015-11-23
--
ALTER TABLE timeline_analytics ADD COLUMN test_group BIGINT NOT NULL DEFAULT 0;


-- User Location Data, added during onboarding for now. 2015-12-30

CREATE TABLE account_location(
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    ip INET,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    city VARCHAR(255),
    state VARCHAR(255),
    country_code CHAR(2), -- see https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
    created TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_account_location_created_idx ON account_location(account_id, created);

GRANT ALL PRIVILEGES ON account_location TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_location_id_seq TO ingress_user;

-- User test groups for timeline
CREATE TABLE user_timeline_test_group(
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    utc_ts TIMESTAMP,
    group_id BIGINT NOT NULL DEFAULT 0);

CREATE INDEX user_test_group_account_id_idx on user_timeline_test_group(account_id);

GRANT ALL PRIVILEGES ON user_timeline_test_group TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE user_timeline_test_group_id_seq TO ingress_user;

-- UPDATES TO timeline_feedback TABLE 2016-01-15
--on purpose, default value is null, we are going to go in an back-populate
ALTER TABLE timeline_feedback ADD COLUMN adjustment_delta_minutes INTEGER;


-- save unsubscribe email 2016-01-27
CREATE TABLE unsubscribe_email (
     id SERIAL PRIMARY KEY,
     account_id BIGINT default 0,
     email VARCHAR(255),
     created TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_unsubscribe_email on unsubscribe_email(email);

GRANT ALL PRIVILEGES ON unsubscribe_email TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE unsubscribe_email_id_seq TO ingress_user;


CREATE TABLE sleep_sound_durations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    duration_seconds BIGINT,
    sort_key INTEGER NOT NULL
);

INSERT INTO sleep_sound_durations (name, duration_seconds, sort_key)
VALUES ('10 Minutes', 600, 600),
       ('30 Minutes', 1800, 1800),
       ('1 Hour', 3600, 3600),
       ('2 Hours', 7200, 7200),
       ('3 Hours', 10800, 10800),
       ('Indefinitely', NULL, 100000);

CREATE TABLE sleep_sounds (
    id SERIAL PRIMARY KEY,
    preview_url VARCHAR(255) NOT NULL,  -- Preview for app
    name VARCHAR(255) NOT NULL,         -- Display name
    file_path VARCHAR(255) NOT NULL,    -- Path on Sense
    url VARCHAR(255) NOT NULL,          -- Path to full file
    sort_key INTEGER NOT NULL,          -- How to sort the values for displaying
    firmware_version INTEGER NOT NULL   -- Minimum firmware version
);


CREATE TABLE file_info (
    id SERIAL PRIMARY KEY,
    sort_key INTEGER NOT NULL,          -- How to sort the values for displaying
    firmware_version INTEGER NOT NULL,  -- Minimum firmware version
    type VARCHAR(255),
    path VARCHAR(255),
    sha VARCHAR(255),
    uri VARCHAR(255),
    preview_uri VARCHAR(255),
    name VARCHAR(255),
    is_public BOOLEAN DEFAULT FALSE
);

CREATE TABLE sense_file_info (
    id SERIAL PRIMARY KEY,
    sense_id VARCHAR(255) NOT NULL,
    file_info_id INTEGER NOT NULL REFERENCES file_info (id)
);

CREATE INDEX sense_id_idx on sense_file_info(sense_id);

GRANT ALL PRIVILEGES ON sleep_sound_durations TO ingress_user;
GRANT ALL PRIVILEGES ON sleep_sounds TO ingress_user;
GRANT ALL PRIVILEGES ON file_info TO ingress_user;
GRANT ALL PRIVILEGES ON sense_file_info TO ingress_user;

ALTER TABLE file_info ADD COLUMN size_bytes INTEGER;

-- Added May 16h 2016
ALTER TABLE accounts ADD COLUMN firstname VARCHAR(255);
ALTER TABLE accounts ADD COLUMN lastname VARCHAR(255);