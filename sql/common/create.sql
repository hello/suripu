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


--
-- UPDATES TO ACCOUNT TABLE 2014-07-17
--

ALTER TABLE accounts DROP COLUMN tz;
ALTER TABLE accounts ADD COLUMN tz INTEGER;


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