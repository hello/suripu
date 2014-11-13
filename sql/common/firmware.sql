CREATE TABLE firmware_updates (
    id SERIAL PRIMARY KEY,
    device_id VARCHAR(100),
    firmware_version INTEGER,

    s3_bucket VARCHAR(100),
    s3_key VARCHAR(255),

    copy_to_serial_flash BOOLEAN,
    reset_network_processor BOOLEAN,
    reset_application_processor BOOLEAN,

    serial_flash_filename VARCHAR (100),
    serial_flash_path VARCHAR (100),
    sd_card_filename VARCHAR (100),
    sd_card_path VARCHAR (100)
);


GRANT ALL PRIVILEGES ON firmware_updates TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE firmware_updates_id_seq TO ingress_user;


ALTER TABLE firmware_updates ADD COLUMN sha1 VARCHAR 100;