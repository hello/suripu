--
-- Insights DB
--

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

-- Sleep score by day of week
CREATE TABLE sleep_score_dow (
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT,
  day_of_week INTEGER,
  score_sum BIGINT default 0, -- sum of scores
  score_count INTEGER default 0, -- number of nights
  local_utc_updated TIMESTAMP
);

CREATE UNIQUE INDEX unique_account_id_score_dow on sleep_score_dow(account_id, day_of_week);
CREATE INDEX scores_dow_account_id on sleep_score_dow(account_id);

GRANT ALL PRIVILEGES ON sleep_score_dow TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_score_dow_id_seq TO ingress_user;

-- sleep duration over time

CREATE TABLE sleep_stats_time(
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT,
  duration INTEGER, -- minutes
  sound_sleep INTEGER, -- mins
  light_sleep INTEGER,
  sleep_time_local_utc TIMESTAMP, -- fall asleep time in local-utc
  wake_time_local_utc TIMESTAMP, -- wake up time
  time_to_sleep INTEGER, -- mins taken to fall asleep after getting into bed
  motion INTEGER, -- number of motion events
  offset_millis INTEGER, -- timezone offset
  local_utc_date TIMESTAMP -- night of yyyy-mm-dd
);

CREATE UNIQUE INDEX unique_account_date_duration on sleep_stats_time(account_id, local_utc_date);
GRANT ALL PRIVILEGES ON sleep_stats_time TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_stats_time_id_seq TO ingress_user;

-- to alter sleep_stats_time table (01/02/2015)
ALTER TABLE sleep_stats_time ADD COLUMN sleep_time_local_utc TIMESTAMP;
ALTER TABLE sleep_stats_time ADD COLUMN wake_time_local_utc TIMESTAMP;
ALTER TABLE sleep_stats_time ADD COLUMN time_to_sleep INTEGER;