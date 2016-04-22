
-- 2016-04 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_01_uniq_device_id_account_id_ts on device_sensors_par_2016_04_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_01_ts_idx on device_sensors_par_2016_04_01(ts);
ALTER TABLE device_sensors_par_2016_04_01 ADD CHECK (local_utc_ts >= '2016-04-01 00:00:00' AND local_utc_ts < '2016-04-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_02_uniq_device_id_account_id_ts on device_sensors_par_2016_04_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_02_ts_idx on device_sensors_par_2016_04_02(ts);
ALTER TABLE device_sensors_par_2016_04_02 ADD CHECK (local_utc_ts >= '2016-04-02 00:00:00' AND local_utc_ts < '2016-04-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_03_uniq_device_id_account_id_ts on device_sensors_par_2016_04_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_03_ts_idx on device_sensors_par_2016_04_03(ts);
ALTER TABLE device_sensors_par_2016_04_03 ADD CHECK (local_utc_ts >= '2016-04-03 00:00:00' AND local_utc_ts < '2016-04-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_04_uniq_device_id_account_id_ts on device_sensors_par_2016_04_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_04_ts_idx on device_sensors_par_2016_04_04(ts);
ALTER TABLE device_sensors_par_2016_04_04 ADD CHECK (local_utc_ts >= '2016-04-04 00:00:00' AND local_utc_ts < '2016-04-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_05_uniq_device_id_account_id_ts on device_sensors_par_2016_04_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_05_ts_idx on device_sensors_par_2016_04_05(ts);
ALTER TABLE device_sensors_par_2016_04_05 ADD CHECK (local_utc_ts >= '2016-04-05 00:00:00' AND local_utc_ts < '2016-04-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_06_uniq_device_id_account_id_ts on device_sensors_par_2016_04_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_06_ts_idx on device_sensors_par_2016_04_06(ts);
ALTER TABLE device_sensors_par_2016_04_06 ADD CHECK (local_utc_ts >= '2016-04-06 00:00:00' AND local_utc_ts < '2016-04-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_07_uniq_device_id_account_id_ts on device_sensors_par_2016_04_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_07_ts_idx on device_sensors_par_2016_04_07(ts);
ALTER TABLE device_sensors_par_2016_04_07 ADD CHECK (local_utc_ts >= '2016-04-07 00:00:00' AND local_utc_ts < '2016-04-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_08_uniq_device_id_account_id_ts on device_sensors_par_2016_04_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_08_ts_idx on device_sensors_par_2016_04_08(ts);
ALTER TABLE device_sensors_par_2016_04_08 ADD CHECK (local_utc_ts >= '2016-04-08 00:00:00' AND local_utc_ts < '2016-04-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_09_uniq_device_id_account_id_ts on device_sensors_par_2016_04_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_09_ts_idx on device_sensors_par_2016_04_09(ts);
ALTER TABLE device_sensors_par_2016_04_09 ADD CHECK (local_utc_ts >= '2016-04-09 00:00:00' AND local_utc_ts < '2016-04-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_10_uniq_device_id_account_id_ts on device_sensors_par_2016_04_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_10_ts_idx on device_sensors_par_2016_04_10(ts);
ALTER TABLE device_sensors_par_2016_04_10 ADD CHECK (local_utc_ts >= '2016-04-10 00:00:00' AND local_utc_ts < '2016-04-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_11_uniq_device_id_account_id_ts on device_sensors_par_2016_04_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_11_ts_idx on device_sensors_par_2016_04_11(ts);
ALTER TABLE device_sensors_par_2016_04_11 ADD CHECK (local_utc_ts >= '2016-04-11 00:00:00' AND local_utc_ts < '2016-04-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_12_uniq_device_id_account_id_ts on device_sensors_par_2016_04_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_12_ts_idx on device_sensors_par_2016_04_12(ts);
ALTER TABLE device_sensors_par_2016_04_12 ADD CHECK (local_utc_ts >= '2016-04-12 00:00:00' AND local_utc_ts < '2016-04-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_13_uniq_device_id_account_id_ts on device_sensors_par_2016_04_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_13_ts_idx on device_sensors_par_2016_04_13(ts);
ALTER TABLE device_sensors_par_2016_04_13 ADD CHECK (local_utc_ts >= '2016-04-13 00:00:00' AND local_utc_ts < '2016-04-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_14_uniq_device_id_account_id_ts on device_sensors_par_2016_04_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_14_ts_idx on device_sensors_par_2016_04_14(ts);
ALTER TABLE device_sensors_par_2016_04_14 ADD CHECK (local_utc_ts >= '2016-04-14 00:00:00' AND local_utc_ts < '2016-04-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_15_uniq_device_id_account_id_ts on device_sensors_par_2016_04_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_15_ts_idx on device_sensors_par_2016_04_15(ts);
ALTER TABLE device_sensors_par_2016_04_15 ADD CHECK (local_utc_ts >= '2016-04-15 00:00:00' AND local_utc_ts < '2016-04-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_16_uniq_device_id_account_id_ts on device_sensors_par_2016_04_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_16_ts_idx on device_sensors_par_2016_04_16(ts);
ALTER TABLE device_sensors_par_2016_04_16 ADD CHECK (local_utc_ts >= '2016-04-16 00:00:00' AND local_utc_ts < '2016-04-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_17_uniq_device_id_account_id_ts on device_sensors_par_2016_04_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_17_ts_idx on device_sensors_par_2016_04_17(ts);
ALTER TABLE device_sensors_par_2016_04_17 ADD CHECK (local_utc_ts >= '2016-04-17 00:00:00' AND local_utc_ts < '2016-04-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_18_uniq_device_id_account_id_ts on device_sensors_par_2016_04_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_18_ts_idx on device_sensors_par_2016_04_18(ts);
ALTER TABLE device_sensors_par_2016_04_18 ADD CHECK (local_utc_ts >= '2016-04-18 00:00:00' AND local_utc_ts < '2016-04-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_19_uniq_device_id_account_id_ts on device_sensors_par_2016_04_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_19_ts_idx on device_sensors_par_2016_04_19(ts);
ALTER TABLE device_sensors_par_2016_04_19 ADD CHECK (local_utc_ts >= '2016-04-19 00:00:00' AND local_utc_ts < '2016-04-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_20_uniq_device_id_account_id_ts on device_sensors_par_2016_04_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_20_ts_idx on device_sensors_par_2016_04_20(ts);
ALTER TABLE device_sensors_par_2016_04_20 ADD CHECK (local_utc_ts >= '2016-04-20 00:00:00' AND local_utc_ts < '2016-04-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_21_uniq_device_id_account_id_ts on device_sensors_par_2016_04_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_21_ts_idx on device_sensors_par_2016_04_21(ts);
ALTER TABLE device_sensors_par_2016_04_21 ADD CHECK (local_utc_ts >= '2016-04-21 00:00:00' AND local_utc_ts < '2016-04-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_22_uniq_device_id_account_id_ts on device_sensors_par_2016_04_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_22_ts_idx on device_sensors_par_2016_04_22(ts);
ALTER TABLE device_sensors_par_2016_04_22 ADD CHECK (local_utc_ts >= '2016-04-22 00:00:00' AND local_utc_ts < '2016-04-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_23_uniq_device_id_account_id_ts on device_sensors_par_2016_04_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_23_ts_idx on device_sensors_par_2016_04_23(ts);
ALTER TABLE device_sensors_par_2016_04_23 ADD CHECK (local_utc_ts >= '2016-04-23 00:00:00' AND local_utc_ts < '2016-04-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_24_uniq_device_id_account_id_ts on device_sensors_par_2016_04_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_24_ts_idx on device_sensors_par_2016_04_24(ts);
ALTER TABLE device_sensors_par_2016_04_24 ADD CHECK (local_utc_ts >= '2016-04-24 00:00:00' AND local_utc_ts < '2016-04-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_25_uniq_device_id_account_id_ts on device_sensors_par_2016_04_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_25_ts_idx on device_sensors_par_2016_04_25(ts);
ALTER TABLE device_sensors_par_2016_04_25 ADD CHECK (local_utc_ts >= '2016-04-25 00:00:00' AND local_utc_ts < '2016-04-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_26_uniq_device_id_account_id_ts on device_sensors_par_2016_04_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_26_ts_idx on device_sensors_par_2016_04_26(ts);
ALTER TABLE device_sensors_par_2016_04_26 ADD CHECK (local_utc_ts >= '2016-04-26 00:00:00' AND local_utc_ts < '2016-04-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_27_uniq_device_id_account_id_ts on device_sensors_par_2016_04_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_27_ts_idx on device_sensors_par_2016_04_27(ts);
ALTER TABLE device_sensors_par_2016_04_27 ADD CHECK (local_utc_ts >= '2016-04-27 00:00:00' AND local_utc_ts < '2016-04-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_28_uniq_device_id_account_id_ts on device_sensors_par_2016_04_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_28_ts_idx on device_sensors_par_2016_04_28(ts);
ALTER TABLE device_sensors_par_2016_04_28 ADD CHECK (local_utc_ts >= '2016-04-28 00:00:00' AND local_utc_ts < '2016-04-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_29_uniq_device_id_account_id_ts on device_sensors_par_2016_04_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_29_ts_idx on device_sensors_par_2016_04_29(ts);
ALTER TABLE device_sensors_par_2016_04_29 ADD CHECK (local_utc_ts >= '2016-04-29 00:00:00' AND local_utc_ts < '2016-04-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_04_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_04_30_uniq_device_id_account_id_ts on device_sensors_par_2016_04_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_04_30_ts_idx on device_sensors_par_2016_04_30(ts);
ALTER TABLE device_sensors_par_2016_04_30 ADD CHECK (local_utc_ts >= '2016-04-30 00:00:00' AND local_utc_ts < '2016-05-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-04-30 00:00:00' AND NEW.local_utc_ts < '2016-05-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-29 00:00:00' AND NEW.local_utc_ts < '2016-04-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-28 00:00:00' AND NEW.local_utc_ts < '2016-04-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-27 00:00:00' AND NEW.local_utc_ts < '2016-04-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-26 00:00:00' AND NEW.local_utc_ts < '2016-04-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-25 00:00:00' AND NEW.local_utc_ts < '2016-04-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-24 00:00:00' AND NEW.local_utc_ts < '2016-04-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-23 00:00:00' AND NEW.local_utc_ts < '2016-04-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-22 00:00:00' AND NEW.local_utc_ts < '2016-04-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-21 00:00:00' AND NEW.local_utc_ts < '2016-04-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-20 00:00:00' AND NEW.local_utc_ts < '2016-04-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-19 00:00:00' AND NEW.local_utc_ts < '2016-04-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_04_19 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2016-04 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_01() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_01_uniq_tracker_ts ON tracker_motion_par_2016_04_01(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_01_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_01(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_01_local_utc_ts ON tracker_motion_par_2016_04_01(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_01 ADD CHECK (local_utc_ts >= '2016-04-01 00:00:00' AND local_utc_ts < '2016-04-02 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_02() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_02_uniq_tracker_ts ON tracker_motion_par_2016_04_02(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_02_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_02(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_02_local_utc_ts ON tracker_motion_par_2016_04_02(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_02 ADD CHECK (local_utc_ts >= '2016-04-02 00:00:00' AND local_utc_ts < '2016-04-03 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_03() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_03_uniq_tracker_ts ON tracker_motion_par_2016_04_03(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_03_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_03(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_03_local_utc_ts ON tracker_motion_par_2016_04_03(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_03 ADD CHECK (local_utc_ts >= '2016-04-03 00:00:00' AND local_utc_ts < '2016-04-04 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_04() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_04_uniq_tracker_ts ON tracker_motion_par_2016_04_04(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_04_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_04(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_04_local_utc_ts ON tracker_motion_par_2016_04_04(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_04 ADD CHECK (local_utc_ts >= '2016-04-04 00:00:00' AND local_utc_ts < '2016-04-05 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_05() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_05_uniq_tracker_ts ON tracker_motion_par_2016_04_05(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_05_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_05(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_05_local_utc_ts ON tracker_motion_par_2016_04_05(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_05 ADD CHECK (local_utc_ts >= '2016-04-05 00:00:00' AND local_utc_ts < '2016-04-06 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_06() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_06_uniq_tracker_ts ON tracker_motion_par_2016_04_06(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_06_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_06(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_06_local_utc_ts ON tracker_motion_par_2016_04_06(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_06 ADD CHECK (local_utc_ts >= '2016-04-06 00:00:00' AND local_utc_ts < '2016-04-07 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_07_uniq_tracker_ts ON tracker_motion_par_2016_04_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_07_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_07_local_utc_ts ON tracker_motion_par_2016_04_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_07 ADD CHECK (local_utc_ts >= '2016-04-07 00:00:00' AND local_utc_ts < '2016-04-08 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_08_uniq_tracker_ts ON tracker_motion_par_2016_04_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_08_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_08_local_utc_ts ON tracker_motion_par_2016_04_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_08 ADD CHECK (local_utc_ts >= '2016-04-08 00:00:00' AND local_utc_ts < '2016-04-09 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_09() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_09_uniq_tracker_ts ON tracker_motion_par_2016_04_09(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_09_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_09(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_09_local_utc_ts ON tracker_motion_par_2016_04_09(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_09 ADD CHECK (local_utc_ts >= '2016-04-09 00:00:00' AND local_utc_ts < '2016-04-10 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_10() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_10_uniq_tracker_ts ON tracker_motion_par_2016_04_10(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_10_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_10(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_10_local_utc_ts ON tracker_motion_par_2016_04_10(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_10 ADD CHECK (local_utc_ts >= '2016-04-10 00:00:00' AND local_utc_ts < '2016-04-11 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_11() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_11_uniq_tracker_ts ON tracker_motion_par_2016_04_11(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_11_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_11(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_11_local_utc_ts ON tracker_motion_par_2016_04_11(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_11 ADD CHECK (local_utc_ts >= '2016-04-11 00:00:00' AND local_utc_ts < '2016-04-12 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_12() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_12_uniq_tracker_ts ON tracker_motion_par_2016_04_12(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_12_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_12(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_12_local_utc_ts ON tracker_motion_par_2016_04_12(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_12 ADD CHECK (local_utc_ts >= '2016-04-12 00:00:00' AND local_utc_ts < '2016-04-13 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_13() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_13_uniq_tracker_ts ON tracker_motion_par_2016_04_13(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_13_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_13(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_13_local_utc_ts ON tracker_motion_par_2016_04_13(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_13 ADD CHECK (local_utc_ts >= '2016-04-13 00:00:00' AND local_utc_ts < '2016-04-14 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_14() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_14_uniq_tracker_ts ON tracker_motion_par_2016_04_14(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_14_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_14(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_14_local_utc_ts ON tracker_motion_par_2016_04_14(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_14 ADD CHECK (local_utc_ts >= '2016-04-14 00:00:00' AND local_utc_ts < '2016-04-15 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_15() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_15_uniq_tracker_ts ON tracker_motion_par_2016_04_15(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_15_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_15(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_15_local_utc_ts ON tracker_motion_par_2016_04_15(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_15 ADD CHECK (local_utc_ts >= '2016-04-15 00:00:00' AND local_utc_ts < '2016-04-16 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_16() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_16_uniq_tracker_ts ON tracker_motion_par_2016_04_16(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_16_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_16(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_16_local_utc_ts ON tracker_motion_par_2016_04_16(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_16 ADD CHECK (local_utc_ts >= '2016-04-16 00:00:00' AND local_utc_ts < '2016-04-17 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_17() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_17_uniq_tracker_ts ON tracker_motion_par_2016_04_17(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_17_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_17(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_17_local_utc_ts ON tracker_motion_par_2016_04_17(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_17 ADD CHECK (local_utc_ts >= '2016-04-17 00:00:00' AND local_utc_ts < '2016-04-18 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_18() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_18_uniq_tracker_ts ON tracker_motion_par_2016_04_18(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_18_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_18(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_18_local_utc_ts ON tracker_motion_par_2016_04_18(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_18 ADD CHECK (local_utc_ts >= '2016-04-18 00:00:00' AND local_utc_ts < '2016-04-19 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_19() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_19_uniq_tracker_ts ON tracker_motion_par_2016_04_19(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_19_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_19(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_19_local_utc_ts ON tracker_motion_par_2016_04_19(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_19 ADD CHECK (local_utc_ts >= '2016-04-19 00:00:00' AND local_utc_ts < '2016-04-20 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_20() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_20_uniq_tracker_ts ON tracker_motion_par_2016_04_20(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_20_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_20(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_20_local_utc_ts ON tracker_motion_par_2016_04_20(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_20 ADD CHECK (local_utc_ts >= '2016-04-20 00:00:00' AND local_utc_ts < '2016-04-21 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_21() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_21_uniq_tracker_ts ON tracker_motion_par_2016_04_21(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_21_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_21(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_21_local_utc_ts ON tracker_motion_par_2016_04_21(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_21 ADD CHECK (local_utc_ts >= '2016-04-21 00:00:00' AND local_utc_ts < '2016-04-22 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_22() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_22_uniq_tracker_ts ON tracker_motion_par_2016_04_22(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_22_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_22(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_22_local_utc_ts ON tracker_motion_par_2016_04_22(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_22 ADD CHECK (local_utc_ts >= '2016-04-22 00:00:00' AND local_utc_ts < '2016-04-23 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_23() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_23_uniq_tracker_ts ON tracker_motion_par_2016_04_23(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_23_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_23(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_23_local_utc_ts ON tracker_motion_par_2016_04_23(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_23 ADD CHECK (local_utc_ts >= '2016-04-23 00:00:00' AND local_utc_ts < '2016-04-24 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_24() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_24_uniq_tracker_ts ON tracker_motion_par_2016_04_24(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_24_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_24(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_24_local_utc_ts ON tracker_motion_par_2016_04_24(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_24 ADD CHECK (local_utc_ts >= '2016-04-24 00:00:00' AND local_utc_ts < '2016-04-25 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_25() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_25_uniq_tracker_ts ON tracker_motion_par_2016_04_25(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_25_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_25(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_25_local_utc_ts ON tracker_motion_par_2016_04_25(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_25 ADD CHECK (local_utc_ts >= '2016-04-25 00:00:00' AND local_utc_ts < '2016-04-26 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_26() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_26_uniq_tracker_ts ON tracker_motion_par_2016_04_26(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_26_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_26(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_26_local_utc_ts ON tracker_motion_par_2016_04_26(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_26 ADD CHECK (local_utc_ts >= '2016-04-26 00:00:00' AND local_utc_ts < '2016-04-27 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_27() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_27_uniq_tracker_ts ON tracker_motion_par_2016_04_27(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_27_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_27(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_27_local_utc_ts ON tracker_motion_par_2016_04_27(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_27 ADD CHECK (local_utc_ts >= '2016-04-27 00:00:00' AND local_utc_ts < '2016-04-28 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_28() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_28_uniq_tracker_ts ON tracker_motion_par_2016_04_28(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_28_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_28(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_28_local_utc_ts ON tracker_motion_par_2016_04_28(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_28 ADD CHECK (local_utc_ts >= '2016-04-28 00:00:00' AND local_utc_ts < '2016-04-29 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_29() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_29_uniq_tracker_ts ON tracker_motion_par_2016_04_29(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_29_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_29(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_29_local_utc_ts ON tracker_motion_par_2016_04_29(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_29 ADD CHECK (local_utc_ts >= '2016-04-29 00:00:00' AND local_utc_ts < '2016-04-30 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_04_30() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_30_uniq_tracker_ts ON tracker_motion_par_2016_04_30(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_04_30_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_04_30(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_04_30_local_utc_ts ON tracker_motion_par_2016_04_30(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_04_30 ADD CHECK (local_utc_ts >= '2016-04-30 00:00:00' AND local_utc_ts < '2016-05-01 00:00:00');




CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-04-30 00:00:00' AND NEW.local_utc_ts < '2016-05-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-29 00:00:00' AND NEW.local_utc_ts < '2016-04-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-28 00:00:00' AND NEW.local_utc_ts < '2016-04-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-27 00:00:00' AND NEW.local_utc_ts < '2016-04-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-26 00:00:00' AND NEW.local_utc_ts < '2016-04-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-25 00:00:00' AND NEW.local_utc_ts < '2016-04-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-24 00:00:00' AND NEW.local_utc_ts < '2016-04-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-23 00:00:00' AND NEW.local_utc_ts < '2016-04-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-22 00:00:00' AND NEW.local_utc_ts < '2016-04-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-21 00:00:00' AND NEW.local_utc_ts < '2016-04-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-20 00:00:00' AND NEW.local_utc_ts < '2016-04-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-19 00:00:00' AND NEW.local_utc_ts < '2016-04-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_04_19 VALUES (NEW.*);
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
