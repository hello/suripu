-- Deprecated


-- sleep duration over time

CREATE TABLE sleep_stats_time(
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT,
  duration INTEGER, -- minutes
  sound_sleep INTEGER, -- mins
  light_sleep INTEGER,
  sleep_time_utc TIMESTAMP, -- fall asleep time in UTC
  wake_time_utc TIMESTAMP, -- wake up time in UTC
  fall_asleep_time INTEGER, -- mins taken to fall asleep after getting into bed
  motion INTEGER, -- number of motion events
  offset_millis INTEGER, -- timezone offset
  local_utc_date TIMESTAMP -- night of yyyy-mm-dd
);

CREATE UNIQUE INDEX unique_account_date_duration on sleep_stats_time(account_id, local_utc_date);
GRANT ALL PRIVILEGES ON sleep_stats_time TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE sleep_stats_time_id_seq TO ingress_user;

-- to alter sleep_stats_time table (01/02/2015)
ALTER TABLE sleep_stats_time ADD COLUMN sleep_time_utc TIMESTAMP;
ALTER TABLE sleep_stats_time ADD COLUMN wake_time_utc TIMESTAMP;
ALTER TABLE sleep_stats_time ADD COLUMN fall_asleep_time INTEGER default 0;
