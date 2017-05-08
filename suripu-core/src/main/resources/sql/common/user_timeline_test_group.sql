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
ALTER TABLE timeline_feedback ADD COLUMN adjustment_delta_millis INTEGER;