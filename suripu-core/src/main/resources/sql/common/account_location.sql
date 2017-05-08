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