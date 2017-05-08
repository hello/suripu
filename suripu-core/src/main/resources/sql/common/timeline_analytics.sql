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