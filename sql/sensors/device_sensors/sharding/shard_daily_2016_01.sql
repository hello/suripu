
-- 2016-01 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_01_uniq_device_id_account_id_ts on device_sensors_par_2016_01_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_01_ts_idx on device_sensors_par_2016_01_01(ts);
ALTER TABLE device_sensors_par_2016_01_01 ADD CHECK (local_utc_ts >= '2016-01-01 00:00:00' AND local_utc_ts < '2016-01-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_02_uniq_device_id_account_id_ts on device_sensors_par_2016_01_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_02_ts_idx on device_sensors_par_2016_01_02(ts);
ALTER TABLE device_sensors_par_2016_01_02 ADD CHECK (local_utc_ts >= '2016-01-02 00:00:00' AND local_utc_ts < '2016-01-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_03_uniq_device_id_account_id_ts on device_sensors_par_2016_01_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_03_ts_idx on device_sensors_par_2016_01_03(ts);
ALTER TABLE device_sensors_par_2016_01_03 ADD CHECK (local_utc_ts >= '2016-01-03 00:00:00' AND local_utc_ts < '2016-01-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_04_uniq_device_id_account_id_ts on device_sensors_par_2016_01_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_04_ts_idx on device_sensors_par_2016_01_04(ts);
ALTER TABLE device_sensors_par_2016_01_04 ADD CHECK (local_utc_ts >= '2016-01-04 00:00:00' AND local_utc_ts < '2016-01-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_05_uniq_device_id_account_id_ts on device_sensors_par_2016_01_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_05_ts_idx on device_sensors_par_2016_01_05(ts);
ALTER TABLE device_sensors_par_2016_01_05 ADD CHECK (local_utc_ts >= '2016-01-05 00:00:00' AND local_utc_ts < '2016-01-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_06_uniq_device_id_account_id_ts on device_sensors_par_2016_01_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_06_ts_idx on device_sensors_par_2016_01_06(ts);
ALTER TABLE device_sensors_par_2016_01_06 ADD CHECK (local_utc_ts >= '2016-01-06 00:00:00' AND local_utc_ts < '2016-01-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_07_uniq_device_id_account_id_ts on device_sensors_par_2016_01_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_07_ts_idx on device_sensors_par_2016_01_07(ts);
ALTER TABLE device_sensors_par_2016_01_07 ADD CHECK (local_utc_ts >= '2016-01-07 00:00:00' AND local_utc_ts < '2016-01-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_08_uniq_device_id_account_id_ts on device_sensors_par_2016_01_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_08_ts_idx on device_sensors_par_2016_01_08(ts);
ALTER TABLE device_sensors_par_2016_01_08 ADD CHECK (local_utc_ts >= '2016-01-08 00:00:00' AND local_utc_ts < '2016-01-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_09_uniq_device_id_account_id_ts on device_sensors_par_2016_01_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_09_ts_idx on device_sensors_par_2016_01_09(ts);
ALTER TABLE device_sensors_par_2016_01_09 ADD CHECK (local_utc_ts >= '2016-01-09 00:00:00' AND local_utc_ts < '2016-01-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_10_uniq_device_id_account_id_ts on device_sensors_par_2016_01_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_10_ts_idx on device_sensors_par_2016_01_10(ts);
ALTER TABLE device_sensors_par_2016_01_10 ADD CHECK (local_utc_ts >= '2016-01-10 00:00:00' AND local_utc_ts < '2016-01-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_11_uniq_device_id_account_id_ts on device_sensors_par_2016_01_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_11_ts_idx on device_sensors_par_2016_01_11(ts);
ALTER TABLE device_sensors_par_2016_01_11 ADD CHECK (local_utc_ts >= '2016-01-11 00:00:00' AND local_utc_ts < '2016-01-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_12_uniq_device_id_account_id_ts on device_sensors_par_2016_01_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_12_ts_idx on device_sensors_par_2016_01_12(ts);
ALTER TABLE device_sensors_par_2016_01_12 ADD CHECK (local_utc_ts >= '2016-01-12 00:00:00' AND local_utc_ts < '2016-01-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_13_uniq_device_id_account_id_ts on device_sensors_par_2016_01_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_13_ts_idx on device_sensors_par_2016_01_13(ts);
ALTER TABLE device_sensors_par_2016_01_13 ADD CHECK (local_utc_ts >= '2016-01-13 00:00:00' AND local_utc_ts < '2016-01-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_14_uniq_device_id_account_id_ts on device_sensors_par_2016_01_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_14_ts_idx on device_sensors_par_2016_01_14(ts);
ALTER TABLE device_sensors_par_2016_01_14 ADD CHECK (local_utc_ts >= '2016-01-14 00:00:00' AND local_utc_ts < '2016-01-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_15_uniq_device_id_account_id_ts on device_sensors_par_2016_01_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_15_ts_idx on device_sensors_par_2016_01_15(ts);
ALTER TABLE device_sensors_par_2016_01_15 ADD CHECK (local_utc_ts >= '2016-01-15 00:00:00' AND local_utc_ts < '2016-01-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_16_uniq_device_id_account_id_ts on device_sensors_par_2016_01_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_16_ts_idx on device_sensors_par_2016_01_16(ts);
ALTER TABLE device_sensors_par_2016_01_16 ADD CHECK (local_utc_ts >= '2016-01-16 00:00:00' AND local_utc_ts < '2016-01-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_17_uniq_device_id_account_id_ts on device_sensors_par_2016_01_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_17_ts_idx on device_sensors_par_2016_01_17(ts);
ALTER TABLE device_sensors_par_2016_01_17 ADD CHECK (local_utc_ts >= '2016-01-17 00:00:00' AND local_utc_ts < '2016-01-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_18_uniq_device_id_account_id_ts on device_sensors_par_2016_01_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_18_ts_idx on device_sensors_par_2016_01_18(ts);
ALTER TABLE device_sensors_par_2016_01_18 ADD CHECK (local_utc_ts >= '2016-01-18 00:00:00' AND local_utc_ts < '2016-01-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_19_uniq_device_id_account_id_ts on device_sensors_par_2016_01_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_19_ts_idx on device_sensors_par_2016_01_19(ts);
ALTER TABLE device_sensors_par_2016_01_19 ADD CHECK (local_utc_ts >= '2016-01-19 00:00:00' AND local_utc_ts < '2016-01-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_20_uniq_device_id_account_id_ts on device_sensors_par_2016_01_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_20_ts_idx on device_sensors_par_2016_01_20(ts);
ALTER TABLE device_sensors_par_2016_01_20 ADD CHECK (local_utc_ts >= '2016-01-20 00:00:00' AND local_utc_ts < '2016-01-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_21_uniq_device_id_account_id_ts on device_sensors_par_2016_01_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_21_ts_idx on device_sensors_par_2016_01_21(ts);
ALTER TABLE device_sensors_par_2016_01_21 ADD CHECK (local_utc_ts >= '2016-01-21 00:00:00' AND local_utc_ts < '2016-01-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_22_uniq_device_id_account_id_ts on device_sensors_par_2016_01_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_22_ts_idx on device_sensors_par_2016_01_22(ts);
ALTER TABLE device_sensors_par_2016_01_22 ADD CHECK (local_utc_ts >= '2016-01-22 00:00:00' AND local_utc_ts < '2016-01-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_23_uniq_device_id_account_id_ts on device_sensors_par_2016_01_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_23_ts_idx on device_sensors_par_2016_01_23(ts);
ALTER TABLE device_sensors_par_2016_01_23 ADD CHECK (local_utc_ts >= '2016-01-23 00:00:00' AND local_utc_ts < '2016-01-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_24_uniq_device_id_account_id_ts on device_sensors_par_2016_01_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_24_ts_idx on device_sensors_par_2016_01_24(ts);
ALTER TABLE device_sensors_par_2016_01_24 ADD CHECK (local_utc_ts >= '2016-01-24 00:00:00' AND local_utc_ts < '2016-01-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_25_uniq_device_id_account_id_ts on device_sensors_par_2016_01_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_25_ts_idx on device_sensors_par_2016_01_25(ts);
ALTER TABLE device_sensors_par_2016_01_25 ADD CHECK (local_utc_ts >= '2016-01-25 00:00:00' AND local_utc_ts < '2016-01-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_26_uniq_device_id_account_id_ts on device_sensors_par_2016_01_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_26_ts_idx on device_sensors_par_2016_01_26(ts);
ALTER TABLE device_sensors_par_2016_01_26 ADD CHECK (local_utc_ts >= '2016-01-26 00:00:00' AND local_utc_ts < '2016-01-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_27_uniq_device_id_account_id_ts on device_sensors_par_2016_01_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_27_ts_idx on device_sensors_par_2016_01_27(ts);
ALTER TABLE device_sensors_par_2016_01_27 ADD CHECK (local_utc_ts >= '2016-01-27 00:00:00' AND local_utc_ts < '2016-01-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_28_uniq_device_id_account_id_ts on device_sensors_par_2016_01_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_28_ts_idx on device_sensors_par_2016_01_28(ts);
ALTER TABLE device_sensors_par_2016_01_28 ADD CHECK (local_utc_ts >= '2016-01-28 00:00:00' AND local_utc_ts < '2016-01-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_29_uniq_device_id_account_id_ts on device_sensors_par_2016_01_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_29_ts_idx on device_sensors_par_2016_01_29(ts);
ALTER TABLE device_sensors_par_2016_01_29 ADD CHECK (local_utc_ts >= '2016-01-29 00:00:00' AND local_utc_ts < '2016-01-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_30_uniq_device_id_account_id_ts on device_sensors_par_2016_01_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_30_ts_idx on device_sensors_par_2016_01_30(ts);
ALTER TABLE device_sensors_par_2016_01_30 ADD CHECK (local_utc_ts >= '2016-01-30 00:00:00' AND local_utc_ts < '2016-01-31 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_01_31() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_01_31_uniq_device_id_account_id_ts on device_sensors_par_2016_01_31(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_01_31_ts_idx on device_sensors_par_2016_01_31(ts);
ALTER TABLE device_sensors_par_2016_01_31 ADD CHECK (local_utc_ts >= '2016-01-31 00:00:00' AND local_utc_ts < '2016-02-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-01-31 00:00:00' AND NEW.local_utc_ts < '2016-02-01 00:00:00' THEN
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
    ELSIF NEW.local_utc_ts >= '2016-01-18 00:00:00' AND NEW.local_utc_ts < '2016-01-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-17 00:00:00' AND NEW.local_utc_ts < '2016-01-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-16 00:00:00' AND NEW.local_utc_ts < '2016-01-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-15 00:00:00' AND NEW.local_utc_ts < '2016-01-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-14 00:00:00' AND NEW.local_utc_ts < '2016-01-15 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-13 00:00:00' AND NEW.local_utc_ts < '2016-01-14 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-12 00:00:00' AND NEW.local_utc_ts < '2016-01-13 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-11 00:00:00' AND NEW.local_utc_ts < '2016-01-12 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-10 00:00:00' AND NEW.local_utc_ts < '2016-01-11 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-09 00:00:00' AND NEW.local_utc_ts < '2016-01-10 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-08 00:00:00' AND NEW.local_utc_ts < '2016-01-09 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-07 00:00:00' AND NEW.local_utc_ts < '2016-01-08 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-06 00:00:00' AND NEW.local_utc_ts < '2016-01-07 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-05 00:00:00' AND NEW.local_utc_ts < '2016-01-06 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-04 00:00:00' AND NEW.local_utc_ts < '2016-01-05 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-03 00:00:00' AND NEW.local_utc_ts < '2016-01-04 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-02 00:00:00' AND NEW.local_utc_ts < '2016-01-03 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-01 00:00:00' AND NEW.local_utc_ts < '2016-01-02 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_01_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-31 00:00:00' AND NEW.local_utc_ts < '2016-01-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-30 00:00:00' AND NEW.local_utc_ts < '2015-12-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-29 00:00:00' AND NEW.local_utc_ts < '2015-12-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-28 00:00:00' AND NEW.local_utc_ts < '2015-12-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-27 00:00:00' AND NEW.local_utc_ts < '2015-12-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-26 00:00:00' AND NEW.local_utc_ts < '2015-12-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-25 00:00:00' AND NEW.local_utc_ts < '2015-12-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-24 00:00:00' AND NEW.local_utc_ts < '2015-12-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-23 00:00:00' AND NEW.local_utc_ts < '2015-12-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-22 00:00:00' AND NEW.local_utc_ts < '2015-12-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-21 00:00:00' AND NEW.local_utc_ts < '2015-12-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-20 00:00:00' AND NEW.local_utc_ts < '2015-12-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-19 00:00:00' AND NEW.local_utc_ts < '2015-12-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-18 00:00:00' AND NEW.local_utc_ts < '2015-12-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_12_18 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2016-01 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_01() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_01_uniq_tracker_ts ON tracker_motion_par_2016_01_01(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_01_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_01(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_01_local_utc_ts ON tracker_motion_par_2016_01_01(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_01 ADD CHECK (local_utc_ts >= '2016-01-01 00:00:00' AND local_utc_ts < '2016-01-02 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_02() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_02_uniq_tracker_ts ON tracker_motion_par_2016_01_02(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_02_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_02(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_02_local_utc_ts ON tracker_motion_par_2016_01_02(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_02 ADD CHECK (local_utc_ts >= '2016-01-02 00:00:00' AND local_utc_ts < '2016-01-03 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_03() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_03_uniq_tracker_ts ON tracker_motion_par_2016_01_03(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_03_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_03(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_03_local_utc_ts ON tracker_motion_par_2016_01_03(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_03 ADD CHECK (local_utc_ts >= '2016-01-03 00:00:00' AND local_utc_ts < '2016-01-04 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_04() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_04_uniq_tracker_ts ON tracker_motion_par_2016_01_04(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_04_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_04(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_04_local_utc_ts ON tracker_motion_par_2016_01_04(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_04 ADD CHECK (local_utc_ts >= '2016-01-04 00:00:00' AND local_utc_ts < '2016-01-05 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_05() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_05_uniq_tracker_ts ON tracker_motion_par_2016_01_05(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_05_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_05(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_05_local_utc_ts ON tracker_motion_par_2016_01_05(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_05 ADD CHECK (local_utc_ts >= '2016-01-05 00:00:00' AND local_utc_ts < '2016-01-06 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_06() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_06_uniq_tracker_ts ON tracker_motion_par_2016_01_06(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_06_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_06(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_06_local_utc_ts ON tracker_motion_par_2016_01_06(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_06 ADD CHECK (local_utc_ts >= '2016-01-06 00:00:00' AND local_utc_ts < '2016-01-07 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_07_uniq_tracker_ts ON tracker_motion_par_2016_01_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_07_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_07_local_utc_ts ON tracker_motion_par_2016_01_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_07 ADD CHECK (local_utc_ts >= '2016-01-07 00:00:00' AND local_utc_ts < '2016-01-08 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_08_uniq_tracker_ts ON tracker_motion_par_2016_01_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_08_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_08_local_utc_ts ON tracker_motion_par_2016_01_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_08 ADD CHECK (local_utc_ts >= '2016-01-08 00:00:00' AND local_utc_ts < '2016-01-09 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_09() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_09_uniq_tracker_ts ON tracker_motion_par_2016_01_09(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_09_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_09(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_09_local_utc_ts ON tracker_motion_par_2016_01_09(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_09 ADD CHECK (local_utc_ts >= '2016-01-09 00:00:00' AND local_utc_ts < '2016-01-10 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_10() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_10_uniq_tracker_ts ON tracker_motion_par_2016_01_10(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_10_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_10(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_10_local_utc_ts ON tracker_motion_par_2016_01_10(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_10 ADD CHECK (local_utc_ts >= '2016-01-10 00:00:00' AND local_utc_ts < '2016-01-11 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_11() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_11_uniq_tracker_ts ON tracker_motion_par_2016_01_11(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_11_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_11(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_11_local_utc_ts ON tracker_motion_par_2016_01_11(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_11 ADD CHECK (local_utc_ts >= '2016-01-11 00:00:00' AND local_utc_ts < '2016-01-12 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_12() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_12_uniq_tracker_ts ON tracker_motion_par_2016_01_12(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_12_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_12(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_12_local_utc_ts ON tracker_motion_par_2016_01_12(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_12 ADD CHECK (local_utc_ts >= '2016-01-12 00:00:00' AND local_utc_ts < '2016-01-13 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_13() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_13_uniq_tracker_ts ON tracker_motion_par_2016_01_13(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_13_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_13(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_13_local_utc_ts ON tracker_motion_par_2016_01_13(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_13 ADD CHECK (local_utc_ts >= '2016-01-13 00:00:00' AND local_utc_ts < '2016-01-14 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_14() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_14_uniq_tracker_ts ON tracker_motion_par_2016_01_14(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_14_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_14(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_14_local_utc_ts ON tracker_motion_par_2016_01_14(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_14 ADD CHECK (local_utc_ts >= '2016-01-14 00:00:00' AND local_utc_ts < '2016-01-15 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_15() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_15_uniq_tracker_ts ON tracker_motion_par_2016_01_15(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_15_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_15(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_15_local_utc_ts ON tracker_motion_par_2016_01_15(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_15 ADD CHECK (local_utc_ts >= '2016-01-15 00:00:00' AND local_utc_ts < '2016-01-16 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_16() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_16_uniq_tracker_ts ON tracker_motion_par_2016_01_16(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_16_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_16(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_16_local_utc_ts ON tracker_motion_par_2016_01_16(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_16 ADD CHECK (local_utc_ts >= '2016-01-16 00:00:00' AND local_utc_ts < '2016-01-17 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_17() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_17_uniq_tracker_ts ON tracker_motion_par_2016_01_17(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_17_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_17(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_17_local_utc_ts ON tracker_motion_par_2016_01_17(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_17 ADD CHECK (local_utc_ts >= '2016-01-17 00:00:00' AND local_utc_ts < '2016-01-18 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_18() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_18_uniq_tracker_ts ON tracker_motion_par_2016_01_18(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_18_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_18(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_18_local_utc_ts ON tracker_motion_par_2016_01_18(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_18 ADD CHECK (local_utc_ts >= '2016-01-18 00:00:00' AND local_utc_ts < '2016-01-19 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_19() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_19_uniq_tracker_ts ON tracker_motion_par_2016_01_19(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_19_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_19(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_19_local_utc_ts ON tracker_motion_par_2016_01_19(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_19 ADD CHECK (local_utc_ts >= '2016-01-19 00:00:00' AND local_utc_ts < '2016-01-20 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_20() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_20_uniq_tracker_ts ON tracker_motion_par_2016_01_20(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_20_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_20(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_20_local_utc_ts ON tracker_motion_par_2016_01_20(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_20 ADD CHECK (local_utc_ts >= '2016-01-20 00:00:00' AND local_utc_ts < '2016-01-21 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_21() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_21_uniq_tracker_ts ON tracker_motion_par_2016_01_21(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_21_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_21(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_21_local_utc_ts ON tracker_motion_par_2016_01_21(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_21 ADD CHECK (local_utc_ts >= '2016-01-21 00:00:00' AND local_utc_ts < '2016-01-22 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_22() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_22_uniq_tracker_ts ON tracker_motion_par_2016_01_22(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_22_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_22(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_22_local_utc_ts ON tracker_motion_par_2016_01_22(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_22 ADD CHECK (local_utc_ts >= '2016-01-22 00:00:00' AND local_utc_ts < '2016-01-23 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_23() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_23_uniq_tracker_ts ON tracker_motion_par_2016_01_23(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_23_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_23(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_23_local_utc_ts ON tracker_motion_par_2016_01_23(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_23 ADD CHECK (local_utc_ts >= '2016-01-23 00:00:00' AND local_utc_ts < '2016-01-24 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_24() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_24_uniq_tracker_ts ON tracker_motion_par_2016_01_24(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_24_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_24(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_24_local_utc_ts ON tracker_motion_par_2016_01_24(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_24 ADD CHECK (local_utc_ts >= '2016-01-24 00:00:00' AND local_utc_ts < '2016-01-25 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_25() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_25_uniq_tracker_ts ON tracker_motion_par_2016_01_25(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_25_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_25(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_25_local_utc_ts ON tracker_motion_par_2016_01_25(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_25 ADD CHECK (local_utc_ts >= '2016-01-25 00:00:00' AND local_utc_ts < '2016-01-26 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_26() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_26_uniq_tracker_ts ON tracker_motion_par_2016_01_26(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_26_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_26(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_26_local_utc_ts ON tracker_motion_par_2016_01_26(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_26 ADD CHECK (local_utc_ts >= '2016-01-26 00:00:00' AND local_utc_ts < '2016-01-27 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_27() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_27_uniq_tracker_ts ON tracker_motion_par_2016_01_27(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_27_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_27(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_27_local_utc_ts ON tracker_motion_par_2016_01_27(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_27 ADD CHECK (local_utc_ts >= '2016-01-27 00:00:00' AND local_utc_ts < '2016-01-28 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_28() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_28_uniq_tracker_ts ON tracker_motion_par_2016_01_28(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_28_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_28(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_28_local_utc_ts ON tracker_motion_par_2016_01_28(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_28 ADD CHECK (local_utc_ts >= '2016-01-28 00:00:00' AND local_utc_ts < '2016-01-29 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_29() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_29_uniq_tracker_ts ON tracker_motion_par_2016_01_29(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_29_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_29(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_29_local_utc_ts ON tracker_motion_par_2016_01_29(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_29 ADD CHECK (local_utc_ts >= '2016-01-29 00:00:00' AND local_utc_ts < '2016-01-30 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_30() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_30_uniq_tracker_ts ON tracker_motion_par_2016_01_30(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_30_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_30(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_30_local_utc_ts ON tracker_motion_par_2016_01_30(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_30 ADD CHECK (local_utc_ts >= '2016-01-30 00:00:00' AND local_utc_ts < '2016-01-31 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_01_31() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_31_uniq_tracker_ts ON tracker_motion_par_2016_01_31(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_01_31_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_01_31(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_01_31_local_utc_ts ON tracker_motion_par_2016_01_31(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_01_31 ADD CHECK (local_utc_ts >= '2016-01-31 00:00:00' AND local_utc_ts < '2016-02-01 00:00:00');




CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-01-31 00:00:00' AND NEW.local_utc_ts < '2016-02-01 00:00:00' THEN
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
    ELSIF NEW.local_utc_ts >= '2016-01-18 00:00:00' AND NEW.local_utc_ts < '2016-01-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-17 00:00:00' AND NEW.local_utc_ts < '2016-01-18 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-16 00:00:00' AND NEW.local_utc_ts < '2016-01-17 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-15 00:00:00' AND NEW.local_utc_ts < '2016-01-16 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-14 00:00:00' AND NEW.local_utc_ts < '2016-01-15 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-13 00:00:00' AND NEW.local_utc_ts < '2016-01-14 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-12 00:00:00' AND NEW.local_utc_ts < '2016-01-13 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-11 00:00:00' AND NEW.local_utc_ts < '2016-01-12 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-10 00:00:00' AND NEW.local_utc_ts < '2016-01-11 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-09 00:00:00' AND NEW.local_utc_ts < '2016-01-10 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-08 00:00:00' AND NEW.local_utc_ts < '2016-01-09 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-07 00:00:00' AND NEW.local_utc_ts < '2016-01-08 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-06 00:00:00' AND NEW.local_utc_ts < '2016-01-07 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-05 00:00:00' AND NEW.local_utc_ts < '2016-01-06 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-04 00:00:00' AND NEW.local_utc_ts < '2016-01-05 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-03 00:00:00' AND NEW.local_utc_ts < '2016-01-04 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-02 00:00:00' AND NEW.local_utc_ts < '2016-01-03 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-01-01 00:00:00' AND NEW.local_utc_ts < '2016-01-02 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_01_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-31 00:00:00' AND NEW.local_utc_ts < '2016-01-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-30 00:00:00' AND NEW.local_utc_ts < '2015-12-31 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-29 00:00:00' AND NEW.local_utc_ts < '2015-12-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-28 00:00:00' AND NEW.local_utc_ts < '2015-12-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-27 00:00:00' AND NEW.local_utc_ts < '2015-12-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-26 00:00:00' AND NEW.local_utc_ts < '2015-12-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-25 00:00:00' AND NEW.local_utc_ts < '2015-12-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-24 00:00:00' AND NEW.local_utc_ts < '2015-12-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-23 00:00:00' AND NEW.local_utc_ts < '2015-12-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-22 00:00:00' AND NEW.local_utc_ts < '2015-12-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-21 00:00:00' AND NEW.local_utc_ts < '2015-12-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-20 00:00:00' AND NEW.local_utc_ts < '2015-12-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-19 00:00:00' AND NEW.local_utc_ts < '2015-12-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-12-18 00:00:00' AND NEW.local_utc_ts < '2015-12-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_12_18 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;
