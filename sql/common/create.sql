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