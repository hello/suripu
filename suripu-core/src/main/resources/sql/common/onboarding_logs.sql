CREATE TABLE onboarding_logs (
    id SERIAL PRIMARY KEY,
    sense_id VARCHAR (24),
    utc_ts TIMESTAMP,
    pill_id VARCHAR (24),
    account_id BIGINT,
    info TEXT,
    result VARCHAR(255),
    operation VARCHAR(20),
    created_at TIMESTAMP default current_timestamp,
    ip VARCHAR(25)
);

CREATE UNIQUE INDEX unique_sense_id_time ON onboarding_logs(sense_id, account_id, utc_ts);
CREATE INDEX onboarding_logs_result_time ON onboarding_logs(result, utc_ts);
CREATE INDEX onboarding_logs_account_id_time ON onboarding_logs(account_id, utc_ts);

GRANT ALL PRIVILEGES ON onboarding_logs TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE onboarding_logs_id_seq TO ingress_user;
