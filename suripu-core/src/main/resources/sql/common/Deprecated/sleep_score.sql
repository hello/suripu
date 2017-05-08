-- Deprecated, data in DynamoDB now

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