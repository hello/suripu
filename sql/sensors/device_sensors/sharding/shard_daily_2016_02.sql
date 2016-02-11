
-- 2016-02 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_01_uniq_device_id_account_id_ts on device_sensors_par_2016_02_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_01_ts_idx on device_sensors_par_2016_02_01(ts);
ALTER TABLE device_sensors_par_2016_02_01 ADD CHECK (local_utc_ts >= '2016-02-01 00:00:00' AND local_utc_ts < '2016-02-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_02_uniq_device_id_account_id_ts on device_sensors_par_2016_02_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_02_ts_idx on device_sensors_par_2016_02_02(ts);
ALTER TABLE device_sensors_par_2016_02_02 ADD CHECK (local_utc_ts >= '2016-02-02 00:00:00' AND local_utc_ts < '2016-02-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_03_uniq_device_id_account_id_ts on device_sensors_par_2016_02_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_03_ts_idx on device_sensors_par_2016_02_03(ts);
ALTER TABLE device_sensors_par_2016_02_03 ADD CHECK (local_utc_ts >= '2016-02-03 00:00:00' AND local_utc_ts < '2016-02-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_04_uniq_device_id_account_id_ts on device_sensors_par_2016_02_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_04_ts_idx on device_sensors_par_2016_02_04(ts);
ALTER TABLE device_sensors_par_2016_02_04 ADD CHECK (local_utc_ts >= '2016-02-04 00:00:00' AND local_utc_ts < '2016-02-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_05_uniq_device_id_account_id_ts on device_sensors_par_2016_02_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_05_ts_idx on device_sensors_par_2016_02_05(ts);
ALTER TABLE device_sensors_par_2016_02_05 ADD CHECK (local_utc_ts >= '2016-02-05 00:00:00' AND local_utc_ts < '2016-02-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_06_uniq_device_id_account_id_ts on device_sensors_par_2016_02_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_06_ts_idx on device_sensors_par_2016_02_06(ts);
ALTER TABLE device_sensors_par_2016_02_06 ADD CHECK (local_utc_ts >= '2016-02-06 00:00:00' AND local_utc_ts < '2016-02-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_07_uniq_device_id_account_id_ts on device_sensors_par_2016_02_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_07_ts_idx on device_sensors_par_2016_02_07(ts);
ALTER TABLE device_sensors_par_2016_02_07 ADD CHECK (local_utc_ts >= '2016-02-07 00:00:00' AND local_utc_ts < '2016-02-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_08_uniq_device_id_account_id_ts on device_sensors_par_2016_02_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_08_ts_idx on device_sensors_par_2016_02_08(ts);
ALTER TABLE device_sensors_par_2016_02_08 ADD CHECK (local_utc_ts >= '2016-02-08 00:00:00' AND local_utc_ts < '2016-02-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_09_uniq_device_id_account_id_ts on device_sensors_par_2016_02_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_09_ts_idx on device_sensors_par_2016_02_09(ts);
ALTER TABLE device_sensors_par_2016_02_09 ADD CHECK (local_utc_ts >= '2016-02-09 00:00:00' AND local_utc_ts < '2016-02-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_10_uniq_device_id_account_id_ts on device_sensors_par_2016_02_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_10_ts_idx on device_sensors_par_2016_02_10(ts);
ALTER TABLE device_sensors_par_2016_02_10 ADD CHECK (local_utc_ts >= '2016-02-10 00:00:00' AND local_utc_ts < '2016-02-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_11_uniq_device_id_account_id_ts on device_sensors_par_2016_02_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_11_ts_idx on device_sensors_par_2016_02_11(ts);
ALTER TABLE device_sensors_par_2016_02_11 ADD CHECK (local_utc_ts >= '2016-02-11 00:00:00' AND local_utc_ts < '2016-02-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_12_uniq_device_id_account_id_ts on device_sensors_par_2016_02_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_12_ts_idx on device_sensors_par_2016_02_12(ts);
ALTER TABLE device_sensors_par_2016_02_12 ADD CHECK (local_utc_ts >= '2016-02-12 00:00:00' AND local_utc_ts < '2016-02-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_13_uniq_device_id_account_id_ts on device_sensors_par_2016_02_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_13_ts_idx on device_sensors_par_2016_02_13(ts);
ALTER TABLE device_sensors_par_2016_02_13 ADD CHECK (local_utc_ts >= '2016-02-13 00:00:00' AND local_utc_ts < '2016-02-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_14_uniq_device_id_account_id_ts on device_sensors_par_2016_02_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_14_ts_idx on device_sensors_par_2016_02_14(ts);
ALTER TABLE device_sensors_par_2016_02_14 ADD CHECK (local_utc_ts >= '2016-02-14 00:00:00' AND local_utc_ts < '2016-02-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_15_uniq_device_id_account_id_ts on device_sensors_par_2016_02_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_15_ts_idx on device_sensors_par_2016_02_15(ts);
ALTER TABLE device_sensors_par_2016_02_15 ADD CHECK (local_utc_ts >= '2016-02-15 00:00:00' AND local_utc_ts < '2016-02-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_16_uniq_device_id_account_id_ts on device_sensors_par_2016_02_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_16_ts_idx on device_sensors_par_2016_02_16(ts);
ALTER TABLE device_sensors_par_2016_02_16 ADD CHECK (local_utc_ts >= '2016-02-16 00:00:00' AND local_utc_ts < '2016-02-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_17_uniq_device_id_account_id_ts on device_sensors_par_2016_02_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_17_ts_idx on device_sensors_par_2016_02_17(ts);
ALTER TABLE device_sensors_par_2016_02_17 ADD CHECK (local_utc_ts >= '2016-02-17 00:00:00' AND local_utc_ts < '2016-02-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_18_uniq_device_id_account_id_ts on device_sensors_par_2016_02_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_18_ts_idx on device_sensors_par_2016_02_18(ts);
ALTER TABLE device_sensors_par_2016_02_18 ADD CHECK (local_utc_ts >= '2016-02-18 00:00:00' AND local_utc_ts < '2016-02-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_19_uniq_device_id_account_id_ts on device_sensors_par_2016_02_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_19_ts_idx on device_sensors_par_2016_02_19(ts);
ALTER TABLE device_sensors_par_2016_02_19 ADD CHECK (local_utc_ts >= '2016-02-19 00:00:00' AND local_utc_ts < '2016-02-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_20_uniq_device_id_account_id_ts on device_sensors_par_2016_02_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_20_ts_idx on device_sensors_par_2016_02_20(ts);
ALTER TABLE device_sensors_par_2016_02_20 ADD CHECK (local_utc_ts >= '2016-02-20 00:00:00' AND local_utc_ts < '2016-02-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_21_uniq_device_id_account_id_ts on device_sensors_par_2016_02_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_21_ts_idx on device_sensors_par_2016_02_21(ts);
ALTER TABLE device_sensors_par_2016_02_21 ADD CHECK (local_utc_ts >= '2016-02-21 00:00:00' AND local_utc_ts < '2016-02-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_22_uniq_device_id_account_id_ts on device_sensors_par_2016_02_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_22_ts_idx on device_sensors_par_2016_02_22(ts);
ALTER TABLE device_sensors_par_2016_02_22 ADD CHECK (local_utc_ts >= '2016-02-22 00:00:00' AND local_utc_ts < '2016-02-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_23_uniq_device_id_account_id_ts on device_sensors_par_2016_02_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_23_ts_idx on device_sensors_par_2016_02_23(ts);
ALTER TABLE device_sensors_par_2016_02_23 ADD CHECK (local_utc_ts >= '2016-02-23 00:00:00' AND local_utc_ts < '2016-02-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_24_uniq_device_id_account_id_ts on device_sensors_par_2016_02_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_24_ts_idx on device_sensors_par_2016_02_24(ts);
ALTER TABLE device_sensors_par_2016_02_24 ADD CHECK (local_utc_ts >= '2016-02-24 00:00:00' AND local_utc_ts < '2016-02-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_25_uniq_device_id_account_id_ts on device_sensors_par_2016_02_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_25_ts_idx on device_sensors_par_2016_02_25(ts);
ALTER TABLE device_sensors_par_2016_02_25 ADD CHECK (local_utc_ts >= '2016-02-25 00:00:00' AND local_utc_ts < '2016-02-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_26_uniq_device_id_account_id_ts on device_sensors_par_2016_02_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_26_ts_idx on device_sensors_par_2016_02_26(ts);
ALTER TABLE device_sensors_par_2016_02_26 ADD CHECK (local_utc_ts >= '2016-02-26 00:00:00' AND local_utc_ts < '2016-02-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_27_uniq_device_id_account_id_ts on device_sensors_par_2016_02_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_27_ts_idx on device_sensors_par_2016_02_27(ts);
ALTER TABLE device_sensors_par_2016_02_27 ADD CHECK (local_utc_ts >= '2016-02-27 00:00:00' AND local_utc_ts < '2016-02-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_28_uniq_device_id_account_id_ts on device_sensors_par_2016_02_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_28_ts_idx on device_sensors_par_2016_02_28(ts);
ALTER TABLE device_sensors_par_2016_02_28 ADD CHECK (local_utc_ts >= '2016-02-28 00:00:00' AND local_utc_ts < '2016-02-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_02_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_02_29_uniq_device_id_account_id_ts on device_sensors_par_2016_02_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_02_29_ts_idx on device_sensors_par_2016_02_29(ts);
ALTER TABLE device_sensors_par_2016_02_29 ADD CHECK (local_utc_ts >= '2016-02-29 00:00:00' AND local_utc_ts < '2016-03-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-02-29 00:00:00' AND NEW.local_utc_ts < '2016-03-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-28 00:00:00' AND NEW.local_utc_ts < '2016-02-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-27 00:00:00' AND NEW.local_utc_ts < '2016-02-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-26 00:00:00' AND NEW.local_utc_ts < '2016-02-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-25 00:00:00' AND NEW.local_utc_ts < '2016-02-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-24 00:00:00' AND NEW.local_utc_ts < '2016-02-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-23 00:00:00' AND NEW.local_utc_ts < '2016-02-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-22 00:00:00' AND NEW.local_utc_ts < '2016-02-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-21 00:00:00' AND NEW.local_utc_ts < '2016-02-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-20 00:00:00' AND NEW.local_utc_ts < '2016-02-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-19 00:00:00' AND NEW.local_utc_ts < '2016-02-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-18 00:00:00' AND NEW.local_utc_ts < '2016-02-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-17 00:00:00' AND NEW.local_utc_ts < '2016-02-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-16 00:00:00' AND NEW.local_utc_ts < '2016-02-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-15 00:00:00' AND NEW.local_utc_ts < '2016-02-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-14 00:00:00' AND NEW.local_utc_ts < '2016-02-15 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-13 00:00:00' AND NEW.local_utc_ts < '2016-02-14 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-12 00:00:00' AND NEW.local_utc_ts < '2016-02-13 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-11 00:00:00' AND NEW.local_utc_ts < '2016-02-12 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-10 00:00:00' AND NEW.local_utc_ts < '2016-02-11 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-09 00:00:00' AND NEW.local_utc_ts < '2016-02-10 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-08 00:00:00' AND NEW.local_utc_ts < '2016-02-09 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-07 00:00:00' AND NEW.local_utc_ts < '2016-02-08 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-06 00:00:00' AND NEW.local_utc_ts < '2016-02-07 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-05 00:00:00' AND NEW.local_utc_ts < '2016-02-06 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-04 00:00:00' AND NEW.local_utc_ts < '2016-02-05 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-03 00:00:00' AND NEW.local_utc_ts < '2016-02-04 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-02 00:00:00' AND NEW.local_utc_ts < '2016-02-03 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-01 00:00:00' AND NEW.local_utc_ts < '2016-02-02 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_02_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-31 00:00:00' AND NEW.local_utc_ts < '2016-02-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-30 00:00:00' AND NEW.local_utc_ts < '2016-01-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-29 00:00:00' AND NEW.local_utc_ts < '2016-01-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-28 00:00:00' AND NEW.local_utc_ts < '2016-01-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-27 00:00:00' AND NEW.local_utc_ts < '2016-01-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-26 00:00:00' AND NEW.local_utc_ts < '2016-01-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-25 00:00:00' AND NEW.local_utc_ts < '2016-01-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-24 00:00:00' AND NEW.local_utc_ts < '2016-01-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-23 00:00:00' AND NEW.local_utc_ts < '2016-01-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-22 00:00:00' AND NEW.local_utc_ts < '2016-01-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-21 00:00:00' AND NEW.local_utc_ts < '2016-01-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-20 00:00:00' AND NEW.local_utc_ts < '2016-01-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-19 00:00:00' AND NEW.local_utc_ts < '2016-01-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_19 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2016-02 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_01() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_01_uniq_tracker_ts ON tracker_motion_par_2016_02_01(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_01_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_01(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_01_local_utc_ts ON tracker_motion_par_2016_02_01(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_01 ADD CHECK (local_utc_ts >= '2016-02-01 00:00:00' AND local_utc_ts < '2016-02-02 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_02() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_02_uniq_tracker_ts ON tracker_motion_par_2016_02_02(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_02_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_02(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_02_local_utc_ts ON tracker_motion_par_2016_02_02(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_02 ADD CHECK (local_utc_ts >= '2016-02-02 00:00:00' AND local_utc_ts < '2016-02-03 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_03() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_03_uniq_tracker_ts ON tracker_motion_par_2016_02_03(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_03_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_03(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_03_local_utc_ts ON tracker_motion_par_2016_02_03(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_03 ADD CHECK (local_utc_ts >= '2016-02-03 00:00:00' AND local_utc_ts < '2016-02-04 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_04() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_04_uniq_tracker_ts ON tracker_motion_par_2016_02_04(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_04_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_04(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_04_local_utc_ts ON tracker_motion_par_2016_02_04(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_04 ADD CHECK (local_utc_ts >= '2016-02-04 00:00:00' AND local_utc_ts < '2016-02-05 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_05() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_05_uniq_tracker_ts ON tracker_motion_par_2016_02_05(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_05_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_05(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_05_local_utc_ts ON tracker_motion_par_2016_02_05(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_05 ADD CHECK (local_utc_ts >= '2016-02-05 00:00:00' AND local_utc_ts < '2016-02-06 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_06() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_06_uniq_tracker_ts ON tracker_motion_par_2016_02_06(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_06_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_06(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_06_local_utc_ts ON tracker_motion_par_2016_02_06(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_06 ADD CHECK (local_utc_ts >= '2016-02-06 00:00:00' AND local_utc_ts < '2016-02-07 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_07_uniq_tracker_ts ON tracker_motion_par_2016_02_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_07_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_07_local_utc_ts ON tracker_motion_par_2016_02_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_07 ADD CHECK (local_utc_ts >= '2016-02-07 00:00:00' AND local_utc_ts < '2016-02-08 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_08_uniq_tracker_ts ON tracker_motion_par_2016_02_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_08_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_08_local_utc_ts ON tracker_motion_par_2016_02_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_08 ADD CHECK (local_utc_ts >= '2016-02-08 00:00:00' AND local_utc_ts < '2016-02-09 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_09() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_09_uniq_tracker_ts ON tracker_motion_par_2016_02_09(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_09_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_09(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_09_local_utc_ts ON tracker_motion_par_2016_02_09(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_09 ADD CHECK (local_utc_ts >= '2016-02-09 00:00:00' AND local_utc_ts < '2016-02-10 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_10() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_10_uniq_tracker_ts ON tracker_motion_par_2016_02_10(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_10_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_10(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_10_local_utc_ts ON tracker_motion_par_2016_02_10(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_10 ADD CHECK (local_utc_ts >= '2016-02-10 00:00:00' AND local_utc_ts < '2016-02-11 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_11() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_11_uniq_tracker_ts ON tracker_motion_par_2016_02_11(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_11_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_11(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_11_local_utc_ts ON tracker_motion_par_2016_02_11(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_11 ADD CHECK (local_utc_ts >= '2016-02-11 00:00:00' AND local_utc_ts < '2016-02-12 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_12() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_12_uniq_tracker_ts ON tracker_motion_par_2016_02_12(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_12_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_12(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_12_local_utc_ts ON tracker_motion_par_2016_02_12(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_12 ADD CHECK (local_utc_ts >= '2016-02-12 00:00:00' AND local_utc_ts < '2016-02-13 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_13() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_13_uniq_tracker_ts ON tracker_motion_par_2016_02_13(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_13_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_13(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_13_local_utc_ts ON tracker_motion_par_2016_02_13(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_13 ADD CHECK (local_utc_ts >= '2016-02-13 00:00:00' AND local_utc_ts < '2016-02-14 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_14() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_14_uniq_tracker_ts ON tracker_motion_par_2016_02_14(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_14_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_14(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_14_local_utc_ts ON tracker_motion_par_2016_02_14(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_14 ADD CHECK (local_utc_ts >= '2016-02-14 00:00:00' AND local_utc_ts < '2016-02-15 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_15() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_15_uniq_tracker_ts ON tracker_motion_par_2016_02_15(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_15_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_15(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_15_local_utc_ts ON tracker_motion_par_2016_02_15(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_15 ADD CHECK (local_utc_ts >= '2016-02-15 00:00:00' AND local_utc_ts < '2016-02-16 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_16() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_16_uniq_tracker_ts ON tracker_motion_par_2016_02_16(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_16_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_16(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_16_local_utc_ts ON tracker_motion_par_2016_02_16(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_16 ADD CHECK (local_utc_ts >= '2016-02-16 00:00:00' AND local_utc_ts < '2016-02-17 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_17() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_17_uniq_tracker_ts ON tracker_motion_par_2016_02_17(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_17_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_17(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_17_local_utc_ts ON tracker_motion_par_2016_02_17(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_17 ADD CHECK (local_utc_ts >= '2016-02-17 00:00:00' AND local_utc_ts < '2016-02-18 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_18() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_18_uniq_tracker_ts ON tracker_motion_par_2016_02_18(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_18_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_18(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_18_local_utc_ts ON tracker_motion_par_2016_02_18(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_18 ADD CHECK (local_utc_ts >= '2016-02-18 00:00:00' AND local_utc_ts < '2016-02-19 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_19() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_19_uniq_tracker_ts ON tracker_motion_par_2016_02_19(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_19_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_19(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_19_local_utc_ts ON tracker_motion_par_2016_02_19(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_19 ADD CHECK (local_utc_ts >= '2016-02-19 00:00:00' AND local_utc_ts < '2016-02-20 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_20() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_20_uniq_tracker_ts ON tracker_motion_par_2016_02_20(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_20_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_20(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_20_local_utc_ts ON tracker_motion_par_2016_02_20(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_20 ADD CHECK (local_utc_ts >= '2016-02-20 00:00:00' AND local_utc_ts < '2016-02-21 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_21() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_21_uniq_tracker_ts ON tracker_motion_par_2016_02_21(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_21_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_21(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_21_local_utc_ts ON tracker_motion_par_2016_02_21(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_21 ADD CHECK (local_utc_ts >= '2016-02-21 00:00:00' AND local_utc_ts < '2016-02-22 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_22() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_22_uniq_tracker_ts ON tracker_motion_par_2016_02_22(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_22_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_22(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_22_local_utc_ts ON tracker_motion_par_2016_02_22(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_22 ADD CHECK (local_utc_ts >= '2016-02-22 00:00:00' AND local_utc_ts < '2016-02-23 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_23() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_23_uniq_tracker_ts ON tracker_motion_par_2016_02_23(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_23_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_23(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_23_local_utc_ts ON tracker_motion_par_2016_02_23(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_23 ADD CHECK (local_utc_ts >= '2016-02-23 00:00:00' AND local_utc_ts < '2016-02-24 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_24() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_24_uniq_tracker_ts ON tracker_motion_par_2016_02_24(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_24_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_24(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_24_local_utc_ts ON tracker_motion_par_2016_02_24(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_24 ADD CHECK (local_utc_ts >= '2016-02-24 00:00:00' AND local_utc_ts < '2016-02-25 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_25() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_25_uniq_tracker_ts ON tracker_motion_par_2016_02_25(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_25_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_25(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_25_local_utc_ts ON tracker_motion_par_2016_02_25(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_25 ADD CHECK (local_utc_ts >= '2016-02-25 00:00:00' AND local_utc_ts < '2016-02-26 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_26() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_26_uniq_tracker_ts ON tracker_motion_par_2016_02_26(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_26_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_26(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_26_local_utc_ts ON tracker_motion_par_2016_02_26(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_26 ADD CHECK (local_utc_ts >= '2016-02-26 00:00:00' AND local_utc_ts < '2016-02-27 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_27() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_27_uniq_tracker_ts ON tracker_motion_par_2016_02_27(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_27_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_27(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_27_local_utc_ts ON tracker_motion_par_2016_02_27(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_27 ADD CHECK (local_utc_ts >= '2016-02-27 00:00:00' AND local_utc_ts < '2016-02-28 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_28() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_28_uniq_tracker_ts ON tracker_motion_par_2016_02_28(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_28_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_28(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_28_local_utc_ts ON tracker_motion_par_2016_02_28(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_28 ADD CHECK (local_utc_ts >= '2016-02-28 00:00:00' AND local_utc_ts < '2016-02-29 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_02_29() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_29_uniq_tracker_ts ON tracker_motion_par_2016_02_29(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_02_29_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_02_29(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_02_29_local_utc_ts ON tracker_motion_par_2016_02_29(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_02_29 ADD CHECK (local_utc_ts >= '2016-02-29 00:00:00' AND local_utc_ts < '2016-03-01 00:00:00');




CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-02-29 00:00:00' AND NEW.local_utc_ts < '2016-03-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-28 00:00:00' AND NEW.local_utc_ts < '2016-02-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-27 00:00:00' AND NEW.local_utc_ts < '2016-02-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-26 00:00:00' AND NEW.local_utc_ts < '2016-02-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-25 00:00:00' AND NEW.local_utc_ts < '2016-02-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-24 00:00:00' AND NEW.local_utc_ts < '2016-02-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-23 00:00:00' AND NEW.local_utc_ts < '2016-02-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-22 00:00:00' AND NEW.local_utc_ts < '2016-02-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-21 00:00:00' AND NEW.local_utc_ts < '2016-02-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-20 00:00:00' AND NEW.local_utc_ts < '2016-02-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-19 00:00:00' AND NEW.local_utc_ts < '2016-02-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-18 00:00:00' AND NEW.local_utc_ts < '2016-02-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-17 00:00:00' AND NEW.local_utc_ts < '2016-02-18 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-16 00:00:00' AND NEW.local_utc_ts < '2016-02-17 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-15 00:00:00' AND NEW.local_utc_ts < '2016-02-16 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-14 00:00:00' AND NEW.local_utc_ts < '2016-02-15 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-13 00:00:00' AND NEW.local_utc_ts < '2016-02-14 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-12 00:00:00' AND NEW.local_utc_ts < '2016-02-13 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-11 00:00:00' AND NEW.local_utc_ts < '2016-02-12 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-10 00:00:00' AND NEW.local_utc_ts < '2016-02-11 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-09 00:00:00' AND NEW.local_utc_ts < '2016-02-10 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-08 00:00:00' AND NEW.local_utc_ts < '2016-02-09 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-07 00:00:00' AND NEW.local_utc_ts < '2016-02-08 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-06 00:00:00' AND NEW.local_utc_ts < '2016-02-07 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-05 00:00:00' AND NEW.local_utc_ts < '2016-02-06 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-04 00:00:00' AND NEW.local_utc_ts < '2016-02-05 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-03 00:00:00' AND NEW.local_utc_ts < '2016-02-04 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-02 00:00:00' AND NEW.local_utc_ts < '2016-02-03 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-01 00:00:00' AND NEW.local_utc_ts < '2016-02-02 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_02_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-31 00:00:00' AND NEW.local_utc_ts < '2016-02-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-30 00:00:00' AND NEW.local_utc_ts < '2016-01-31 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-29 00:00:00' AND NEW.local_utc_ts < '2016-01-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-28 00:00:00' AND NEW.local_utc_ts < '2016-01-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-27 00:00:00' AND NEW.local_utc_ts < '2016-01-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-26 00:00:00' AND NEW.local_utc_ts < '2016-01-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-25 00:00:00' AND NEW.local_utc_ts < '2016-01-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-24 00:00:00' AND NEW.local_utc_ts < '2016-01-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-23 00:00:00' AND NEW.local_utc_ts < '2016-01-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-22 00:00:00' AND NEW.local_utc_ts < '2016-01-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-21 00:00:00' AND NEW.local_utc_ts < '2016-01-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-20 00:00:00' AND NEW.local_utc_ts < '2016-01-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-19 00:00:00' AND NEW.local_utc_ts < '2016-01-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_19 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;
