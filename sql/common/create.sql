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
-- SCORES -- not in common as of 01/13/2016
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


-- not in common as of 01/13/2016
CREATE TABLE tracking (id SERIAL PRIMARY KEY,
  sense_id VARCHAR(255),
  internal_sense_id BIGINT,
  account_id BIGINT,
  category SMALLINT,
  created_at TIMESTAMP
);

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

-- Added June 20th 2016
CREATE TABLE oauth_codes(
    id BIGSERIAL PRIMARY KEY,
    auth_code UUID,
    expires_in INTEGER,
    created_at TIMESTAMP default current_timestamp,
    app_id INTEGER,
    account_id INTEGER,
    scopes int[]
);

CREATE UNIQUE INDEX uniq_auth_code on oauth_codes(auth_code);

GRANT ALL PRIVILEGES ON oauth_codes TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE oauth_codes_id_seq TO ingress_user;

ALTER TABLE oauth_tokens ADD COLUMN refresh_expires_in INTEGER;

-- Added August 25th
CREATE TABLE sense_metadata (
    id BIGSERIAL PRIMARY KEY,
    sense_id VARCHAR(100),
    hw_version INTEGER,
    last_updated_at TIMESTAMP
);

CREATE INDEX sense_id_idx on sense_metadata(sense_id);

