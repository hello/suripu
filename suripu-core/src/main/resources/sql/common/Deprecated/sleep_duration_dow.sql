-- Deprecated. Data is in DynamoDB sleep_stats table

-- For Trends Graphs

-- Sleep duration by day of week
CREATE TABLE sleep_duration_dow (
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT,
  day_of_week INTEGER,
  duration_sum BIGINT default 0, -- minutes
  duration_count INTEGER default 0,   -- number of nights
  local_utc_updated TIMESTAMP
);

CREATE UNIQUE INDEX unique_account_id_duration_dow on sleep_duration_dow(account_id, day_of_week);
CREATE INDEX duration_dow_account_id on sleep_duration_dow(account_id);

GRANT ALL PRIVILEGES ON sleep_duration_dow TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_duration_dow_id_seq TO ingress_user;

