
-- 2015-11 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_01_uniq_device_id_account_id_ts on device_sensors_par_2015_11_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_01_ts_idx on device_sensors_par_2015_11_01(ts);
ALTER TABLE device_sensors_par_2015_11_01 ADD CHECK (local_utc_ts >= '2015-11-01 00:00:00' AND local_utc_ts < '2015-11-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_02_uniq_device_id_account_id_ts on device_sensors_par_2015_11_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_02_ts_idx on device_sensors_par_2015_11_02(ts);
ALTER TABLE device_sensors_par_2015_11_02 ADD CHECK (local_utc_ts >= '2015-11-02 00:00:00' AND local_utc_ts < '2015-11-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_03_uniq_device_id_account_id_ts on device_sensors_par_2015_11_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_03_ts_idx on device_sensors_par_2015_11_03(ts);
ALTER TABLE device_sensors_par_2015_11_03 ADD CHECK (local_utc_ts >= '2015-11-03 00:00:00' AND local_utc_ts < '2015-11-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_04_uniq_device_id_account_id_ts on device_sensors_par_2015_11_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_04_ts_idx on device_sensors_par_2015_11_04(ts);
ALTER TABLE device_sensors_par_2015_11_04 ADD CHECK (local_utc_ts >= '2015-11-04 00:00:00' AND local_utc_ts < '2015-11-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_05_uniq_device_id_account_id_ts on device_sensors_par_2015_11_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_05_ts_idx on device_sensors_par_2015_11_05(ts);
ALTER TABLE device_sensors_par_2015_11_05 ADD CHECK (local_utc_ts >= '2015-11-05 00:00:00' AND local_utc_ts < '2015-11-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_06_uniq_device_id_account_id_ts on device_sensors_par_2015_11_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_06_ts_idx on device_sensors_par_2015_11_06(ts);
ALTER TABLE device_sensors_par_2015_11_06 ADD CHECK (local_utc_ts >= '2015-11-06 00:00:00' AND local_utc_ts < '2015-11-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_07_uniq_device_id_account_id_ts on device_sensors_par_2015_11_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_07_ts_idx on device_sensors_par_2015_11_07(ts);
ALTER TABLE device_sensors_par_2015_11_07 ADD CHECK (local_utc_ts >= '2015-11-07 00:00:00' AND local_utc_ts < '2015-11-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_08_uniq_device_id_account_id_ts on device_sensors_par_2015_11_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_08_ts_idx on device_sensors_par_2015_11_08(ts);
ALTER TABLE device_sensors_par_2015_11_08 ADD CHECK (local_utc_ts >= '2015-11-08 00:00:00' AND local_utc_ts < '2015-11-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_09_uniq_device_id_account_id_ts on device_sensors_par_2015_11_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_09_ts_idx on device_sensors_par_2015_11_09(ts);
ALTER TABLE device_sensors_par_2015_11_09 ADD CHECK (local_utc_ts >= '2015-11-09 00:00:00' AND local_utc_ts < '2015-11-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_10_uniq_device_id_account_id_ts on device_sensors_par_2015_11_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_10_ts_idx on device_sensors_par_2015_11_10(ts);
ALTER TABLE device_sensors_par_2015_11_10 ADD CHECK (local_utc_ts >= '2015-11-10 00:00:00' AND local_utc_ts < '2015-11-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_11_uniq_device_id_account_id_ts on device_sensors_par_2015_11_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_11_ts_idx on device_sensors_par_2015_11_11(ts);
ALTER TABLE device_sensors_par_2015_11_11 ADD CHECK (local_utc_ts >= '2015-11-11 00:00:00' AND local_utc_ts < '2015-11-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_12_uniq_device_id_account_id_ts on device_sensors_par_2015_11_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_12_ts_idx on device_sensors_par_2015_11_12(ts);
ALTER TABLE device_sensors_par_2015_11_12 ADD CHECK (local_utc_ts >= '2015-11-12 00:00:00' AND local_utc_ts < '2015-11-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_13_uniq_device_id_account_id_ts on device_sensors_par_2015_11_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_13_ts_idx on device_sensors_par_2015_11_13(ts);
ALTER TABLE device_sensors_par_2015_11_13 ADD CHECK (local_utc_ts >= '2015-11-13 00:00:00' AND local_utc_ts < '2015-11-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_14_uniq_device_id_account_id_ts on device_sensors_par_2015_11_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_14_ts_idx on device_sensors_par_2015_11_14(ts);
ALTER TABLE device_sensors_par_2015_11_14 ADD CHECK (local_utc_ts >= '2015-11-14 00:00:00' AND local_utc_ts < '2015-11-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_15_uniq_device_id_account_id_ts on device_sensors_par_2015_11_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_15_ts_idx on device_sensors_par_2015_11_15(ts);
ALTER TABLE device_sensors_par_2015_11_15 ADD CHECK (local_utc_ts >= '2015-11-15 00:00:00' AND local_utc_ts < '2015-11-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_16_uniq_device_id_account_id_ts on device_sensors_par_2015_11_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_16_ts_idx on device_sensors_par_2015_11_16(ts);
ALTER TABLE device_sensors_par_2015_11_16 ADD CHECK (local_utc_ts >= '2015-11-16 00:00:00' AND local_utc_ts < '2015-11-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_17_uniq_device_id_account_id_ts on device_sensors_par_2015_11_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_17_ts_idx on device_sensors_par_2015_11_17(ts);
ALTER TABLE device_sensors_par_2015_11_17 ADD CHECK (local_utc_ts >= '2015-11-17 00:00:00' AND local_utc_ts < '2015-11-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_18_uniq_device_id_account_id_ts on device_sensors_par_2015_11_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_18_ts_idx on device_sensors_par_2015_11_18(ts);
ALTER TABLE device_sensors_par_2015_11_18 ADD CHECK (local_utc_ts >= '2015-11-18 00:00:00' AND local_utc_ts < '2015-11-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_19_uniq_device_id_account_id_ts on device_sensors_par_2015_11_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_19_ts_idx on device_sensors_par_2015_11_19(ts);
ALTER TABLE device_sensors_par_2015_11_19 ADD CHECK (local_utc_ts >= '2015-11-19 00:00:00' AND local_utc_ts < '2015-11-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_20_uniq_device_id_account_id_ts on device_sensors_par_2015_11_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_20_ts_idx on device_sensors_par_2015_11_20(ts);
ALTER TABLE device_sensors_par_2015_11_20 ADD CHECK (local_utc_ts >= '2015-11-20 00:00:00' AND local_utc_ts < '2015-11-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_21_uniq_device_id_account_id_ts on device_sensors_par_2015_11_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_21_ts_idx on device_sensors_par_2015_11_21(ts);
ALTER TABLE device_sensors_par_2015_11_21 ADD CHECK (local_utc_ts >= '2015-11-21 00:00:00' AND local_utc_ts < '2015-11-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_22_uniq_device_id_account_id_ts on device_sensors_par_2015_11_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_22_ts_idx on device_sensors_par_2015_11_22(ts);
ALTER TABLE device_sensors_par_2015_11_22 ADD CHECK (local_utc_ts >= '2015-11-22 00:00:00' AND local_utc_ts < '2015-11-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_23_uniq_device_id_account_id_ts on device_sensors_par_2015_11_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_23_ts_idx on device_sensors_par_2015_11_23(ts);
ALTER TABLE device_sensors_par_2015_11_23 ADD CHECK (local_utc_ts >= '2015-11-23 00:00:00' AND local_utc_ts < '2015-11-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_24_uniq_device_id_account_id_ts on device_sensors_par_2015_11_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_24_ts_idx on device_sensors_par_2015_11_24(ts);
ALTER TABLE device_sensors_par_2015_11_24 ADD CHECK (local_utc_ts >= '2015-11-24 00:00:00' AND local_utc_ts < '2015-11-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_25_uniq_device_id_account_id_ts on device_sensors_par_2015_11_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_25_ts_idx on device_sensors_par_2015_11_25(ts);
ALTER TABLE device_sensors_par_2015_11_25 ADD CHECK (local_utc_ts >= '2015-11-25 00:00:00' AND local_utc_ts < '2015-11-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_26_uniq_device_id_account_id_ts on device_sensors_par_2015_11_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_26_ts_idx on device_sensors_par_2015_11_26(ts);
ALTER TABLE device_sensors_par_2015_11_26 ADD CHECK (local_utc_ts >= '2015-11-26 00:00:00' AND local_utc_ts < '2015-11-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_27_uniq_device_id_account_id_ts on device_sensors_par_2015_11_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_27_ts_idx on device_sensors_par_2015_11_27(ts);
ALTER TABLE device_sensors_par_2015_11_27 ADD CHECK (local_utc_ts >= '2015-11-27 00:00:00' AND local_utc_ts < '2015-11-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_28_uniq_device_id_account_id_ts on device_sensors_par_2015_11_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_28_ts_idx on device_sensors_par_2015_11_28(ts);
ALTER TABLE device_sensors_par_2015_11_28 ADD CHECK (local_utc_ts >= '2015-11-28 00:00:00' AND local_utc_ts < '2015-11-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_29_uniq_device_id_account_id_ts on device_sensors_par_2015_11_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_29_ts_idx on device_sensors_par_2015_11_29(ts);
ALTER TABLE device_sensors_par_2015_11_29 ADD CHECK (local_utc_ts >= '2015-11-29 00:00:00' AND local_utc_ts < '2015-11-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_11_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_11_30_uniq_device_id_account_id_ts on device_sensors_par_2015_11_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_11_30_ts_idx on device_sensors_par_2015_11_30(ts);
ALTER TABLE device_sensors_par_2015_11_30 ADD CHECK (local_utc_ts >= '2015-11-30 00:00:00' AND local_utc_ts < '2015-12-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-11-30 00:00:00' AND NEW.local_utc_ts < '2015-12-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-29 00:00:00' AND NEW.local_utc_ts < '2015-11-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-28 00:00:00' AND NEW.local_utc_ts < '2015-11-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-27 00:00:00' AND NEW.local_utc_ts < '2015-11-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-26 00:00:00' AND NEW.local_utc_ts < '2015-11-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-25 00:00:00' AND NEW.local_utc_ts < '2015-11-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-24 00:00:00' AND NEW.local_utc_ts < '2015-11-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-23 00:00:00' AND NEW.local_utc_ts < '2015-11-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-22 00:00:00' AND NEW.local_utc_ts < '2015-11-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-21 00:00:00' AND NEW.local_utc_ts < '2015-11-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-20 00:00:00' AND NEW.local_utc_ts < '2015-11-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-19 00:00:00' AND NEW.local_utc_ts < '2015-11-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-18 00:00:00' AND NEW.local_utc_ts < '2015-11-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-17 00:00:00' AND NEW.local_utc_ts < '2015-11-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-16 00:00:00' AND NEW.local_utc_ts < '2015-11-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-15 00:00:00' AND NEW.local_utc_ts < '2015-11-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-14 00:00:00' AND NEW.local_utc_ts < '2015-11-15 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-13 00:00:00' AND NEW.local_utc_ts < '2015-11-14 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-12 00:00:00' AND NEW.local_utc_ts < '2015-11-13 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-11 00:00:00' AND NEW.local_utc_ts < '2015-11-12 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-10 00:00:00' AND NEW.local_utc_ts < '2015-11-11 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-09 00:00:00' AND NEW.local_utc_ts < '2015-11-10 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-08 00:00:00' AND NEW.local_utc_ts < '2015-11-09 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-07 00:00:00' AND NEW.local_utc_ts < '2015-11-08 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-06 00:00:00' AND NEW.local_utc_ts < '2015-11-07 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-05 00:00:00' AND NEW.local_utc_ts < '2015-11-06 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-04 00:00:00' AND NEW.local_utc_ts < '2015-11-05 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-03 00:00:00' AND NEW.local_utc_ts < '2015-11-04 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-02 00:00:00' AND NEW.local_utc_ts < '2015-11-03 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-01 00:00:00' AND NEW.local_utc_ts < '2015-11-02 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_11_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-31 00:00:00' AND NEW.local_utc_ts < '2015-11-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-30 00:00:00' AND NEW.local_utc_ts < '2015-10-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-29 00:00:00' AND NEW.local_utc_ts < '2015-10-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-28 00:00:00' AND NEW.local_utc_ts < '2015-10-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-27 00:00:00' AND NEW.local_utc_ts < '2015-10-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-26 00:00:00' AND NEW.local_utc_ts < '2015-10-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-25 00:00:00' AND NEW.local_utc_ts < '2015-10-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-24 00:00:00' AND NEW.local_utc_ts < '2015-10-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-23 00:00:00' AND NEW.local_utc_ts < '2015-10-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-22 00:00:00' AND NEW.local_utc_ts < '2015-10-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-21 00:00:00' AND NEW.local_utc_ts < '2015-10-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-20 00:00:00' AND NEW.local_utc_ts < '2015-10-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-19 00:00:00' AND NEW.local_utc_ts < '2015-10-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-18 00:00:00' AND NEW.local_utc_ts < '2015-10-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-17 00:00:00' AND NEW.local_utc_ts < '2015-10-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_10_17 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2015-11 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_01() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_01_uniq_tracker_ts ON tracker_motion_par_2015_11_01(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_01_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_01(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_01_local_utc_ts ON tracker_motion_par_2015_11_01(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_01 ADD CHECK (local_utc_ts >= '2015-11-01 00:00:00' AND local_utc_ts < '2015-11-02 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_02() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_02_uniq_tracker_ts ON tracker_motion_par_2015_11_02(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_02_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_02(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_02_local_utc_ts ON tracker_motion_par_2015_11_02(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_02 ADD CHECK (local_utc_ts >= '2015-11-02 00:00:00' AND local_utc_ts < '2015-11-03 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_03() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_03_uniq_tracker_ts ON tracker_motion_par_2015_11_03(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_03_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_03(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_03_local_utc_ts ON tracker_motion_par_2015_11_03(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_03 ADD CHECK (local_utc_ts >= '2015-11-03 00:00:00' AND local_utc_ts < '2015-11-04 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_04() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_04_uniq_tracker_ts ON tracker_motion_par_2015_11_04(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_04_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_04(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_04_local_utc_ts ON tracker_motion_par_2015_11_04(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_04 ADD CHECK (local_utc_ts >= '2015-11-04 00:00:00' AND local_utc_ts < '2015-11-05 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_05() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_05_uniq_tracker_ts ON tracker_motion_par_2015_11_05(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_05_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_05(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_05_local_utc_ts ON tracker_motion_par_2015_11_05(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_05 ADD CHECK (local_utc_ts >= '2015-11-05 00:00:00' AND local_utc_ts < '2015-11-06 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_06() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_06_uniq_tracker_ts ON tracker_motion_par_2015_11_06(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_06_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_06(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_06_local_utc_ts ON tracker_motion_par_2015_11_06(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_06 ADD CHECK (local_utc_ts >= '2015-11-06 00:00:00' AND local_utc_ts < '2015-11-07 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_07_uniq_tracker_ts ON tracker_motion_par_2015_11_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_07_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_07_local_utc_ts ON tracker_motion_par_2015_11_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_07 ADD CHECK (local_utc_ts >= '2015-11-07 00:00:00' AND local_utc_ts < '2015-11-08 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_08_uniq_tracker_ts ON tracker_motion_par_2015_11_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_08_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_08_local_utc_ts ON tracker_motion_par_2015_11_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_08 ADD CHECK (local_utc_ts >= '2015-11-08 00:00:00' AND local_utc_ts < '2015-11-09 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_09() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_09_uniq_tracker_ts ON tracker_motion_par_2015_11_09(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_09_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_09(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_09_local_utc_ts ON tracker_motion_par_2015_11_09(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_09 ADD CHECK (local_utc_ts >= '2015-11-09 00:00:00' AND local_utc_ts < '2015-11-10 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_10() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_10_uniq_tracker_ts ON tracker_motion_par_2015_11_10(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_10_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_10(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_10_local_utc_ts ON tracker_motion_par_2015_11_10(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_10 ADD CHECK (local_utc_ts >= '2015-11-10 00:00:00' AND local_utc_ts < '2015-11-11 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_11() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_11_uniq_tracker_ts ON tracker_motion_par_2015_11_11(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_11_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_11(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_11_local_utc_ts ON tracker_motion_par_2015_11_11(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_11 ADD CHECK (local_utc_ts >= '2015-11-11 00:00:00' AND local_utc_ts < '2015-11-12 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_12() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_12_uniq_tracker_ts ON tracker_motion_par_2015_11_12(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_12_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_12(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_12_local_utc_ts ON tracker_motion_par_2015_11_12(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_12 ADD CHECK (local_utc_ts >= '2015-11-12 00:00:00' AND local_utc_ts < '2015-11-13 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_13() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_13_uniq_tracker_ts ON tracker_motion_par_2015_11_13(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_13_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_13(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_13_local_utc_ts ON tracker_motion_par_2015_11_13(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_13 ADD CHECK (local_utc_ts >= '2015-11-13 00:00:00' AND local_utc_ts < '2015-11-14 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_14() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_14_uniq_tracker_ts ON tracker_motion_par_2015_11_14(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_14_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_14(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_14_local_utc_ts ON tracker_motion_par_2015_11_14(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_14 ADD CHECK (local_utc_ts >= '2015-11-14 00:00:00' AND local_utc_ts < '2015-11-15 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_15() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_15_uniq_tracker_ts ON tracker_motion_par_2015_11_15(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_15_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_15(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_15_local_utc_ts ON tracker_motion_par_2015_11_15(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_15 ADD CHECK (local_utc_ts >= '2015-11-15 00:00:00' AND local_utc_ts < '2015-11-16 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_16() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_16_uniq_tracker_ts ON tracker_motion_par_2015_11_16(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_16_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_16(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_16_local_utc_ts ON tracker_motion_par_2015_11_16(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_16 ADD CHECK (local_utc_ts >= '2015-11-16 00:00:00' AND local_utc_ts < '2015-11-17 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_17() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_17_uniq_tracker_ts ON tracker_motion_par_2015_11_17(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_17_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_17(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_17_local_utc_ts ON tracker_motion_par_2015_11_17(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_17 ADD CHECK (local_utc_ts >= '2015-11-17 00:00:00' AND local_utc_ts < '2015-11-18 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_18() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_18_uniq_tracker_ts ON tracker_motion_par_2015_11_18(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_18_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_18(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_18_local_utc_ts ON tracker_motion_par_2015_11_18(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_18 ADD CHECK (local_utc_ts >= '2015-11-18 00:00:00' AND local_utc_ts < '2015-11-19 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_19() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_19_uniq_tracker_ts ON tracker_motion_par_2015_11_19(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_19_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_19(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_19_local_utc_ts ON tracker_motion_par_2015_11_19(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_19 ADD CHECK (local_utc_ts >= '2015-11-19 00:00:00' AND local_utc_ts < '2015-11-20 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_20() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_20_uniq_tracker_ts ON tracker_motion_par_2015_11_20(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_20_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_20(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_20_local_utc_ts ON tracker_motion_par_2015_11_20(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_20 ADD CHECK (local_utc_ts >= '2015-11-20 00:00:00' AND local_utc_ts < '2015-11-21 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_21() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_21_uniq_tracker_ts ON tracker_motion_par_2015_11_21(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_21_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_21(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_21_local_utc_ts ON tracker_motion_par_2015_11_21(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_21 ADD CHECK (local_utc_ts >= '2015-11-21 00:00:00' AND local_utc_ts < '2015-11-22 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_22() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_22_uniq_tracker_ts ON tracker_motion_par_2015_11_22(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_22_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_22(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_22_local_utc_ts ON tracker_motion_par_2015_11_22(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_22 ADD CHECK (local_utc_ts >= '2015-11-22 00:00:00' AND local_utc_ts < '2015-11-23 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_23() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_23_uniq_tracker_ts ON tracker_motion_par_2015_11_23(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_23_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_23(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_23_local_utc_ts ON tracker_motion_par_2015_11_23(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_23 ADD CHECK (local_utc_ts >= '2015-11-23 00:00:00' AND local_utc_ts < '2015-11-24 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_24() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_24_uniq_tracker_ts ON tracker_motion_par_2015_11_24(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_24_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_24(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_24_local_utc_ts ON tracker_motion_par_2015_11_24(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_24 ADD CHECK (local_utc_ts >= '2015-11-24 00:00:00' AND local_utc_ts < '2015-11-25 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_25() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_25_uniq_tracker_ts ON tracker_motion_par_2015_11_25(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_25_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_25(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_25_local_utc_ts ON tracker_motion_par_2015_11_25(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_25 ADD CHECK (local_utc_ts >= '2015-11-25 00:00:00' AND local_utc_ts < '2015-11-26 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_26() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_26_uniq_tracker_ts ON tracker_motion_par_2015_11_26(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_26_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_26(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_26_local_utc_ts ON tracker_motion_par_2015_11_26(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_26 ADD CHECK (local_utc_ts >= '2015-11-26 00:00:00' AND local_utc_ts < '2015-11-27 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_27() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_27_uniq_tracker_ts ON tracker_motion_par_2015_11_27(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_27_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_27(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_27_local_utc_ts ON tracker_motion_par_2015_11_27(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_27 ADD CHECK (local_utc_ts >= '2015-11-27 00:00:00' AND local_utc_ts < '2015-11-28 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_28() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_28_uniq_tracker_ts ON tracker_motion_par_2015_11_28(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_28_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_28(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_28_local_utc_ts ON tracker_motion_par_2015_11_28(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_28 ADD CHECK (local_utc_ts >= '2015-11-28 00:00:00' AND local_utc_ts < '2015-11-29 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_29() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_29_uniq_tracker_ts ON tracker_motion_par_2015_11_29(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_29_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_29(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_29_local_utc_ts ON tracker_motion_par_2015_11_29(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_29 ADD CHECK (local_utc_ts >= '2015-11-29 00:00:00' AND local_utc_ts < '2015-11-30 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_11_30() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_30_uniq_tracker_ts ON tracker_motion_par_2015_11_30(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_11_30_uniq_tracker_id_account_id_ts ON tracker_motion_par_2015_11_30(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_11_30_local_utc_ts ON tracker_motion_par_2015_11_30(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_11_30 ADD CHECK (local_utc_ts >= '2015-11-30 00:00:00' AND local_utc_ts < '2015-12-01 00:00:00');




CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-11-30 00:00:00' AND NEW.local_utc_ts < '2015-12-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-29 00:00:00' AND NEW.local_utc_ts < '2015-11-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-28 00:00:00' AND NEW.local_utc_ts < '2015-11-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-27 00:00:00' AND NEW.local_utc_ts < '2015-11-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-26 00:00:00' AND NEW.local_utc_ts < '2015-11-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-25 00:00:00' AND NEW.local_utc_ts < '2015-11-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-24 00:00:00' AND NEW.local_utc_ts < '2015-11-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-23 00:00:00' AND NEW.local_utc_ts < '2015-11-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-22 00:00:00' AND NEW.local_utc_ts < '2015-11-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-21 00:00:00' AND NEW.local_utc_ts < '2015-11-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-20 00:00:00' AND NEW.local_utc_ts < '2015-11-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-19 00:00:00' AND NEW.local_utc_ts < '2015-11-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-18 00:00:00' AND NEW.local_utc_ts < '2015-11-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-17 00:00:00' AND NEW.local_utc_ts < '2015-11-18 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-16 00:00:00' AND NEW.local_utc_ts < '2015-11-17 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-15 00:00:00' AND NEW.local_utc_ts < '2015-11-16 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-14 00:00:00' AND NEW.local_utc_ts < '2015-11-15 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-13 00:00:00' AND NEW.local_utc_ts < '2015-11-14 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-12 00:00:00' AND NEW.local_utc_ts < '2015-11-13 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-11 00:00:00' AND NEW.local_utc_ts < '2015-11-12 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-10 00:00:00' AND NEW.local_utc_ts < '2015-11-11 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-09 00:00:00' AND NEW.local_utc_ts < '2015-11-10 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-08 00:00:00' AND NEW.local_utc_ts < '2015-11-09 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-07 00:00:00' AND NEW.local_utc_ts < '2015-11-08 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-06 00:00:00' AND NEW.local_utc_ts < '2015-11-07 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-05 00:00:00' AND NEW.local_utc_ts < '2015-11-06 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-04 00:00:00' AND NEW.local_utc_ts < '2015-11-05 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-03 00:00:00' AND NEW.local_utc_ts < '2015-11-04 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-02 00:00:00' AND NEW.local_utc_ts < '2015-11-03 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-11-01 00:00:00' AND NEW.local_utc_ts < '2015-11-02 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_11_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-31 00:00:00' AND NEW.local_utc_ts < '2015-11-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-30 00:00:00' AND NEW.local_utc_ts < '2015-10-31 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-29 00:00:00' AND NEW.local_utc_ts < '2015-10-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-28 00:00:00' AND NEW.local_utc_ts < '2015-10-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-27 00:00:00' AND NEW.local_utc_ts < '2015-10-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-26 00:00:00' AND NEW.local_utc_ts < '2015-10-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-25 00:00:00' AND NEW.local_utc_ts < '2015-10-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-24 00:00:00' AND NEW.local_utc_ts < '2015-10-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-23 00:00:00' AND NEW.local_utc_ts < '2015-10-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-22 00:00:00' AND NEW.local_utc_ts < '2015-10-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-21 00:00:00' AND NEW.local_utc_ts < '2015-10-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-20 00:00:00' AND NEW.local_utc_ts < '2015-10-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-19 00:00:00' AND NEW.local_utc_ts < '2015-10-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-18 00:00:00' AND NEW.local_utc_ts < '2015-10-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-10-17 00:00:00' AND NEW.local_utc_ts < '2015-10-18 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_10_17 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;
