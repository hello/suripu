
-- 2015-08 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_01_uniq_device_id_account_id_ts on device_sensors_par_2015_08_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_01_ts_idx on device_sensors_par_2015_08_01(ts);
ALTER TABLE device_sensors_par_2015_08_01 ADD CHECK (local_utc_ts >= '2015-08-01 00:00:00' AND local_utc_ts < '2015-08-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_02_uniq_device_id_account_id_ts on device_sensors_par_2015_08_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_02_ts_idx on device_sensors_par_2015_08_02(ts);
ALTER TABLE device_sensors_par_2015_08_02 ADD CHECK (local_utc_ts >= '2015-08-02 00:00:00' AND local_utc_ts < '2015-08-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_03_uniq_device_id_account_id_ts on device_sensors_par_2015_08_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_03_ts_idx on device_sensors_par_2015_08_03(ts);
ALTER TABLE device_sensors_par_2015_08_03 ADD CHECK (local_utc_ts >= '2015-08-03 00:00:00' AND local_utc_ts < '2015-08-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_04_uniq_device_id_account_id_ts on device_sensors_par_2015_08_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_04_ts_idx on device_sensors_par_2015_08_04(ts);
ALTER TABLE device_sensors_par_2015_08_04 ADD CHECK (local_utc_ts >= '2015-08-04 00:00:00' AND local_utc_ts < '2015-08-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_05_uniq_device_id_account_id_ts on device_sensors_par_2015_08_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_05_ts_idx on device_sensors_par_2015_08_05(ts);
ALTER TABLE device_sensors_par_2015_08_05 ADD CHECK (local_utc_ts >= '2015-08-05 00:00:00' AND local_utc_ts < '2015-08-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_06_uniq_device_id_account_id_ts on device_sensors_par_2015_08_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_06_ts_idx on device_sensors_par_2015_08_06(ts);
ALTER TABLE device_sensors_par_2015_08_06 ADD CHECK (local_utc_ts >= '2015-08-06 00:00:00' AND local_utc_ts < '2015-08-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_07_uniq_device_id_account_id_ts on device_sensors_par_2015_08_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_07_ts_idx on device_sensors_par_2015_08_07(ts);
ALTER TABLE device_sensors_par_2015_08_07 ADD CHECK (local_utc_ts >= '2015-08-07 00:00:00' AND local_utc_ts < '2015-08-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_08_uniq_device_id_account_id_ts on device_sensors_par_2015_08_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_08_ts_idx on device_sensors_par_2015_08_08(ts);
ALTER TABLE device_sensors_par_2015_08_08 ADD CHECK (local_utc_ts >= '2015-08-08 00:00:00' AND local_utc_ts < '2015-08-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_09_uniq_device_id_account_id_ts on device_sensors_par_2015_08_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_09_ts_idx on device_sensors_par_2015_08_09(ts);
ALTER TABLE device_sensors_par_2015_08_09 ADD CHECK (local_utc_ts >= '2015-08-09 00:00:00' AND local_utc_ts < '2015-08-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_10_uniq_device_id_account_id_ts on device_sensors_par_2015_08_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_10_ts_idx on device_sensors_par_2015_08_10(ts);
ALTER TABLE device_sensors_par_2015_08_10 ADD CHECK (local_utc_ts >= '2015-08-10 00:00:00' AND local_utc_ts < '2015-08-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_11_uniq_device_id_account_id_ts on device_sensors_par_2015_08_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_11_ts_idx on device_sensors_par_2015_08_11(ts);
ALTER TABLE device_sensors_par_2015_08_11 ADD CHECK (local_utc_ts >= '2015-08-11 00:00:00' AND local_utc_ts < '2015-08-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_12_uniq_device_id_account_id_ts on device_sensors_par_2015_08_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_12_ts_idx on device_sensors_par_2015_08_12(ts);
ALTER TABLE device_sensors_par_2015_08_12 ADD CHECK (local_utc_ts >= '2015-08-12 00:00:00' AND local_utc_ts < '2015-08-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_13_uniq_device_id_account_id_ts on device_sensors_par_2015_08_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_13_ts_idx on device_sensors_par_2015_08_13(ts);
ALTER TABLE device_sensors_par_2015_08_13 ADD CHECK (local_utc_ts >= '2015-08-13 00:00:00' AND local_utc_ts < '2015-08-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_14_uniq_device_id_account_id_ts on device_sensors_par_2015_08_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_14_ts_idx on device_sensors_par_2015_08_14(ts);
ALTER TABLE device_sensors_par_2015_08_14 ADD CHECK (local_utc_ts >= '2015-08-14 00:00:00' AND local_utc_ts < '2015-08-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_15_uniq_device_id_account_id_ts on device_sensors_par_2015_08_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_15_ts_idx on device_sensors_par_2015_08_15(ts);
ALTER TABLE device_sensors_par_2015_08_15 ADD CHECK (local_utc_ts >= '2015-08-15 00:00:00' AND local_utc_ts < '2015-08-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_16_uniq_device_id_account_id_ts on device_sensors_par_2015_08_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_16_ts_idx on device_sensors_par_2015_08_16(ts);
ALTER TABLE device_sensors_par_2015_08_16 ADD CHECK (local_utc_ts >= '2015-08-16 00:00:00' AND local_utc_ts < '2015-08-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_17_uniq_device_id_account_id_ts on device_sensors_par_2015_08_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_17_ts_idx on device_sensors_par_2015_08_17(ts);
ALTER TABLE device_sensors_par_2015_08_17 ADD CHECK (local_utc_ts >= '2015-08-17 00:00:00' AND local_utc_ts < '2015-08-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_18_uniq_device_id_account_id_ts on device_sensors_par_2015_08_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_18_ts_idx on device_sensors_par_2015_08_18(ts);
ALTER TABLE device_sensors_par_2015_08_18 ADD CHECK (local_utc_ts >= '2015-08-18 00:00:00' AND local_utc_ts < '2015-08-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_19_uniq_device_id_account_id_ts on device_sensors_par_2015_08_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_19_ts_idx on device_sensors_par_2015_08_19(ts);
ALTER TABLE device_sensors_par_2015_08_19 ADD CHECK (local_utc_ts >= '2015-08-19 00:00:00' AND local_utc_ts < '2015-08-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_20_uniq_device_id_account_id_ts on device_sensors_par_2015_08_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_20_ts_idx on device_sensors_par_2015_08_20(ts);
ALTER TABLE device_sensors_par_2015_08_20 ADD CHECK (local_utc_ts >= '2015-08-20 00:00:00' AND local_utc_ts < '2015-08-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_21_uniq_device_id_account_id_ts on device_sensors_par_2015_08_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_21_ts_idx on device_sensors_par_2015_08_21(ts);
ALTER TABLE device_sensors_par_2015_08_21 ADD CHECK (local_utc_ts >= '2015-08-21 00:00:00' AND local_utc_ts < '2015-08-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_22_uniq_device_id_account_id_ts on device_sensors_par_2015_08_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_22_ts_idx on device_sensors_par_2015_08_22(ts);
ALTER TABLE device_sensors_par_2015_08_22 ADD CHECK (local_utc_ts >= '2015-08-22 00:00:00' AND local_utc_ts < '2015-08-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_23_uniq_device_id_account_id_ts on device_sensors_par_2015_08_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_23_ts_idx on device_sensors_par_2015_08_23(ts);
ALTER TABLE device_sensors_par_2015_08_23 ADD CHECK (local_utc_ts >= '2015-08-23 00:00:00' AND local_utc_ts < '2015-08-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_24_uniq_device_id_account_id_ts on device_sensors_par_2015_08_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_24_ts_idx on device_sensors_par_2015_08_24(ts);
ALTER TABLE device_sensors_par_2015_08_24 ADD CHECK (local_utc_ts >= '2015-08-24 00:00:00' AND local_utc_ts < '2015-08-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_25_uniq_device_id_account_id_ts on device_sensors_par_2015_08_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_25_ts_idx on device_sensors_par_2015_08_25(ts);
ALTER TABLE device_sensors_par_2015_08_25 ADD CHECK (local_utc_ts >= '2015-08-25 00:00:00' AND local_utc_ts < '2015-08-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_26_uniq_device_id_account_id_ts on device_sensors_par_2015_08_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_26_ts_idx on device_sensors_par_2015_08_26(ts);
ALTER TABLE device_sensors_par_2015_08_26 ADD CHECK (local_utc_ts >= '2015-08-26 00:00:00' AND local_utc_ts < '2015-08-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_27_uniq_device_id_account_id_ts on device_sensors_par_2015_08_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_27_ts_idx on device_sensors_par_2015_08_27(ts);
ALTER TABLE device_sensors_par_2015_08_27 ADD CHECK (local_utc_ts >= '2015-08-27 00:00:00' AND local_utc_ts < '2015-08-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_28_uniq_device_id_account_id_ts on device_sensors_par_2015_08_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_28_ts_idx on device_sensors_par_2015_08_28(ts);
ALTER TABLE device_sensors_par_2015_08_28 ADD CHECK (local_utc_ts >= '2015-08-28 00:00:00' AND local_utc_ts < '2015-08-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_29_uniq_device_id_account_id_ts on device_sensors_par_2015_08_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_29_ts_idx on device_sensors_par_2015_08_29(ts);
ALTER TABLE device_sensors_par_2015_08_29 ADD CHECK (local_utc_ts >= '2015-08-29 00:00:00' AND local_utc_ts < '2015-08-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_30_uniq_device_id_account_id_ts on device_sensors_par_2015_08_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_30_ts_idx on device_sensors_par_2015_08_30(ts);
ALTER TABLE device_sensors_par_2015_08_30 ADD CHECK (local_utc_ts >= '2015-08-30 00:00:00' AND local_utc_ts < '2015-08-31 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2015_08_31() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2015_08_31_uniq_device_id_account_id_ts on device_sensors_par_2015_08_31(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2015_08_31_ts_idx on device_sensors_par_2015_08_31(ts);
ALTER TABLE device_sensors_par_2015_08_31 ADD CHECK (local_utc_ts >= '2015-08-31 00:00:00' AND local_utc_ts < '2015-09-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-08-31 00:00:00' AND NEW.local_utc_ts < '2015-09-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-30 00:00:00' AND NEW.local_utc_ts < '2015-08-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-29 00:00:00' AND NEW.local_utc_ts < '2015-08-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-28 00:00:00' AND NEW.local_utc_ts < '2015-08-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-27 00:00:00' AND NEW.local_utc_ts < '2015-08-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-26 00:00:00' AND NEW.local_utc_ts < '2015-08-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-25 00:00:00' AND NEW.local_utc_ts < '2015-08-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-24 00:00:00' AND NEW.local_utc_ts < '2015-08-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-23 00:00:00' AND NEW.local_utc_ts < '2015-08-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-22 00:00:00' AND NEW.local_utc_ts < '2015-08-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-21 00:00:00' AND NEW.local_utc_ts < '2015-08-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-20 00:00:00' AND NEW.local_utc_ts < '2015-08-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-19 00:00:00' AND NEW.local_utc_ts < '2015-08-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-18 00:00:00' AND NEW.local_utc_ts < '2015-08-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-17 00:00:00' AND NEW.local_utc_ts < '2015-08-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-16 00:00:00' AND NEW.local_utc_ts < '2015-08-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-15 00:00:00' AND NEW.local_utc_ts < '2015-08-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-14 00:00:00' AND NEW.local_utc_ts < '2015-08-15 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-13 00:00:00' AND NEW.local_utc_ts < '2015-08-14 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-12 00:00:00' AND NEW.local_utc_ts < '2015-08-13 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-11 00:00:00' AND NEW.local_utc_ts < '2015-08-12 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-10 00:00:00' AND NEW.local_utc_ts < '2015-08-11 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-09 00:00:00' AND NEW.local_utc_ts < '2015-08-10 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-08 00:00:00' AND NEW.local_utc_ts < '2015-08-09 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-07 00:00:00' AND NEW.local_utc_ts < '2015-08-08 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-06 00:00:00' AND NEW.local_utc_ts < '2015-08-07 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-05 00:00:00' AND NEW.local_utc_ts < '2015-08-06 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-04 00:00:00' AND NEW.local_utc_ts < '2015-08-05 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-03 00:00:00' AND NEW.local_utc_ts < '2015-08-04 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-02 00:00:00' AND NEW.local_utc_ts < '2015-08-03 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-08-01 00:00:00' AND NEW.local_utc_ts < '2015-08-02 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_08_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-31 00:00:00' AND NEW.local_utc_ts < '2015-08-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-30 00:00:00' AND NEW.local_utc_ts < '2015-07-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-29 00:00:00' AND NEW.local_utc_ts < '2015-07-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-28 00:00:00' AND NEW.local_utc_ts < '2015-07-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-27 00:00:00' AND NEW.local_utc_ts < '2015-07-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2015_07_27 VALUES (NEW.*);
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2015-08 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2015_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2015_08_uniq_tracker_ts on tracker_motion_par_2015_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2015_08_uniq_tracker_id_account_id_ts on tracker_motion_par_2015_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2015_08_local_utc_ts on tracker_motion_par_2015_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2015_08 ADD CHECK (local_utc_ts >= '2015-08-01 00:00:00' AND local_utc_ts < '2015-09-01 00:00:00');

CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2015-08-01 00:00:00' AND NEW.local_utc_ts < '2015-09-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-01 00:00:00' AND NEW.local_utc_ts < '2015-08-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_07 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;