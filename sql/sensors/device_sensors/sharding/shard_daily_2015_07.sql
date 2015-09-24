
-- 2015-07 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_01_uniq_device_id_account_id_ts on device_sensors_par_2015_07_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_01_ts_idx on device_sensors_par_2015_07_01(ts);
ALTER TABLE device_sensors_par_2015_07_01 ADD CHECK (local_utc_ts >= '2015-07-01 00:00:00' AND local_utc_ts < '2015-07-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_02_uniq_device_id_account_id_ts on device_sensors_par_2015_07_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_02_ts_idx on device_sensors_par_2015_07_02(ts);
ALTER TABLE device_sensors_par_2015_07_02 ADD CHECK (local_utc_ts >= '2015-07-02 00:00:00' AND local_utc_ts < '2015-07-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_03_uniq_device_id_account_id_ts on device_sensors_par_2015_07_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_03_ts_idx on device_sensors_par_2015_07_03(ts);
ALTER TABLE device_sensors_par_2015_07_03 ADD CHECK (local_utc_ts >= '2015-07-03 00:00:00' AND local_utc_ts < '2015-07-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_04_uniq_device_id_account_id_ts on device_sensors_par_2015_07_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_04_ts_idx on device_sensors_par_2015_07_04(ts);
ALTER TABLE device_sensors_par_2015_07_04 ADD CHECK (local_utc_ts >= '2015-07-04 00:00:00' AND local_utc_ts < '2015-07-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_05_uniq_device_id_account_id_ts on device_sensors_par_2015_07_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_05_ts_idx on device_sensors_par_2015_07_05(ts);
ALTER TABLE device_sensors_par_2015_07_05 ADD CHECK (local_utc_ts >= '2015-07-05 00:00:00' AND local_utc_ts < '2015-07-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_06_uniq_device_id_account_id_ts on device_sensors_par_2015_07_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_06_ts_idx on device_sensors_par_2015_07_06(ts);
ALTER TABLE device_sensors_par_2015_07_06 ADD CHECK (local_utc_ts >= '2015-07-06 00:00:00' AND local_utc_ts < '2015-07-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_07_uniq_device_id_account_id_ts on device_sensors_par_2015_07_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_07_ts_idx on device_sensors_par_2015_07_07(ts);
ALTER TABLE device_sensors_par_2015_07_07 ADD CHECK (local_utc_ts >= '2015-07-07 00:00:00' AND local_utc_ts < '2015-07-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_08_uniq_device_id_account_id_ts on device_sensors_par_2015_07_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_08_ts_idx on device_sensors_par_2015_07_08(ts);
ALTER TABLE device_sensors_par_2015_07_08 ADD CHECK (local_utc_ts >= '2015-07-08 00:00:00' AND local_utc_ts < '2015-07-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_09_uniq_device_id_account_id_ts on device_sensors_par_2015_07_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_09_ts_idx on device_sensors_par_2015_07_09(ts);
ALTER TABLE device_sensors_par_2015_07_09 ADD CHECK (local_utc_ts >= '2015-07-09 00:00:00' AND local_utc_ts < '2015-07-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_10_uniq_device_id_account_id_ts on device_sensors_par_2015_07_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_10_ts_idx on device_sensors_par_2015_07_10(ts);
ALTER TABLE device_sensors_par_2015_07_10 ADD CHECK (local_utc_ts >= '2015-07-10 00:00:00' AND local_utc_ts < '2015-07-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_11_uniq_device_id_account_id_ts on device_sensors_par_2015_07_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_11_ts_idx on device_sensors_par_2015_07_11(ts);
ALTER TABLE device_sensors_par_2015_07_11 ADD CHECK (local_utc_ts >= '2015-07-11 00:00:00' AND local_utc_ts < '2015-07-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_12_uniq_device_id_account_id_ts on device_sensors_par_2015_07_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_12_ts_idx on device_sensors_par_2015_07_12(ts);
ALTER TABLE device_sensors_par_2015_07_12 ADD CHECK (local_utc_ts >= '2015-07-12 00:00:00' AND local_utc_ts < '2015-07-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_13_uniq_device_id_account_id_ts on device_sensors_par_2015_07_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_13_ts_idx on device_sensors_par_2015_07_13(ts);
ALTER TABLE device_sensors_par_2015_07_13 ADD CHECK (local_utc_ts >= '2015-07-13 00:00:00' AND local_utc_ts < '2015-07-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_14_uniq_device_id_account_id_ts on device_sensors_par_2015_07_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_14_ts_idx on device_sensors_par_2015_07_14(ts);
ALTER TABLE device_sensors_par_2015_07_14 ADD CHECK (local_utc_ts >= '2015-07-14 00:00:00' AND local_utc_ts < '2015-07-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_15_uniq_device_id_account_id_ts on device_sensors_par_2015_07_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_15_ts_idx on device_sensors_par_2015_07_15(ts);
ALTER TABLE device_sensors_par_2015_07_15 ADD CHECK (local_utc_ts >= '2015-07-15 00:00:00' AND local_utc_ts < '2015-07-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_16_uniq_device_id_account_id_ts on device_sensors_par_2015_07_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_16_ts_idx on device_sensors_par_2015_07_16(ts);
ALTER TABLE device_sensors_par_2015_07_16 ADD CHECK (local_utc_ts >= '2015-07-16 00:00:00' AND local_utc_ts < '2015-07-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_17_uniq_device_id_account_id_ts on device_sensors_par_2015_07_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_17_ts_idx on device_sensors_par_2015_07_17(ts);
ALTER TABLE device_sensors_par_2015_07_17 ADD CHECK (local_utc_ts >= '2015-07-17 00:00:00' AND local_utc_ts < '2015-07-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_18_uniq_device_id_account_id_ts on device_sensors_par_2015_07_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_18_ts_idx on device_sensors_par_2015_07_18(ts);
ALTER TABLE device_sensors_par_2015_07_18 ADD CHECK (local_utc_ts >= '2015-07-18 00:00:00' AND local_utc_ts < '2015-07-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_19_uniq_device_id_account_id_ts on device_sensors_par_2015_07_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_19_ts_idx on device_sensors_par_2015_07_19(ts);
ALTER TABLE device_sensors_par_2015_07_19 ADD CHECK (local_utc_ts >= '2015-07-19 00:00:00' AND local_utc_ts < '2015-07-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_20_uniq_device_id_account_id_ts on device_sensors_par_2015_07_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_20_ts_idx on device_sensors_par_2015_07_20(ts);
ALTER TABLE device_sensors_par_2015_07_20 ADD CHECK (local_utc_ts >= '2015-07-20 00:00:00' AND local_utc_ts < '2015-07-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_21_uniq_device_id_account_id_ts on device_sensors_par_2015_07_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_21_ts_idx on device_sensors_par_2015_07_21(ts);
ALTER TABLE device_sensors_par_2015_07_21 ADD CHECK (local_utc_ts >= '2015-07-21 00:00:00' AND local_utc_ts < '2015-07-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_22_uniq_device_id_account_id_ts on device_sensors_par_2015_07_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_22_ts_idx on device_sensors_par_2015_07_22(ts);
ALTER TABLE device_sensors_par_2015_07_22 ADD CHECK (local_utc_ts >= '2015-07-22 00:00:00' AND local_utc_ts < '2015-07-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_23_uniq_device_id_account_id_ts on device_sensors_par_2015_07_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_23_ts_idx on device_sensors_par_2015_07_23(ts);
ALTER TABLE device_sensors_par_2015_07_23 ADD CHECK (local_utc_ts >= '2015-07-23 00:00:00' AND local_utc_ts < '2015-07-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_24_uniq_device_id_account_id_ts on device_sensors_par_2015_07_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_24_ts_idx on device_sensors_par_2015_07_24(ts);
ALTER TABLE device_sensors_par_2015_07_24 ADD CHECK (local_utc_ts >= '2015-07-24 00:00:00' AND local_utc_ts < '2015-07-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_25_uniq_device_id_account_id_ts on device_sensors_par_2015_07_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_25_ts_idx on device_sensors_par_2015_07_25(ts);
ALTER TABLE device_sensors_par_2015_07_25 ADD CHECK (local_utc_ts >= '2015-07-25 00:00:00' AND local_utc_ts < '2015-07-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_26_uniq_device_id_account_id_ts on device_sensors_par_2015_07_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_26_ts_idx on device_sensors_par_2015_07_26(ts);
ALTER TABLE device_sensors_par_2015_07_26 ADD CHECK (local_utc_ts >= '2015-07-26 00:00:00' AND local_utc_ts < '2015-07-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_27_uniq_device_id_account_id_ts on device_sensors_par_2015_07_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_27_ts_idx on device_sensors_par_2015_07_27(ts);
ALTER TABLE device_sensors_par_2015_07_27 ADD CHECK (local_utc_ts >= '2015-07-27 00:00:00' AND local_utc_ts < '2015-07-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_28_uniq_device_id_account_id_ts on device_sensors_par_2015_07_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_28_ts_idx on device_sensors_par_2015_07_28(ts);
ALTER TABLE device_sensors_par_2015_07_28 ADD CHECK (local_utc_ts >= '2015-07-28 00:00:00' AND local_utc_ts < '2015-07-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_29_uniq_device_id_account_id_ts on device_sensors_par_2015_07_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_29_ts_idx on device_sensors_par_2015_07_29(ts);
ALTER TABLE device_sensors_par_2015_07_29 ADD CHECK (local_utc_ts >= '2015-07-29 00:00:00' AND local_utc_ts < '2015-07-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_30_uniq_device_id_account_id_ts on device_sensors_par_2015_07_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_30_ts_idx on device_sensors_par_2015_07_30(ts);
ALTER TABLE device_sensors_par_2015_07_30 ADD CHECK (local_utc_ts >= '2015-07-30 00:00:00' AND local_utc_ts < '2015-07-31 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_07_31() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_07_31_uniq_device_id_account_id_ts on device_sensors_par_2015_07_31(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_07_31_ts_idx on device_sensors_par_2015_07_31(ts);
ALTER TABLE device_sensors_par_2015_07_31 ADD CHECK (local_utc_ts >= '2015-07-31 00:00:00' AND local_utc_ts < '2015-08-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-07-31 00:00:00' AND NEW.local_utc_ts < '2015-08-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-30 00:00:00' AND NEW.local_utc_ts < '2015-07-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-29 00:00:00' AND NEW.local_utc_ts < '2015-07-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-28 00:00:00' AND NEW.local_utc_ts < '2015-07-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-27 00:00:00' AND NEW.local_utc_ts < '2015-07-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-26 00:00:00' AND NEW.local_utc_ts < '2015-07-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-25 00:00:00' AND NEW.local_utc_ts < '2015-07-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-24 00:00:00' AND NEW.local_utc_ts < '2015-07-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-23 00:00:00' AND NEW.local_utc_ts < '2015-07-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-22 00:00:00' AND NEW.local_utc_ts < '2015-07-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-21 00:00:00' AND NEW.local_utc_ts < '2015-07-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-20 00:00:00' AND NEW.local_utc_ts < '2015-07-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-19 00:00:00' AND NEW.local_utc_ts < '2015-07-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-18 00:00:00' AND NEW.local_utc_ts < '2015-07-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-17 00:00:00' AND NEW.local_utc_ts < '2015-07-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-16 00:00:00' AND NEW.local_utc_ts < '2015-07-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-15 00:00:00' AND NEW.local_utc_ts < '2015-07-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-14 00:00:00' AND NEW.local_utc_ts < '2015-07-15 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-13 00:00:00' AND NEW.local_utc_ts < '2015-07-14 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-12 00:00:00' AND NEW.local_utc_ts < '2015-07-13 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-11 00:00:00' AND NEW.local_utc_ts < '2015-07-12 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-10 00:00:00' AND NEW.local_utc_ts < '2015-07-11 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-09 00:00:00' AND NEW.local_utc_ts < '2015-07-10 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-08 00:00:00' AND NEW.local_utc_ts < '2015-07-09 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-07 00:00:00' AND NEW.local_utc_ts < '2015-07-08 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-06 00:00:00' AND NEW.local_utc_ts < '2015-07-07 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-05 00:00:00' AND NEW.local_utc_ts < '2015-07-06 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-04 00:00:00' AND NEW.local_utc_ts < '2015-07-05 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-03 00:00:00' AND NEW.local_utc_ts < '2015-07-04 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-02 00:00:00' AND NEW.local_utc_ts < '2015-07-03 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-01 00:00:00' AND NEW.local_utc_ts < '2015-07-02 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-06-01 00:00:00' AND NEW.local_utc_ts < '2015-07-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-05-01 00:00:00' AND NEW.local_utc_ts < '2015-06-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-03-01 00:00:00' AND NEW.local_utc_ts < '2015-04-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-02-01 00:00:00' AND NEW.local_utc_ts < '2015-03-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_02 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2015-07 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_07_uniq_tracker_ts on tracker_motion_par_2015_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_07_uniq_tracker_id_account_id_ts on tracker_motion_par_2015_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_07_local_utc_ts on tracker_motion_par_2015_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_07 ADD CHECK (local_utc_ts >= '2015-07-01 00:00:00' AND local_utc_ts < '2015-08-01 00:00:00');

CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-07-01 00:00:00' AND NEW.local_utc_ts < '2015-08-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-06-01 00:00:00' AND NEW.local_utc_ts < '2015-07-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-05-01 00:00:00' AND NEW.local_utc_ts < '2015-06-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_05 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;