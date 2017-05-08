CREATE TABLE notifications_subscriptions (
    id SERIAL,
    account_id BIGINT,
    os VARCHAR(10),
    version VARCHAR(10),
    app_version VARCHAR(10),
    device_token VARCHAR,
    endpoint VARCHAR,
    oauth_token VARCHAR,
    created_at_utc TIMESTAMP
);

CREATE UNIQUE INDEX device_token ON notifications_subscriptions (device_token);

-- WARNING: required for prod, but invalid for tests :(

GRANT ALL PRIVILEGES ON notifications_subscriptions TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE notifications_subscriptions_id_seq TO ingress_user;