
CREATE TYPE USER_LABEL_TYPE AS ENUM ('make_bed', 'went_to_bed', 'fall_asleep',
    'awake', 'out_of_bed', 'awake_in_bed',
    'sound_disturbance', 'got_up_at_night', 'other_disturbance');


-- table to save user-annotation for timeline

CREATE TABLE user_labels (
    id SERIAL PRIMARY KEY,
    account_id BIGINT,
    email VARCHAR (255),
    label USER_LABEL_TYPE,
    night_date TIMESTAMP,
    utc_ts TIMESTAMP,
    local_utc_ts TIMESTAMP,
    tz_offset int,
    created TIMESTAMP default current_timestamp
);

GRANT ALL PRIVILEGES ON user_labels TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE user_labels_id_seq TO ingress_user;
CREATE INDEX user_labels_account_id_night ON user_labels(account_id, night_date);

ALTER TABLE user_labels ADD COLUMN duration INT DEFAULT 0;
ALTER TABLE user_labels ADD COLUMN note VARCHAR(255) DEFAULT '';