-- Added September 20th 2016
CREATE TABLE external_oauth_states(
    id BIGSERIAL PRIMARY KEY,
    auth_state VARCHAR(100),
    created_at TIMESTAMP default current_timestamp,
    app_id INTEGER,
    device_id VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uniq_auth_state on external_oauth_states(auth_state);

GRANT ALL PRIVILEGES ON external_oauth_states TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE external_oauth_states_id_seq TO ingress_user;


CREATE TABLE external_oauth_tokens(
    id BIGSERIAL PRIMARY KEY,
    access_token VARCHAR(511),
    refresh_token VARCHAR(511),
    access_expires_in INTEGER,
    refresh_expires_in INTEGER,
    created_at TIMESTAMP default current_timestamp,
    app_id INTEGER,
    device_id VARCHAR(255) NOT NULL
);

CREATE UNIQUE INDEX uniq_ext_access_token on external_oauth_tokens(access_token);
CREATE UNIQUE INDEX uniq_ext_refresh_token on external_oauth_tokens(refresh_token);

GRANT ALL PRIVILEGES ON external_oauth_tokens TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE external_oauth_tokens_id_seq TO ingress_user;

-- Added Sept 29th 2016
-- Expansions replacing 'external applications'
CREATE TABLE expansions (
    id SERIAL PRIMARY KEY,
    service_name VARCHAR (100),
    device_name VARCHAR (100),
    description VARCHAR (255),
    icon text,
    client_id VARCHAR (100),
    client_secret VARCHAR (100),
    api_uri VARCHAR (255),
    auth_uri VARCHAR (255),
    token_uri VARCHAR (255),
    refresh_uri VARCHAR (255),
    category VARCHAR(50),
    created TIMESTAMP default current_timestamp,
    grant_type INTEGER
);

CREATE UNIQUE INDEX uniq_exp_client_id on expansions(client_id);

GRANT ALL PRIVILEGES ON expansions TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE expansions_id_seq TO ingress_user;

INSERT INTO expansions (service_name, device_name, client_id, client_secret, api_uri, auth_uri, token_uri, refresh_uri, description, grant_type, category) VALUES('Nest', 'Nest Thermostat', 'f2961543-c903-4ad5-bf0f-e502eea8a476', '3kCD53nn0BYcb6e3rl7qPpEG3', 'https://developer-api.nest.com/', 'https://home.nest.com/login/oauth2', 'https://api.home.nest.com/oauth2/access_token', '', 'Nest API Integration', 2, 'temperature');
INSERT INTO expansions (service_name, device_name, client_id, client_secret, api_uri, auth_uri, token_uri, refresh_uri, description, grant_type, category) VALUES('Hue', 'Hue Light', '0diWnf29t9OkaOowxGYtVYXcn3MSRJni', 'YI0noOhH9AafLAGG', 'https://api.meethue.com/v2/', 'https://api.meethue.com/oauth2/auth?appid=sense-dev', 'https://api.meethue.com/oauth2/token', 'https://api.meethue.com/oauth2/refresh?grant_type=refresh_token', 'Hue API Integration', 2, 'light');

UPDATE expansions SET description = 'Connecting to Sense allows your Nest Learning Thermostat to set a specific temperature ahead of your Sense alarm, letting you wake up to your ideal temperature every morning.' WHERE service_name='Nest';
UPDATE expansions SET description = 'Connecting to Sense allows your Philips Hue system to activate a scene when Sense''s alarm sounds, letting you wake up to your ideal lighting every morning.' WHERE service_name='Hue';

UPDATE expansions SET icon = '{"phone_1x":"https://hello-dev.s3.amazonaws.com/josef/expansions/icon-nest@1x.png","phone_2x":"https://hello-dev.s3.amazonaws.com/josef/expansions/icon-nest@2x.png","phone_3x":"https://hello-dev.s3.amazonaws.com/josef/expansions/icon-nest@3x.png"}' WHERE service_name='Nest';
UPDATE expansions SET icon = '{"phone_1x":"https://hello-dev.s3.amazonaws.com/josef/expansions/icon-hue@1x.png","phone_2x":"https://hello-dev.s3.amazonaws.com/josef/expansions/icon-hue@2x.png","phone_3x":"https://hello-dev.s3.amazonaws.com/josef/expansions/icon-hue@3x.png"}' WHERE service_name='Hue';

CREATE TABLE expansion_data (
	id SERIAL PRIMARY KEY,
	app_id INTEGER,
	device_id VARCHAR(255) NOT NULL,
	created_at TIMESTAMP default current_timestamp,
	updated_at TIMESTAMP default current_timestamp,
	data text,
	enabled boolean
);

CREATE UNIQUE INDEX uniq_exp_app_device_id on expansion_data(app_id, device_id);

GRANT ALL PRIVILEGES ON expansion_data TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE expansion_data_id_seq TO ingress_user;


-- Added Oct 27th 2016
ALTER TABLE expansions ADD COLUMN company_name VARCHAR (100);
UPDATE expansions SET company_name='Phillips' WHERE service_name='HUE';
UPDATE expansions SET company_name='Nest' WHERE service_name='NEST';

ALTER TABLE expansion_data ADD COLUMN account_id bigint default null;

CREATE TABLE alerts (
    id SERIAL,
    account_id bigint,
    title text,
    body text,
    created_at timestamp without time zone DEFAULT timezone('utc'::text, now()),
    seen boolean DEFAULT false
);

GRANT ALL PRIVILEGES ON alerts TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE alerts_id_seq TO ingress_user;


-- Added Feb 3rd, 2017
CREATE TABLE voice_command_topics (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    icon VARCHAR(255) NOT NULL
);

GRANT ALL PRIVILEGES ON voice_command_topics TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE voice_command_topics_id_seq TO ingress_user;

CREATE TABLE voice_command_subtopics (
    id SERIAL PRIMARY KEY,
    command_title VARCHAR(255) NOT NULL,
    voice_command_topic_id INTEGER references voice_command_topics(id) NOT NULL
);

GRANT ALL PRIVILEGES ON voice_command_subtopics TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE voice_command_subtopics_id_seq TO ingress_user;

CREATE TABLE voice_commands(
    id SERIAL PRIMARY KEY,
    command VARCHAR(255) NOT NULL,
    voice_command_subtopic_id INTEGER references voice_command_subtopics(id) NOT NULL
);

GRANT ALL PRIVILEGES ON voice_commands TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE voice_commands_id_seq TO ingress_user;



INSERT INTO voice_command_topics (title, description, icon) VALUES
    ('Alarm and Sleep Sounds', 'Before each command, say "Okay Sense" to set an alarm or begin playing Sleep Sounds.', 'icon_alarms'),
    ('Sleep', 'Before each command, say "Okay Sense" to ask about your Sleep Score, Sleep Timeline, and Sleep Trends.', 'icon_sleep'),
    ('Room Conditions', 'Before each command, say "Okay Sense" to ask about your temperature, humidity, air quality, and more.', 'icon_rc'),
    ('Expansions', 'Before each command, say "Okay Sense" to control your lights or thermostat.', 'icon_expansions');

INSERT INTO voice_command_subtopics(command_title, voice_command_topic_id) VALUES
    ('Alarms', (SELECT id FROM voice_command_topics where title = 'Alarm and Sleep Sounds')),
    ('Sleep Sounds', (SELECT id FROM voice_command_topics where title = 'Alarm and Sleep Sounds')),
    ('Sleep Timeline', (SELECT id FROM voice_command_topics where title = 'Sleep')),
    ('Temperature', (SELECT id FROM voice_command_topics where title = 'Room Conditions')),
    ('Humidity', (SELECT id FROM voice_command_topics where title = 'Room Conditions')),
    ('Noise Level', (SELECT id FROM voice_command_topics where title = 'Room Conditions')),
    ('Air Quality', (SELECT id FROM voice_command_topics where title = 'Room Conditions')),
    ('Lights', (SELECT id FROM voice_command_topics where title = 'Expansions')),
    ('Thermostat', (SELECT id FROM voice_command_topics where title = 'Expansions'));

INSERT INTO voice_commands(command, voice_command_subtopic_id) VALUES
    ('Wake me up at 10 AM.', (SELECT id FROM voice_command_subtopics where command_title = 'Alarms')),
    ('Set alarm for tomorrow morning at 8.', (SELECT id FROM voice_command_subtopics where command_title = 'Alarms')),
    ('Play a Sleep Sound.',  (SELECT id FROM voice_command_subtopics where command_title = 'Sleep Sounds')),
    ('Play White Noise.',  (SELECT id FROM voice_command_subtopics where command_title = 'Sleep Sounds')),
    ('How was my sleep last night?', (SELECT id FROM voice_command_subtopics where command_title = 'Sleep Timeline')),
    ('What was my Sleep Score?', (SELECT id FROM voice_command_subtopics where command_title = 'Sleep Timeline')),
    ('What is the temperature?', (SELECT id FROM voice_command_subtopics where command_title = 'Temperature')),
    ('What is the humidity?', (SELECT id FROM voice_command_subtopics where command_title = 'Humidity')),
    ('How noisy is it?', (SELECT id FROM voice_command_subtopics where command_title = 'Noise Level')),
    ('What is the noise level?', (SELECT id FROM voice_command_subtopics where command_title = 'Noise Level')),
    ('How is the air quality?', (SELECT id FROM voice_command_subtopics where command_title = 'Air Quality')),
    ('Turn the lights on.', (SELECT id FROM voice_command_subtopics where command_title = 'Lights')),
    ('Turn the lights off.', (SELECT id FROM voice_command_subtopics where command_title = 'Lights')),
    ('Brighten the lights.', (SELECT id FROM voice_command_subtopics where command_title = 'Lights')),
    ('Dim the lights.',  (SELECT id FROM voice_command_subtopics where command_title = 'Lights')),
    ('Set the thermostat to 70°.',  (SELECT id FROM voice_command_subtopics where command_title = 'Thermostat'));


-- Added Feb 22nd
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- need this for the uuid generation
ALTER TABLE accounts ADD COLUMN external_id uuid DEFAULT uuid_generate_v4();

-- Added Feb 23rd
ALTER TABLE accounts ADD COLUMN gender_name VARCHAR(100);

-- 2017-03-22 file_info for Sense 1.5
CREATE TABLE file_info_one_five (
    id SERIAL PRIMARY KEY,
    sort_key INTEGER NOT NULL,          -- How to sort the values for displaying
    firmware_version INTEGER NOT NULL,  -- Minimum firmware version
    type VARCHAR(255),
    path VARCHAR(255),
    sha VARCHAR(255),
    uri VARCHAR(255),
    preview_uri VARCHAR(255),
    name VARCHAR(255),
    size_bytes INTEGER,
    is_public BOOLEAN DEFAULT FALSE
);

CREATE TABLE sense_file_info_one_five (
    id SERIAL PRIMARY KEY,
    sense_id VARCHAR(255) NOT NULL,
    file_info_id INTEGER NOT NULL REFERENCES file_info_one_five (id)
);

CREATE INDEX sense_id_one_five_idx on sense_file_info_one_five(sense_id);

GRANT ALL PRIVILEGES ON file_info_one_five TO ingress_user;
GRANT ALL PRIVILEGES ON sense_file_info_one_five TO ingress_user;

INSERT INTO file_info_one_five
(id, sort_key, firmware_version, type, path, sha, uri, preview_uri, name, is_public, size_bytes)
VALUES
( 9, 11, 4215, 'SLEEP_SOUND', '/SLPTONES/ST001.RAW', '7dd42ec7e55b00afbbcb2a8e129dc0fc573961eb', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST001.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Brown_Noise.mp3', 'Brown Noise', true,    161552),
(10,  5, 4215, 'SLEEP_SOUND', '/SLPTONES/ST002.RAW', 'd30817227ce93708167be1022de1f1625853ffce', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST002.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Cosmos.mp3', 'Cosmos', true,    628054),
(11,  6, 4215, 'SLEEP_SOUND', '/SLPTONES/ST003.RAW', '777a25489420095ce034bd082d964fc47d9c7c78', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST003.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Autumn_Wind.mp3', 'Autumn Wind', true,   2304000),
(12,  7, 4215, 'SLEEP_SOUND', '/SLPTONES/ST004.RAW', '686ab4987f6014611b1bc18872364e76ab98277d', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST004.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Fireside.mp3', 'Fireside', true,    905342),
(13,  8, 4215, 'SLEEP_SOUND', '/SLPTONES/ST005.RAW', '25f05b2302ae69c1a501989852497257a1cf31d7', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST005.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Ocean_Waves.mp3', 'Ocean Waves', false,  1669802),
(14,  9, 4215, 'SLEEP_SOUND', '/SLPTONES/ST006.RAW', '128cf3d664e39667e4eabff647f8ed0cc4edd109', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST006.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Rainfall.mp3', 'Rainfall', true,    539354),
(15, 12, 4215, 'SLEEP_SOUND', '/SLPTONES/ST007.RAW', 'bd26a9cbbe9781852c5454706c1b04e742d72f2e', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST007.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/White_Noise.mp3', 'White Noise', true,     81866),
(16, 10, 4215, 'SLEEP_SOUND', '/SLPTONES/ST008.RAW', '56ac9affc489328cd14bdd35bd0c256635ab0faa', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST008.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Forest_Creek.mp3', 'Forest Creek', true,    595286),
(17,  3, 4215, 'SLEEP_SOUND', '/SLPTONES/ST009.RAW', 'a0a370f64cd543449f055f8ca666bfff40bf6620', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST009.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Morpheus.mp3', 'Morpheus', true,  11004180),
(18,  1, 4215, 'SLEEP_SOUND', '/SLPTONES/ST010.RAW', 'f7b36fb9c4ade09397ce3135b1bd0d2c3b0cfb12', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST010.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Aura.mp3', 'Aura', true,   6247766),
(19,  4, 4215, 'SLEEP_SOUND', '/SLPTONES/ST011.RAW', '684dcf2842df76cf0409bf51c7e303bae92e25d0', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST011.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Horizon.mp3', 'Horizon', true,   8476982),
(20,  2, 4215, 'SLEEP_SOUND', '/SLPTONES/ST012.RAW', '3fa21852f29d15b3d8d8eeb0d05c6c42b7ca9041', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST012.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Nocturne.mp3', 'Nocturne', true,   6323200);

-- Special non-public sound files for 60D3DE89DB96F490 (ST106) and B87BC42357A797BF (ST101) to address buffer-underrun problem
INSERT INTO file_info_one_five
(id, sort_key, firmware_version, type, path, sha, uri, preview_uri, name, is_public, size_bytes)
VALUES
( 100, 13, 4215, 'SLEEP_SOUND', '/SLPTONES/ST101.RAW', '7dd42ec7e55b00afbbcb2a8e129dc0fc573961eb', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST101.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Brown_Noise.mp3', 'Brown Noise Special', false, 161552),
( 101, 14, 4215, 'SLEEP_SOUND', '/SLPTONES/ST106.RAW', '128cf3d664e39667e4eabff647f8ed0cc4edd109', 's3://hello-audio/sleep-tones-raw-one-five/2017-03-23/ST106.raw', 'https://s3.amazonaws.com/hello-audio/sleep-tones-preview/Rainfall.mp3', 'Rainfall Special', false, 539354);
