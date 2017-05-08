
-- 2015-09 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_01_uniq_device_id_account_id_ts on device_sensors_par_2015_09_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_01_ts_idx on device_sensors_par_2015_09_01(ts);
ALTER TABLE device_sensors_par_2015_09_01 ADD CHECK (local_utc_ts >= '2015-09-01 00:00:00' AND local_utc_ts < '2015-09-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_02_uniq_device_id_account_id_ts on device_sensors_par_2015_09_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_02_ts_idx on device_sensors_par_2015_09_02(ts);
ALTER TABLE device_sensors_par_2015_09_02 ADD CHECK (local_utc_ts >= '2015-09-02 00:00:00' AND local_utc_ts < '2015-09-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_03_uniq_device_id_account_id_ts on device_sensors_par_2015_09_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_03_ts_idx on device_sensors_par_2015_09_03(ts);
ALTER TABLE device_sensors_par_2015_09_03 ADD CHECK (local_utc_ts >= '2015-09-03 00:00:00' AND local_utc_ts < '2015-09-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_04_uniq_device_id_account_id_ts on device_sensors_par_2015_09_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_04_ts_idx on device_sensors_par_2015_09_04(ts);
ALTER TABLE device_sensors_par_2015_09_04 ADD CHECK (local_utc_ts >= '2015-09-04 00:00:00' AND local_utc_ts < '2015-09-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_05_uniq_device_id_account_id_ts on device_sensors_par_2015_09_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_05_ts_idx on device_sensors_par_2015_09_05(ts);
ALTER TABLE device_sensors_par_2015_09_05 ADD CHECK (local_utc_ts >= '2015-09-05 00:00:00' AND local_utc_ts < '2015-09-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_06_uniq_device_id_account_id_ts on device_sensors_par_2015_09_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_06_ts_idx on device_sensors_par_2015_09_06(ts);
ALTER TABLE device_sensors_par_2015_09_06 ADD CHECK (local_utc_ts >= '2015-09-06 00:00:00' AND local_utc_ts < '2015-09-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_07_uniq_device_id_account_id_ts on device_sensors_par_2015_09_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_07_ts_idx on device_sensors_par_2015_09_07(ts);
ALTER TABLE device_sensors_par_2015_09_07 ADD CHECK (local_utc_ts >= '2015-09-07 00:00:00' AND local_utc_ts < '2015-09-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_08_uniq_device_id_account_id_ts on device_sensors_par_2015_09_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_08_ts_idx on device_sensors_par_2015_09_08(ts);
ALTER TABLE device_sensors_par_2015_09_08 ADD CHECK (local_utc_ts >= '2015-09-08 00:00:00' AND local_utc_ts < '2015-09-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_09_uniq_device_id_account_id_ts on device_sensors_par_2015_09_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_09_ts_idx on device_sensors_par_2015_09_09(ts);
ALTER TABLE device_sensors_par_2015_09_09 ADD CHECK (local_utc_ts >= '2015-09-09 00:00:00' AND local_utc_ts < '2015-09-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_10_uniq_device_id_account_id_ts on device_sensors_par_2015_09_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_10_ts_idx on device_sensors_par_2015_09_10(ts);
ALTER TABLE device_sensors_par_2015_09_10 ADD CHECK (local_utc_ts >= '2015-09-10 00:00:00' AND local_utc_ts < '2015-09-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_11_uniq_device_id_account_id_ts on device_sensors_par_2015_09_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_11_ts_idx on device_sensors_par_2015_09_11(ts);
ALTER TABLE device_sensors_par_2015_09_11 ADD CHECK (local_utc_ts >= '2015-09-11 00:00:00' AND local_utc_ts < '2015-09-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_12_uniq_device_id_account_id_ts on device_sensors_par_2015_09_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_12_ts_idx on device_sensors_par_2015_09_12(ts);
ALTER TABLE device_sensors_par_2015_09_12 ADD CHECK (local_utc_ts >= '2015-09-12 00:00:00' AND local_utc_ts < '2015-09-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_13_uniq_device_id_account_id_ts on device_sensors_par_2015_09_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_13_ts_idx on device_sensors_par_2015_09_13(ts);
ALTER TABLE device_sensors_par_2015_09_13 ADD CHECK (local_utc_ts >= '2015-09-13 00:00:00' AND local_utc_ts < '2015-09-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_14_uniq_device_id_account_id_ts on device_sensors_par_2015_09_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_14_ts_idx on device_sensors_par_2015_09_14(ts);
ALTER TABLE device_sensors_par_2015_09_14 ADD CHECK (local_utc_ts >= '2015-09-14 00:00:00' AND local_utc_ts < '2015-09-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_15_uniq_device_id_account_id_ts on device_sensors_par_2015_09_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_15_ts_idx on device_sensors_par_2015_09_15(ts);
ALTER TABLE device_sensors_par_2015_09_15 ADD CHECK (local_utc_ts >= '2015-09-15 00:00:00' AND local_utc_ts < '2015-09-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_16_uniq_device_id_account_id_ts on device_sensors_par_2015_09_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_16_ts_idx on device_sensors_par_2015_09_16(ts);
ALTER TABLE device_sensors_par_2015_09_16 ADD CHECK (local_utc_ts >= '2015-09-16 00:00:00' AND local_utc_ts < '2015-09-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_17_uniq_device_id_account_id_ts on device_sensors_par_2015_09_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_17_ts_idx on device_sensors_par_2015_09_17(ts);
ALTER TABLE device_sensors_par_2015_09_17 ADD CHECK (local_utc_ts >= '2015-09-17 00:00:00' AND local_utc_ts < '2015-09-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_18_uniq_device_id_account_id_ts on device_sensors_par_2015_09_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_18_ts_idx on device_sensors_par_2015_09_18(ts);
ALTER TABLE device_sensors_par_2015_09_18 ADD CHECK (local_utc_ts >= '2015-09-18 00:00:00' AND local_utc_ts < '2015-09-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_19_uniq_device_id_account_id_ts on device_sensors_par_2015_09_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_19_ts_idx on device_sensors_par_2015_09_19(ts);
ALTER TABLE device_sensors_par_2015_09_19 ADD CHECK (local_utc_ts >= '2015-09-19 00:00:00' AND local_utc_ts < '2015-09-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_20_uniq_device_id_account_id_ts on device_sensors_par_2015_09_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_20_ts_idx on device_sensors_par_2015_09_20(ts);
ALTER TABLE device_sensors_par_2015_09_20 ADD CHECK (local_utc_ts >= '2015-09-20 00:00:00' AND local_utc_ts < '2015-09-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_21_uniq_device_id_account_id_ts on device_sensors_par_2015_09_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_21_ts_idx on device_sensors_par_2015_09_21(ts);
ALTER TABLE device_sensors_par_2015_09_21 ADD CHECK (local_utc_ts >= '2015-09-21 00:00:00' AND local_utc_ts < '2015-09-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_22_uniq_device_id_account_id_ts on device_sensors_par_2015_09_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_22_ts_idx on device_sensors_par_2015_09_22(ts);
ALTER TABLE device_sensors_par_2015_09_22 ADD CHECK (local_utc_ts >= '2015-09-22 00:00:00' AND local_utc_ts < '2015-09-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_23_uniq_device_id_account_id_ts on device_sensors_par_2015_09_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_23_ts_idx on device_sensors_par_2015_09_23(ts);
ALTER TABLE device_sensors_par_2015_09_23 ADD CHECK (local_utc_ts >= '2015-09-23 00:00:00' AND local_utc_ts < '2015-09-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_24_uniq_device_id_account_id_ts on device_sensors_par_2015_09_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_24_ts_idx on device_sensors_par_2015_09_24(ts);
ALTER TABLE device_sensors_par_2015_09_24 ADD CHECK (local_utc_ts >= '2015-09-24 00:00:00' AND local_utc_ts < '2015-09-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_25_uniq_device_id_account_id_ts on device_sensors_par_2015_09_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_25_ts_idx on device_sensors_par_2015_09_25(ts);
ALTER TABLE device_sensors_par_2015_09_25 ADD CHECK (local_utc_ts >= '2015-09-25 00:00:00' AND local_utc_ts < '2015-09-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_26_uniq_device_id_account_id_ts on device_sensors_par_2015_09_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_26_ts_idx on device_sensors_par_2015_09_26(ts);
ALTER TABLE device_sensors_par_2015_09_26 ADD CHECK (local_utc_ts >= '2015-09-26 00:00:00' AND local_utc_ts < '2015-09-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_27_uniq_device_id_account_id_ts on device_sensors_par_2015_09_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_27_ts_idx on device_sensors_par_2015_09_27(ts);
ALTER TABLE device_sensors_par_2015_09_27 ADD CHECK (local_utc_ts >= '2015-09-27 00:00:00' AND local_utc_ts < '2015-09-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_28_uniq_device_id_account_id_ts on device_sensors_par_2015_09_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_28_ts_idx on device_sensors_par_2015_09_28(ts);
ALTER TABLE device_sensors_par_2015_09_28 ADD CHECK (local_utc_ts >= '2015-09-28 00:00:00' AND local_utc_ts < '2015-09-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_29_uniq_device_id_account_id_ts on device_sensors_par_2015_09_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_29_ts_idx on device_sensors_par_2015_09_29(ts);
ALTER TABLE device_sensors_par_2015_09_29 ADD CHECK (local_utc_ts >= '2015-09-29 00:00:00' AND local_utc_ts < '2015-09-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_09_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_09_30_uniq_device_id_account_id_ts on device_sensors_par_2015_09_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_09_30_ts_idx on device_sensors_par_2015_09_30(ts);
ALTER TABLE device_sensors_par_2015_09_30 ADD CHECK (local_utc_ts >= '2015-09-30 00:00:00' AND local_utc_ts < '2015-10-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-09-30 00:00:00' AND NEW.local_utc_ts < '2015-10-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-29 00:00:00' AND NEW.local_utc_ts < '2015-09-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-28 00:00:00' AND NEW.local_utc_ts < '2015-09-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-27 00:00:00' AND NEW.local_utc_ts < '2015-09-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-26 00:00:00' AND NEW.local_utc_ts < '2015-09-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-25 00:00:00' AND NEW.local_utc_ts < '2015-09-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-24 00:00:00' AND NEW.local_utc_ts < '2015-09-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-23 00:00:00' AND NEW.local_utc_ts < '2015-09-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-22 00:00:00' AND NEW.local_utc_ts < '2015-09-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-21 00:00:00' AND NEW.local_utc_ts < '2015-09-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-20 00:00:00' AND NEW.local_utc_ts < '2015-09-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-19 00:00:00' AND NEW.local_utc_ts < '2015-09-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-18 00:00:00' AND NEW.local_utc_ts < '2015-09-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-17 00:00:00' AND NEW.local_utc_ts < '2015-09-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-16 00:00:00' AND NEW.local_utc_ts < '2015-09-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-15 00:00:00' AND NEW.local_utc_ts < '2015-09-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_09_15 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2015-09 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_01() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_01_uniq_tracker_ts ON tracker_motion_par_2015_09_01(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_01_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_01(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_01_local_utc_ts ON tracker_motion_par_2015_09_01(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_01 ADD CHECK (local_utc_ts >= '2015-09-01 00:00:00' AND local_utc_ts < '2015-09-02 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_02() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_02_uniq_tracker_ts ON tracker_motion_par_2015_09_02(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_02_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_02(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_02_local_utc_ts ON tracker_motion_par_2015_09_02(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_02 ADD CHECK (local_utc_ts >= '2015-09-02 00:00:00' AND local_utc_ts < '2015-09-03 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_03() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_03_uniq_tracker_ts ON tracker_motion_par_2015_09_03(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_03_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_03(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_03_local_utc_ts ON tracker_motion_par_2015_09_03(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_03 ADD CHECK (local_utc_ts >= '2015-09-03 00:00:00' AND local_utc_ts < '2015-09-04 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_04() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_04_uniq_tracker_ts ON tracker_motion_par_2015_09_04(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_04_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_04(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_04_local_utc_ts ON tracker_motion_par_2015_09_04(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_04 ADD CHECK (local_utc_ts >= '2015-09-04 00:00:00' AND local_utc_ts < '2015-09-05 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_05() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_05_uniq_tracker_ts ON tracker_motion_par_2015_09_05(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_05_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_05(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_05_local_utc_ts ON tracker_motion_par_2015_09_05(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_05 ADD CHECK (local_utc_ts >= '2015-09-05 00:00:00' AND local_utc_ts < '2015-09-06 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_06() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_06_uniq_tracker_ts ON tracker_motion_par_2015_09_06(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_06_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_06(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_06_local_utc_ts ON tracker_motion_par_2015_09_06(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_06 ADD CHECK (local_utc_ts >= '2015-09-06 00:00:00' AND local_utc_ts < '2015-09-07 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_07_uniq_tracker_ts ON tracker_motion_par_2015_09_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_07_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_07_local_utc_ts ON tracker_motion_par_2015_09_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_07 ADD CHECK (local_utc_ts >= '2015-09-07 00:00:00' AND local_utc_ts < '2015-09-08 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_08_uniq_tracker_ts ON tracker_motion_par_2015_09_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_08_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_08_local_utc_ts ON tracker_motion_par_2015_09_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_08 ADD CHECK (local_utc_ts >= '2015-09-08 00:00:00' AND local_utc_ts < '2015-09-09 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_09() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_09_uniq_tracker_ts ON tracker_motion_par_2015_09_09(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_09_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_09(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_09_local_utc_ts ON tracker_motion_par_2015_09_09(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_09 ADD CHECK (local_utc_ts >= '2015-09-09 00:00:00' AND local_utc_ts < '2015-09-10 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_10() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_10_uniq_tracker_ts ON tracker_motion_par_2015_09_10(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_10_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_10(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_10_local_utc_ts ON tracker_motion_par_2015_09_10(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_10 ADD CHECK (local_utc_ts >= '2015-09-10 00:00:00' AND local_utc_ts < '2015-09-11 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_11() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_11_uniq_tracker_ts ON tracker_motion_par_2015_09_11(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_11_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_11(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_11_local_utc_ts ON tracker_motion_par_2015_09_11(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_11 ADD CHECK (local_utc_ts >= '2015-09-11 00:00:00' AND local_utc_ts < '2015-09-12 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_12() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_12_uniq_tracker_ts ON tracker_motion_par_2015_09_12(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_12_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_12(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_12_local_utc_ts ON tracker_motion_par_2015_09_12(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_12 ADD CHECK (local_utc_ts >= '2015-09-12 00:00:00' AND local_utc_ts < '2015-09-13 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_13() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_13_uniq_tracker_ts ON tracker_motion_par_2015_09_13(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_13_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_13(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_13_local_utc_ts ON tracker_motion_par_2015_09_13(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_13 ADD CHECK (local_utc_ts >= '2015-09-13 00:00:00' AND local_utc_ts < '2015-09-14 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_14() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_14_uniq_tracker_ts ON tracker_motion_par_2015_09_14(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_14_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_14(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_14_local_utc_ts ON tracker_motion_par_2015_09_14(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_14 ADD CHECK (local_utc_ts >= '2015-09-14 00:00:00' AND local_utc_ts < '2015-09-15 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_15() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_15_uniq_tracker_ts ON tracker_motion_par_2015_09_15(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_15_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_15(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_15_local_utc_ts ON tracker_motion_par_2015_09_15(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_15 ADD CHECK (local_utc_ts >= '2015-09-15 00:00:00' AND local_utc_ts < '2015-09-16 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_16() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_16_uniq_tracker_ts ON tracker_motion_par_2015_09_16(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_16_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_16(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_16_local_utc_ts ON tracker_motion_par_2015_09_16(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_16 ADD CHECK (local_utc_ts >= '2015-09-16 00:00:00' AND local_utc_ts < '2015-09-17 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_17() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_17_uniq_tracker_ts ON tracker_motion_par_2015_09_17(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_17_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_17(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_17_local_utc_ts ON tracker_motion_par_2015_09_17(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_17 ADD CHECK (local_utc_ts >= '2015-09-17 00:00:00' AND local_utc_ts < '2015-09-18 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_18() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_18_uniq_tracker_ts ON tracker_motion_par_2015_09_18(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_18_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_18(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_18_local_utc_ts ON tracker_motion_par_2015_09_18(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_18 ADD CHECK (local_utc_ts >= '2015-09-18 00:00:00' AND local_utc_ts < '2015-09-19 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_19() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_19_uniq_tracker_ts ON tracker_motion_par_2015_09_19(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_19_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_19(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_19_local_utc_ts ON tracker_motion_par_2015_09_19(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_19 ADD CHECK (local_utc_ts >= '2015-09-19 00:00:00' AND local_utc_ts < '2015-09-20 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_20() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_20_uniq_tracker_ts ON tracker_motion_par_2015_09_20(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_20_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_20(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_20_local_utc_ts ON tracker_motion_par_2015_09_20(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_20 ADD CHECK (local_utc_ts >= '2015-09-20 00:00:00' AND local_utc_ts < '2015-09-21 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_21() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_21_uniq_tracker_ts ON tracker_motion_par_2015_09_21(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_21_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_21(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_21_local_utc_ts ON tracker_motion_par_2015_09_21(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_21 ADD CHECK (local_utc_ts >= '2015-09-21 00:00:00' AND local_utc_ts < '2015-09-22 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_22() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_22_uniq_tracker_ts ON tracker_motion_par_2015_09_22(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_22_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_22(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_22_local_utc_ts ON tracker_motion_par_2015_09_22(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_22 ADD CHECK (local_utc_ts >= '2015-09-22 00:00:00' AND local_utc_ts < '2015-09-23 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_23() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_23_uniq_tracker_ts ON tracker_motion_par_2015_09_23(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_23_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_23(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_23_local_utc_ts ON tracker_motion_par_2015_09_23(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_23 ADD CHECK (local_utc_ts >= '2015-09-23 00:00:00' AND local_utc_ts < '2015-09-24 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_24() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_24_uniq_tracker_ts ON tracker_motion_par_2015_09_24(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_24_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_24(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_24_local_utc_ts ON tracker_motion_par_2015_09_24(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_24 ADD CHECK (local_utc_ts >= '2015-09-24 00:00:00' AND local_utc_ts < '2015-09-25 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_25() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_25_uniq_tracker_ts ON tracker_motion_par_2015_09_25(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_25_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_25(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_25_local_utc_ts ON tracker_motion_par_2015_09_25(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_25 ADD CHECK (local_utc_ts >= '2015-09-25 00:00:00' AND local_utc_ts < '2015-09-26 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_26() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_26_uniq_tracker_ts ON tracker_motion_par_2015_09_26(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_26_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_26(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_26_local_utc_ts ON tracker_motion_par_2015_09_26(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_26 ADD CHECK (local_utc_ts >= '2015-09-26 00:00:00' AND local_utc_ts < '2015-09-27 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_27() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_27_uniq_tracker_ts ON tracker_motion_par_2015_09_27(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_27_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_27(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_27_local_utc_ts ON tracker_motion_par_2015_09_27(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_27 ADD CHECK (local_utc_ts >= '2015-09-27 00:00:00' AND local_utc_ts < '2015-09-28 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_28() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_28_uniq_tracker_ts ON tracker_motion_par_2015_09_28(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_28_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_28(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_28_local_utc_ts ON tracker_motion_par_2015_09_28(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_28 ADD CHECK (local_utc_ts >= '2015-09-28 00:00:00' AND local_utc_ts < '2015-09-29 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_29() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_29_uniq_tracker_ts ON tracker_motion_par_2015_09_29(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_29_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_29(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_29_local_utc_ts ON tracker_motion_par_2015_09_29(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_29 ADD CHECK (local_utc_ts >= '2015-09-29 00:00:00' AND local_utc_ts < '2015-09-30 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_09_30() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_30_uniq_tracker_ts ON tracker_motion_par_2015_09_30(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_09_30_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_09_30(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_09_30_local_utc_ts ON tracker_motion_par_2015_09_30(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_09_30 ADD CHECK (local_utc_ts >= '2015-09-30 00:00:00' AND local_utc_ts < '2015-10-01 00:00:00');




CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-09-30 00:00:00' AND NEW.local_utc_ts < '2015-10-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-29 00:00:00' AND NEW.local_utc_ts < '2015-09-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-28 00:00:00' AND NEW.local_utc_ts < '2015-09-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-27 00:00:00' AND NEW.local_utc_ts < '2015-09-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-26 00:00:00' AND NEW.local_utc_ts < '2015-09-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-25 00:00:00' AND NEW.local_utc_ts < '2015-09-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-24 00:00:00' AND NEW.local_utc_ts < '2015-09-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-23 00:00:00' AND NEW.local_utc_ts < '2015-09-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-22 00:00:00' AND NEW.local_utc_ts < '2015-09-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-21 00:00:00' AND NEW.local_utc_ts < '2015-09-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-20 00:00:00' AND NEW.local_utc_ts < '2015-09-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-19 00:00:00' AND NEW.local_utc_ts < '2015-09-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-18 00:00:00' AND NEW.local_utc_ts < '2015-09-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-17 00:00:00' AND NEW.local_utc_ts < '2015-09-18 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-16 00:00:00' AND NEW.local_utc_ts < '2015-09-17 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-09-15 00:00:00' AND NEW.local_utc_ts < '2015-09-16 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_09_15 VALUES (NEW.*);
 ELSIF NEW.local_utc_ts >= '2015-08-01 00:00:00' AND NEW.local_utc_ts < '2015-09-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-01 00:00:00' AND NEW.local_utc_ts < '2015-08-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_07 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;
