-- Keeping track of trackers for each users
-- Normally should only be one / account
-- but might want to support multiple for debug/admin purposes

CREATE TABLE account_tracker_map(
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    device_id VARCHAR(100),
    created_at TIMESTAMP default current_timestamp
);

-- CREATE UNIQUE INDEX uniq_account_tracker on account_tracker_map(account_id, device_id);
CREATE UNIQUE INDEX uniq_account_tracker_active on account_tracker_map(account_id, device_id, active);
CREATE INDEX pill_id_active ON account_tracker_map(device_id, active);


GRANT ALL PRIVILEGES ON account_tracker_map TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_tracker_map_id_seq TO ingress_user;

ALTER TABLE account_tracker_map ADD COLUMN active BOOLEAN DEFAULT TRUE;
ALTER TABLE account_tracker_map ADD COLUMN last_updated TIMESTAMP default current_timestamp;
