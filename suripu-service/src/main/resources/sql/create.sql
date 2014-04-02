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

GRANT ALL PRIVILEGES ON device_sensors TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE device_sensors_id_seq TO ingress_user;

CREATE ROLE ingress_user WITH LOGIN ENCRYPTED PASSWORD 'hello ingress user' CREATEDB;
ALTER ROLE ingress_user REPLICATION;

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
    created TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_client_id on oauth_applications(client_id);

CREATE INDEX dev_account_id_idx on oauth_applications(dev_account_id);



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




CREATE TABLE account_device_map(
    id SERIAL PRIMARY KEY,
    account_id INTEGER,
    device_id VARCHAR(100),
    created_at TIMESTAMP default current_timestamp
);

-- TODO : create index for account_id

--CREATE TABLE sensor_samples (
--    id BIGSERIAL PRIMARY KEY,
--    device_id BIGINT,
--    sensor_id INT,
--    ts TIMESTAMP,
--    val INT
--);
--
--CREATE UNIQUE INDEX uniq_sample on sensor_samples(device_id, ts);
--
--GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;
--GRANT ALL PRIVILEGES ON sensor_samples TO ingress_user;