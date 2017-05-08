-- Deprecated

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

