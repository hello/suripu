CREATE TABLE account_device_map(
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    device_name VARCHAR(100),
    created_at TIMESTAMP default current_timestamp
);

GRANT ALL PRIVILEGES ON account_device_map TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_device_map_id_seq TO ingress_user;

ALTER TABLE account_device_map ADD COLUMN active BOOLEAN DEFAULT TRUE;
ALTER TABLE account_device_map ADD COLUMN last_updated TIMESTAMP default current_timestamp;

CREATE UNIQUE INDEX uniq_account_device_name on account_device_map(account_id, device_name, active);
