-- table not found in sensor db as of 01/13/2016
CREATE TABLE device_sound(
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT,
    amplitude INTEGER,
    ts TIMESTAMP,
    offset_millis INTEGER
);

CREATE UNIQUE INDEX uniq_device_ts_sound on device_sound(device_id, ts);

GRANT ALL PRIVILEGES ON device_sound TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE device_sound_id_seq TO ingress_user;
