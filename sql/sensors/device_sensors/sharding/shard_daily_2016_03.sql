
-- 2016-03 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_01_uniq_device_id_account_id_ts on device_sensors_par_2016_03_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_01_ts_idx on device_sensors_par_2016_03_01(ts);
ALTER TABLE device_sensors_par_2016_03_01 ADD CHECK (local_utc_ts >= '2016-03-01 00:00:00' AND local_utc_ts < '2016-03-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_02_uniq_device_id_account_id_ts on device_sensors_par_2016_03_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_02_ts_idx on device_sensors_par_2016_03_02(ts);
ALTER TABLE device_sensors_par_2016_03_02 ADD CHECK (local_utc_ts >= '2016-03-02 00:00:00' AND local_utc_ts < '2016-03-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_03_uniq_device_id_account_id_ts on device_sensors_par_2016_03_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_03_ts_idx on device_sensors_par_2016_03_03(ts);
ALTER TABLE device_sensors_par_2016_03_03 ADD CHECK (local_utc_ts >= '2016-03-03 00:00:00' AND local_utc_ts < '2016-03-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_04_uniq_device_id_account_id_ts on device_sensors_par_2016_03_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_04_ts_idx on device_sensors_par_2016_03_04(ts);
ALTER TABLE device_sensors_par_2016_03_04 ADD CHECK (local_utc_ts >= '2016-03-04 00:00:00' AND local_utc_ts < '2016-03-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_05_uniq_device_id_account_id_ts on device_sensors_par_2016_03_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_05_ts_idx on device_sensors_par_2016_03_05(ts);
ALTER TABLE device_sensors_par_2016_03_05 ADD CHECK (local_utc_ts >= '2016-03-05 00:00:00' AND local_utc_ts < '2016-03-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_06_uniq_device_id_account_id_ts on device_sensors_par_2016_03_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_06_ts_idx on device_sensors_par_2016_03_06(ts);
ALTER TABLE device_sensors_par_2016_03_06 ADD CHECK (local_utc_ts >= '2016-03-06 00:00:00' AND local_utc_ts < '2016-03-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_07_uniq_device_id_account_id_ts on device_sensors_par_2016_03_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_07_ts_idx on device_sensors_par_2016_03_07(ts);
ALTER TABLE device_sensors_par_2016_03_07 ADD CHECK (local_utc_ts >= '2016-03-07 00:00:00' AND local_utc_ts < '2016-03-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_08_uniq_device_id_account_id_ts on device_sensors_par_2016_03_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_08_ts_idx on device_sensors_par_2016_03_08(ts);
ALTER TABLE device_sensors_par_2016_03_08 ADD CHECK (local_utc_ts >= '2016-03-08 00:00:00' AND local_utc_ts < '2016-03-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_09_uniq_device_id_account_id_ts on device_sensors_par_2016_03_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_09_ts_idx on device_sensors_par_2016_03_09(ts);
ALTER TABLE device_sensors_par_2016_03_09 ADD CHECK (local_utc_ts >= '2016-03-09 00:00:00' AND local_utc_ts < '2016-03-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_10_uniq_device_id_account_id_ts on device_sensors_par_2016_03_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_10_ts_idx on device_sensors_par_2016_03_10(ts);
ALTER TABLE device_sensors_par_2016_03_10 ADD CHECK (local_utc_ts >= '2016-03-10 00:00:00' AND local_utc_ts < '2016-03-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_11_uniq_device_id_account_id_ts on device_sensors_par_2016_03_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_11_ts_idx on device_sensors_par_2016_03_11(ts);
ALTER TABLE device_sensors_par_2016_03_11 ADD CHECK (local_utc_ts >= '2016-03-11 00:00:00' AND local_utc_ts < '2016-03-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_12_uniq_device_id_account_id_ts on device_sensors_par_2016_03_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_12_ts_idx on device_sensors_par_2016_03_12(ts);
ALTER TABLE device_sensors_par_2016_03_12 ADD CHECK (local_utc_ts >= '2016-03-12 00:00:00' AND local_utc_ts < '2016-03-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_13_uniq_device_id_account_id_ts on device_sensors_par_2016_03_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_13_ts_idx on device_sensors_par_2016_03_13(ts);
ALTER TABLE device_sensors_par_2016_03_13 ADD CHECK (local_utc_ts >= '2016-03-13 00:00:00' AND local_utc_ts < '2016-03-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_14_uniq_device_id_account_id_ts on device_sensors_par_2016_03_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_14_ts_idx on device_sensors_par_2016_03_14(ts);
ALTER TABLE device_sensors_par_2016_03_14 ADD CHECK (local_utc_ts >= '2016-03-14 00:00:00' AND local_utc_ts < '2016-03-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_15_uniq_device_id_account_id_ts on device_sensors_par_2016_03_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_15_ts_idx on device_sensors_par_2016_03_15(ts);
ALTER TABLE device_sensors_par_2016_03_15 ADD CHECK (local_utc_ts >= '2016-03-15 00:00:00' AND local_utc_ts < '2016-03-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_16_uniq_device_id_account_id_ts on device_sensors_par_2016_03_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_16_ts_idx on device_sensors_par_2016_03_16(ts);
ALTER TABLE device_sensors_par_2016_03_16 ADD CHECK (local_utc_ts >= '2016-03-16 00:00:00' AND local_utc_ts < '2016-03-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_17_uniq_device_id_account_id_ts on device_sensors_par_2016_03_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_17_ts_idx on device_sensors_par_2016_03_17(ts);
ALTER TABLE device_sensors_par_2016_03_17 ADD CHECK (local_utc_ts >= '2016-03-17 00:00:00' AND local_utc_ts < '2016-03-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_18_uniq_device_id_account_id_ts on device_sensors_par_2016_03_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_18_ts_idx on device_sensors_par_2016_03_18(ts);
ALTER TABLE device_sensors_par_2016_03_18 ADD CHECK (local_utc_ts >= '2016-03-18 00:00:00' AND local_utc_ts < '2016-03-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_19_uniq_device_id_account_id_ts on device_sensors_par_2016_03_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_19_ts_idx on device_sensors_par_2016_03_19(ts);
ALTER TABLE device_sensors_par_2016_03_19 ADD CHECK (local_utc_ts >= '2016-03-19 00:00:00' AND local_utc_ts < '2016-03-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_20_uniq_device_id_account_id_ts on device_sensors_par_2016_03_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_20_ts_idx on device_sensors_par_2016_03_20(ts);
ALTER TABLE device_sensors_par_2016_03_20 ADD CHECK (local_utc_ts >= '2016-03-20 00:00:00' AND local_utc_ts < '2016-03-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_21_uniq_device_id_account_id_ts on device_sensors_par_2016_03_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_21_ts_idx on device_sensors_par_2016_03_21(ts);
ALTER TABLE device_sensors_par_2016_03_21 ADD CHECK (local_utc_ts >= '2016-03-21 00:00:00' AND local_utc_ts < '2016-03-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_22_uniq_device_id_account_id_ts on device_sensors_par_2016_03_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_22_ts_idx on device_sensors_par_2016_03_22(ts);
ALTER TABLE device_sensors_par_2016_03_22 ADD CHECK (local_utc_ts >= '2016-03-22 00:00:00' AND local_utc_ts < '2016-03-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_23_uniq_device_id_account_id_ts on device_sensors_par_2016_03_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_23_ts_idx on device_sensors_par_2016_03_23(ts);
ALTER TABLE device_sensors_par_2016_03_23 ADD CHECK (local_utc_ts >= '2016-03-23 00:00:00' AND local_utc_ts < '2016-03-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_24_uniq_device_id_account_id_ts on device_sensors_par_2016_03_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_24_ts_idx on device_sensors_par_2016_03_24(ts);
ALTER TABLE device_sensors_par_2016_03_24 ADD CHECK (local_utc_ts >= '2016-03-24 00:00:00' AND local_utc_ts < '2016-03-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_25_uniq_device_id_account_id_ts on device_sensors_par_2016_03_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_25_ts_idx on device_sensors_par_2016_03_25(ts);
ALTER TABLE device_sensors_par_2016_03_25 ADD CHECK (local_utc_ts >= '2016-03-25 00:00:00' AND local_utc_ts < '2016-03-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_26_uniq_device_id_account_id_ts on device_sensors_par_2016_03_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_26_ts_idx on device_sensors_par_2016_03_26(ts);
ALTER TABLE device_sensors_par_2016_03_26 ADD CHECK (local_utc_ts >= '2016-03-26 00:00:00' AND local_utc_ts < '2016-03-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_27_uniq_device_id_account_id_ts on device_sensors_par_2016_03_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_27_ts_idx on device_sensors_par_2016_03_27(ts);
ALTER TABLE device_sensors_par_2016_03_27 ADD CHECK (local_utc_ts >= '2016-03-27 00:00:00' AND local_utc_ts < '2016-03-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_28_uniq_device_id_account_id_ts on device_sensors_par_2016_03_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_28_ts_idx on device_sensors_par_2016_03_28(ts);
ALTER TABLE device_sensors_par_2016_03_28 ADD CHECK (local_utc_ts >= '2016-03-28 00:00:00' AND local_utc_ts < '2016-03-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_29_uniq_device_id_account_id_ts on device_sensors_par_2016_03_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_29_ts_idx on device_sensors_par_2016_03_29(ts);
ALTER TABLE device_sensors_par_2016_03_29 ADD CHECK (local_utc_ts >= '2016-03-29 00:00:00' AND local_utc_ts < '2016-03-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_30_uniq_device_id_account_id_ts on device_sensors_par_2016_03_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_30_ts_idx on device_sensors_par_2016_03_30(ts);
ALTER TABLE device_sensors_par_2016_03_30 ADD CHECK (local_utc_ts >= '2016-03-30 00:00:00' AND local_utc_ts < '2016-03-31 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_03_31() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_03_31_uniq_device_id_account_id_ts on device_sensors_par_2016_03_31(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_03_31_ts_idx on device_sensors_par_2016_03_31(ts);
ALTER TABLE device_sensors_par_2016_03_31 ADD CHECK (local_utc_ts >= '2016-03-31 00:00:00' AND local_utc_ts < '2016-04-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-03-31 00:00:00' AND NEW.local_utc_ts < '2016-04-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-30 00:00:00' AND NEW.local_utc_ts < '2016-03-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-29 00:00:00' AND NEW.local_utc_ts < '2016-03-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-28 00:00:00' AND NEW.local_utc_ts < '2016-03-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-27 00:00:00' AND NEW.local_utc_ts < '2016-03-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-26 00:00:00' AND NEW.local_utc_ts < '2016-03-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-25 00:00:00' AND NEW.local_utc_ts < '2016-03-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-24 00:00:00' AND NEW.local_utc_ts < '2016-03-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-23 00:00:00' AND NEW.local_utc_ts < '2016-03-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-22 00:00:00' AND NEW.local_utc_ts < '2016-03-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-21 00:00:00' AND NEW.local_utc_ts < '2016-03-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-20 00:00:00' AND NEW.local_utc_ts < '2016-03-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-19 00:00:00' AND NEW.local_utc_ts < '2016-03-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-18 00:00:00' AND NEW.local_utc_ts < '2016-03-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-17 00:00:00' AND NEW.local_utc_ts < '2016-03-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-16 00:00:00' AND NEW.local_utc_ts < '2016-03-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-15 00:00:00' AND NEW.local_utc_ts < '2016-03-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-14 00:00:00' AND NEW.local_utc_ts < '2016-03-15 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-13 00:00:00' AND NEW.local_utc_ts < '2016-03-14 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-12 00:00:00' AND NEW.local_utc_ts < '2016-03-13 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-11 00:00:00' AND NEW.local_utc_ts < '2016-03-12 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-10 00:00:00' AND NEW.local_utc_ts < '2016-03-11 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-09 00:00:00' AND NEW.local_utc_ts < '2016-03-10 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-08 00:00:00' AND NEW.local_utc_ts < '2016-03-09 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-07 00:00:00' AND NEW.local_utc_ts < '2016-03-08 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-06 00:00:00' AND NEW.local_utc_ts < '2016-03-07 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-05 00:00:00' AND NEW.local_utc_ts < '2016-03-06 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-04 00:00:00' AND NEW.local_utc_ts < '2016-03-05 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-03 00:00:00' AND NEW.local_utc_ts < '2016-03-04 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-02 00:00:00' AND NEW.local_utc_ts < '2016-03-03 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-01 00:00:00' AND NEW.local_utc_ts < '2016-03-02 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_03_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-29 00:00:00' AND NEW.local_utc_ts < '2016-03-01 00:00:00' THEN
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
    ELSE
        INSERT INTO device_sensors_par_default VALUES (NEW.*);
    END IF;

    RETURN NULL;
END
$BODY$;


-- 2016-03 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_01() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_01_uniq_tracker_ts ON tracker_motion_par_2016_03_01(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_01_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_01(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_01_local_utc_ts ON tracker_motion_par_2016_03_01(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_01 ADD CHECK (local_utc_ts >= '2016-03-01 00:00:00' AND local_utc_ts < '2016-03-02 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_02() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_02_uniq_tracker_ts ON tracker_motion_par_2016_03_02(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_02_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_02(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_02_local_utc_ts ON tracker_motion_par_2016_03_02(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_02 ADD CHECK (local_utc_ts >= '2016-03-02 00:00:00' AND local_utc_ts < '2016-03-03 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_03() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_03_uniq_tracker_ts ON tracker_motion_par_2016_03_03(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_03_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_03(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_03_local_utc_ts ON tracker_motion_par_2016_03_03(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_03 ADD CHECK (local_utc_ts >= '2016-03-03 00:00:00' AND local_utc_ts < '2016-03-04 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_04() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_04_uniq_tracker_ts ON tracker_motion_par_2016_03_04(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_04_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_04(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_04_local_utc_ts ON tracker_motion_par_2016_03_04(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_04 ADD CHECK (local_utc_ts >= '2016-03-04 00:00:00' AND local_utc_ts < '2016-03-05 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_05() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_05_uniq_tracker_ts ON tracker_motion_par_2016_03_05(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_05_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_05(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_05_local_utc_ts ON tracker_motion_par_2016_03_05(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_05 ADD CHECK (local_utc_ts >= '2016-03-05 00:00:00' AND local_utc_ts < '2016-03-06 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_06() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_06_uniq_tracker_ts ON tracker_motion_par_2016_03_06(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_06_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_06(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_06_local_utc_ts ON tracker_motion_par_2016_03_06(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_06 ADD CHECK (local_utc_ts >= '2016-03-06 00:00:00' AND local_utc_ts < '2016-03-07 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_07_uniq_tracker_ts ON tracker_motion_par_2016_03_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_07_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_07_local_utc_ts ON tracker_motion_par_2016_03_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_07 ADD CHECK (local_utc_ts >= '2016-03-07 00:00:00' AND local_utc_ts < '2016-03-08 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_08_uniq_tracker_ts ON tracker_motion_par_2016_03_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_08_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_08_local_utc_ts ON tracker_motion_par_2016_03_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_08 ADD CHECK (local_utc_ts >= '2016-03-08 00:00:00' AND local_utc_ts < '2016-03-09 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_09() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_09_uniq_tracker_ts ON tracker_motion_par_2016_03_09(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_09_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_09(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_09_local_utc_ts ON tracker_motion_par_2016_03_09(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_09 ADD CHECK (local_utc_ts >= '2016-03-09 00:00:00' AND local_utc_ts < '2016-03-10 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_10() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_10_uniq_tracker_ts ON tracker_motion_par_2016_03_10(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_10_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_10(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_10_local_utc_ts ON tracker_motion_par_2016_03_10(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_10 ADD CHECK (local_utc_ts >= '2016-03-10 00:00:00' AND local_utc_ts < '2016-03-11 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_11() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_11_uniq_tracker_ts ON tracker_motion_par_2016_03_11(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_11_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_11(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_11_local_utc_ts ON tracker_motion_par_2016_03_11(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_11 ADD CHECK (local_utc_ts >= '2016-03-11 00:00:00' AND local_utc_ts < '2016-03-12 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_12() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_12_uniq_tracker_ts ON tracker_motion_par_2016_03_12(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_12_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_12(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_12_local_utc_ts ON tracker_motion_par_2016_03_12(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_12 ADD CHECK (local_utc_ts >= '2016-03-12 00:00:00' AND local_utc_ts < '2016-03-13 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_13() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_13_uniq_tracker_ts ON tracker_motion_par_2016_03_13(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_13_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_13(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_13_local_utc_ts ON tracker_motion_par_2016_03_13(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_13 ADD CHECK (local_utc_ts >= '2016-03-13 00:00:00' AND local_utc_ts < '2016-03-14 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_14() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_14_uniq_tracker_ts ON tracker_motion_par_2016_03_14(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_14_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_14(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_14_local_utc_ts ON tracker_motion_par_2016_03_14(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_14 ADD CHECK (local_utc_ts >= '2016-03-14 00:00:00' AND local_utc_ts < '2016-03-15 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_15() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_15_uniq_tracker_ts ON tracker_motion_par_2016_03_15(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_15_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_15(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_15_local_utc_ts ON tracker_motion_par_2016_03_15(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_15 ADD CHECK (local_utc_ts >= '2016-03-15 00:00:00' AND local_utc_ts < '2016-03-16 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_16() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_16_uniq_tracker_ts ON tracker_motion_par_2016_03_16(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_16_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_16(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_16_local_utc_ts ON tracker_motion_par_2016_03_16(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_16 ADD CHECK (local_utc_ts >= '2016-03-16 00:00:00' AND local_utc_ts < '2016-03-17 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_17() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_17_uniq_tracker_ts ON tracker_motion_par_2016_03_17(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_17_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_17(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_17_local_utc_ts ON tracker_motion_par_2016_03_17(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_17 ADD CHECK (local_utc_ts >= '2016-03-17 00:00:00' AND local_utc_ts < '2016-03-18 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_18() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_18_uniq_tracker_ts ON tracker_motion_par_2016_03_18(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_18_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_18(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_18_local_utc_ts ON tracker_motion_par_2016_03_18(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_18 ADD CHECK (local_utc_ts >= '2016-03-18 00:00:00' AND local_utc_ts < '2016-03-19 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_19() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_19_uniq_tracker_ts ON tracker_motion_par_2016_03_19(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_19_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_19(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_19_local_utc_ts ON tracker_motion_par_2016_03_19(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_19 ADD CHECK (local_utc_ts >= '2016-03-19 00:00:00' AND local_utc_ts < '2016-03-20 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_20() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_20_uniq_tracker_ts ON tracker_motion_par_2016_03_20(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_20_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_20(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_20_local_utc_ts ON tracker_motion_par_2016_03_20(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_20 ADD CHECK (local_utc_ts >= '2016-03-20 00:00:00' AND local_utc_ts < '2016-03-21 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_21() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_21_uniq_tracker_ts ON tracker_motion_par_2016_03_21(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_21_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_21(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_21_local_utc_ts ON tracker_motion_par_2016_03_21(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_21 ADD CHECK (local_utc_ts >= '2016-03-21 00:00:00' AND local_utc_ts < '2016-03-22 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_22() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_22_uniq_tracker_ts ON tracker_motion_par_2016_03_22(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_22_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_22(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_22_local_utc_ts ON tracker_motion_par_2016_03_22(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_22 ADD CHECK (local_utc_ts >= '2016-03-22 00:00:00' AND local_utc_ts < '2016-03-23 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_23() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_23_uniq_tracker_ts ON tracker_motion_par_2016_03_23(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_23_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_23(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_23_local_utc_ts ON tracker_motion_par_2016_03_23(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_23 ADD CHECK (local_utc_ts >= '2016-03-23 00:00:00' AND local_utc_ts < '2016-03-24 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_24() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_24_uniq_tracker_ts ON tracker_motion_par_2016_03_24(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_24_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_24(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_24_local_utc_ts ON tracker_motion_par_2016_03_24(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_24 ADD CHECK (local_utc_ts >= '2016-03-24 00:00:00' AND local_utc_ts < '2016-03-25 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_25() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_25_uniq_tracker_ts ON tracker_motion_par_2016_03_25(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_25_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_25(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_25_local_utc_ts ON tracker_motion_par_2016_03_25(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_25 ADD CHECK (local_utc_ts >= '2016-03-25 00:00:00' AND local_utc_ts < '2016-03-26 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_26() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_26_uniq_tracker_ts ON tracker_motion_par_2016_03_26(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_26_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_26(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_26_local_utc_ts ON tracker_motion_par_2016_03_26(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_26 ADD CHECK (local_utc_ts >= '2016-03-26 00:00:00' AND local_utc_ts < '2016-03-27 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_27() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_27_uniq_tracker_ts ON tracker_motion_par_2016_03_27(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_27_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_27(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_27_local_utc_ts ON tracker_motion_par_2016_03_27(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_27 ADD CHECK (local_utc_ts >= '2016-03-27 00:00:00' AND local_utc_ts < '2016-03-28 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_28() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_28_uniq_tracker_ts ON tracker_motion_par_2016_03_28(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_28_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_28(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_28_local_utc_ts ON tracker_motion_par_2016_03_28(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_28 ADD CHECK (local_utc_ts >= '2016-03-28 00:00:00' AND local_utc_ts < '2016-03-29 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_29() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_29_uniq_tracker_ts ON tracker_motion_par_2016_03_29(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_29_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_29(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_29_local_utc_ts ON tracker_motion_par_2016_03_29(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_29 ADD CHECK (local_utc_ts >= '2016-03-29 00:00:00' AND local_utc_ts < '2016-03-30 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_30() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_30_uniq_tracker_ts ON tracker_motion_par_2016_03_30(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_30_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_30(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_30_local_utc_ts ON tracker_motion_par_2016_03_30(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_30 ADD CHECK (local_utc_ts >= '2016-03-30 00:00:00' AND local_utc_ts < '2016-03-31 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_03_31() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_31_uniq_tracker_ts ON tracker_motion_par_2016_03_31(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_03_31_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_03_31(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_03_31_local_utc_ts ON tracker_motion_par_2016_03_31(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_03_31 ADD CHECK (local_utc_ts >= '2016-03-31 00:00:00' AND local_utc_ts < '2016-04-01 00:00:00');




CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-03-31 00:00:00' AND NEW.local_utc_ts < '2016-04-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-30 00:00:00' AND NEW.local_utc_ts < '2016-03-31 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-29 00:00:00' AND NEW.local_utc_ts < '2016-03-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-28 00:00:00' AND NEW.local_utc_ts < '2016-03-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-27 00:00:00' AND NEW.local_utc_ts < '2016-03-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-26 00:00:00' AND NEW.local_utc_ts < '2016-03-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-25 00:00:00' AND NEW.local_utc_ts < '2016-03-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-24 00:00:00' AND NEW.local_utc_ts < '2016-03-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-23 00:00:00' AND NEW.local_utc_ts < '2016-03-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-22 00:00:00' AND NEW.local_utc_ts < '2016-03-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-21 00:00:00' AND NEW.local_utc_ts < '2016-03-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-20 00:00:00' AND NEW.local_utc_ts < '2016-03-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-19 00:00:00' AND NEW.local_utc_ts < '2016-03-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-18 00:00:00' AND NEW.local_utc_ts < '2016-03-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-17 00:00:00' AND NEW.local_utc_ts < '2016-03-18 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-16 00:00:00' AND NEW.local_utc_ts < '2016-03-17 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-15 00:00:00' AND NEW.local_utc_ts < '2016-03-16 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-14 00:00:00' AND NEW.local_utc_ts < '2016-03-15 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-13 00:00:00' AND NEW.local_utc_ts < '2016-03-14 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-12 00:00:00' AND NEW.local_utc_ts < '2016-03-13 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-11 00:00:00' AND NEW.local_utc_ts < '2016-03-12 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-10 00:00:00' AND NEW.local_utc_ts < '2016-03-11 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-09 00:00:00' AND NEW.local_utc_ts < '2016-03-10 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-08 00:00:00' AND NEW.local_utc_ts < '2016-03-09 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-07 00:00:00' AND NEW.local_utc_ts < '2016-03-08 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-06 00:00:00' AND NEW.local_utc_ts < '2016-03-07 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-05 00:00:00' AND NEW.local_utc_ts < '2016-03-06 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-04 00:00:00' AND NEW.local_utc_ts < '2016-03-05 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-03 00:00:00' AND NEW.local_utc_ts < '2016-03-04 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-02 00:00:00' AND NEW.local_utc_ts < '2016-03-03 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-03-01 00:00:00' AND NEW.local_utc_ts < '2016-03-02 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_03_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-02-29 00:00:00' AND NEW.local_utc_ts < '2016-03-01 00:00:00' THEN
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
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;
