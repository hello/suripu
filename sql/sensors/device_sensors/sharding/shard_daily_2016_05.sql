
-- 2016-05 shard for device_sensors

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_01() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_01_uniq_device_id_account_id_ts on device_sensors_par_2016_05_01(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_01_ts_idx on device_sensors_par_2016_05_01(ts);
ALTER TABLE device_sensors_par_2016_05_01 ADD CHECK (local_utc_ts >= '2016-05-01 00:00:00' AND local_utc_ts < '2016-05-02 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_02() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_02_uniq_device_id_account_id_ts on device_sensors_par_2016_05_02(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_02_ts_idx on device_sensors_par_2016_05_02(ts);
ALTER TABLE device_sensors_par_2016_05_02 ADD CHECK (local_utc_ts >= '2016-05-02 00:00:00' AND local_utc_ts < '2016-05-03 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_03() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_03_uniq_device_id_account_id_ts on device_sensors_par_2016_05_03(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_03_ts_idx on device_sensors_par_2016_05_03(ts);
ALTER TABLE device_sensors_par_2016_05_03 ADD CHECK (local_utc_ts >= '2016-05-03 00:00:00' AND local_utc_ts < '2016-05-04 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_04() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_04_uniq_device_id_account_id_ts on device_sensors_par_2016_05_04(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_04_ts_idx on device_sensors_par_2016_05_04(ts);
ALTER TABLE device_sensors_par_2016_05_04 ADD CHECK (local_utc_ts >= '2016-05-04 00:00:00' AND local_utc_ts < '2016-05-05 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_05() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_05_uniq_device_id_account_id_ts on device_sensors_par_2016_05_05(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_05_ts_idx on device_sensors_par_2016_05_05(ts);
ALTER TABLE device_sensors_par_2016_05_05 ADD CHECK (local_utc_ts >= '2016-05-05 00:00:00' AND local_utc_ts < '2016-05-06 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_06() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_06_uniq_device_id_account_id_ts on device_sensors_par_2016_05_06(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_06_ts_idx on device_sensors_par_2016_05_06(ts);
ALTER TABLE device_sensors_par_2016_05_06 ADD CHECK (local_utc_ts >= '2016-05-06 00:00:00' AND local_utc_ts < '2016-05-07 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_07() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_07_uniq_device_id_account_id_ts on device_sensors_par_2016_05_07(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_07_ts_idx on device_sensors_par_2016_05_07(ts);
ALTER TABLE device_sensors_par_2016_05_07 ADD CHECK (local_utc_ts >= '2016-05-07 00:00:00' AND local_utc_ts < '2016-05-08 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_08() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_08_uniq_device_id_account_id_ts on device_sensors_par_2016_05_08(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_08_ts_idx on device_sensors_par_2016_05_08(ts);
ALTER TABLE device_sensors_par_2016_05_08 ADD CHECK (local_utc_ts >= '2016-05-08 00:00:00' AND local_utc_ts < '2016-05-09 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_09() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_09_uniq_device_id_account_id_ts on device_sensors_par_2016_05_09(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_09_ts_idx on device_sensors_par_2016_05_09(ts);
ALTER TABLE device_sensors_par_2016_05_09 ADD CHECK (local_utc_ts >= '2016-05-09 00:00:00' AND local_utc_ts < '2016-05-10 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_10() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_10_uniq_device_id_account_id_ts on device_sensors_par_2016_05_10(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_10_ts_idx on device_sensors_par_2016_05_10(ts);
ALTER TABLE device_sensors_par_2016_05_10 ADD CHECK (local_utc_ts >= '2016-05-10 00:00:00' AND local_utc_ts < '2016-05-11 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_11() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_11_uniq_device_id_account_id_ts on device_sensors_par_2016_05_11(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_11_ts_idx on device_sensors_par_2016_05_11(ts);
ALTER TABLE device_sensors_par_2016_05_11 ADD CHECK (local_utc_ts >= '2016-05-11 00:00:00' AND local_utc_ts < '2016-05-12 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_12() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_12_uniq_device_id_account_id_ts on device_sensors_par_2016_05_12(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_12_ts_idx on device_sensors_par_2016_05_12(ts);
ALTER TABLE device_sensors_par_2016_05_12 ADD CHECK (local_utc_ts >= '2016-05-12 00:00:00' AND local_utc_ts < '2016-05-13 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_13() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_13_uniq_device_id_account_id_ts on device_sensors_par_2016_05_13(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_13_ts_idx on device_sensors_par_2016_05_13(ts);
ALTER TABLE device_sensors_par_2016_05_13 ADD CHECK (local_utc_ts >= '2016-05-13 00:00:00' AND local_utc_ts < '2016-05-14 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_14() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_14_uniq_device_id_account_id_ts on device_sensors_par_2016_05_14(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_14_ts_idx on device_sensors_par_2016_05_14(ts);
ALTER TABLE device_sensors_par_2016_05_14 ADD CHECK (local_utc_ts >= '2016-05-14 00:00:00' AND local_utc_ts < '2016-05-15 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_15() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_15_uniq_device_id_account_id_ts on device_sensors_par_2016_05_15(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_15_ts_idx on device_sensors_par_2016_05_15(ts);
ALTER TABLE device_sensors_par_2016_05_15 ADD CHECK (local_utc_ts >= '2016-05-15 00:00:00' AND local_utc_ts < '2016-05-16 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_16() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_16_uniq_device_id_account_id_ts on device_sensors_par_2016_05_16(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_16_ts_idx on device_sensors_par_2016_05_16(ts);
ALTER TABLE device_sensors_par_2016_05_16 ADD CHECK (local_utc_ts >= '2016-05-16 00:00:00' AND local_utc_ts < '2016-05-17 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_17() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_17_uniq_device_id_account_id_ts on device_sensors_par_2016_05_17(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_17_ts_idx on device_sensors_par_2016_05_17(ts);
ALTER TABLE device_sensors_par_2016_05_17 ADD CHECK (local_utc_ts >= '2016-05-17 00:00:00' AND local_utc_ts < '2016-05-18 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_18() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_18_uniq_device_id_account_id_ts on device_sensors_par_2016_05_18(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_18_ts_idx on device_sensors_par_2016_05_18(ts);
ALTER TABLE device_sensors_par_2016_05_18 ADD CHECK (local_utc_ts >= '2016-05-18 00:00:00' AND local_utc_ts < '2016-05-19 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_19() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_19_uniq_device_id_account_id_ts on device_sensors_par_2016_05_19(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_19_ts_idx on device_sensors_par_2016_05_19(ts);
ALTER TABLE device_sensors_par_2016_05_19 ADD CHECK (local_utc_ts >= '2016-05-19 00:00:00' AND local_utc_ts < '2016-05-20 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_20() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_20_uniq_device_id_account_id_ts on device_sensors_par_2016_05_20(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_20_ts_idx on device_sensors_par_2016_05_20(ts);
ALTER TABLE device_sensors_par_2016_05_20 ADD CHECK (local_utc_ts >= '2016-05-20 00:00:00' AND local_utc_ts < '2016-05-21 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_21() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_21_uniq_device_id_account_id_ts on device_sensors_par_2016_05_21(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_21_ts_idx on device_sensors_par_2016_05_21(ts);
ALTER TABLE device_sensors_par_2016_05_21 ADD CHECK (local_utc_ts >= '2016-05-21 00:00:00' AND local_utc_ts < '2016-05-22 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_22() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_22_uniq_device_id_account_id_ts on device_sensors_par_2016_05_22(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_22_ts_idx on device_sensors_par_2016_05_22(ts);
ALTER TABLE device_sensors_par_2016_05_22 ADD CHECK (local_utc_ts >= '2016-05-22 00:00:00' AND local_utc_ts < '2016-05-23 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_23() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_23_uniq_device_id_account_id_ts on device_sensors_par_2016_05_23(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_23_ts_idx on device_sensors_par_2016_05_23(ts);
ALTER TABLE device_sensors_par_2016_05_23 ADD CHECK (local_utc_ts >= '2016-05-23 00:00:00' AND local_utc_ts < '2016-05-24 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_24() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_24_uniq_device_id_account_id_ts on device_sensors_par_2016_05_24(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_24_ts_idx on device_sensors_par_2016_05_24(ts);
ALTER TABLE device_sensors_par_2016_05_24 ADD CHECK (local_utc_ts >= '2016-05-24 00:00:00' AND local_utc_ts < '2016-05-25 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_25() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_25_uniq_device_id_account_id_ts on device_sensors_par_2016_05_25(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_25_ts_idx on device_sensors_par_2016_05_25(ts);
ALTER TABLE device_sensors_par_2016_05_25 ADD CHECK (local_utc_ts >= '2016-05-25 00:00:00' AND local_utc_ts < '2016-05-26 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_26() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_26_uniq_device_id_account_id_ts on device_sensors_par_2016_05_26(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_26_ts_idx on device_sensors_par_2016_05_26(ts);
ALTER TABLE device_sensors_par_2016_05_26 ADD CHECK (local_utc_ts >= '2016-05-26 00:00:00' AND local_utc_ts < '2016-05-27 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_27() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_27_uniq_device_id_account_id_ts on device_sensors_par_2016_05_27(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_27_ts_idx on device_sensors_par_2016_05_27(ts);
ALTER TABLE device_sensors_par_2016_05_27 ADD CHECK (local_utc_ts >= '2016-05-27 00:00:00' AND local_utc_ts < '2016-05-28 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_28() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_28_uniq_device_id_account_id_ts on device_sensors_par_2016_05_28(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_28_ts_idx on device_sensors_par_2016_05_28(ts);
ALTER TABLE device_sensors_par_2016_05_28 ADD CHECK (local_utc_ts >= '2016-05-28 00:00:00' AND local_utc_ts < '2016-05-29 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_29() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_29_uniq_device_id_account_id_ts on device_sensors_par_2016_05_29(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_29_ts_idx on device_sensors_par_2016_05_29(ts);
ALTER TABLE device_sensors_par_2016_05_29 ADD CHECK (local_utc_ts >= '2016-05-29 00:00:00' AND local_utc_ts < '2016-05-30 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_30() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_30_uniq_device_id_account_id_ts on device_sensors_par_2016_05_30(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_30_ts_idx on device_sensors_par_2016_05_30(ts);
ALTER TABLE device_sensors_par_2016_05_30 ADD CHECK (local_utc_ts >= '2016-05-30 00:00:00' AND local_utc_ts < '2016-05-31 00:00:00');

CREATE TABLE IF NOT EXISTS device_sensors_par_2016_05_31() INHERITS (device_sensors_master);
CREATE UNIQUE INDEX device_sensors_par_2016_05_31_uniq_device_id_account_id_ts on device_sensors_par_2016_05_31(device_id, account_id, ts);
CREATE INDEX device_sensors_par_2016_05_31_ts_idx on device_sensors_par_2016_05_31(ts);
ALTER TABLE device_sensors_par_2016_05_31 ADD CHECK (local_utc_ts >= '2016-05-31 00:00:00' AND local_utc_ts < '2016-06-01 00:00:00');



CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-05-31 00:00:00' AND NEW.local_utc_ts < '2016-06-01 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-30 00:00:00' AND NEW.local_utc_ts < '2016-05-31 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-29 00:00:00' AND NEW.local_utc_ts < '2016-05-30 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-28 00:00:00' AND NEW.local_utc_ts < '2016-05-29 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-27 00:00:00' AND NEW.local_utc_ts < '2016-05-28 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-26 00:00:00' AND NEW.local_utc_ts < '2016-05-27 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-25 00:00:00' AND NEW.local_utc_ts < '2016-05-26 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-24 00:00:00' AND NEW.local_utc_ts < '2016-05-25 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-23 00:00:00' AND NEW.local_utc_ts < '2016-05-24 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-22 00:00:00' AND NEW.local_utc_ts < '2016-05-23 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-21 00:00:00' AND NEW.local_utc_ts < '2016-05-22 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-20 00:00:00' AND NEW.local_utc_ts < '2016-05-21 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-19 00:00:00' AND NEW.local_utc_ts < '2016-05-20 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-18 00:00:00' AND NEW.local_utc_ts < '2016-05-19 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-17 00:00:00' AND NEW.local_utc_ts < '2016-05-18 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-16 00:00:00' AND NEW.local_utc_ts < '2016-05-17 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-15 00:00:00' AND NEW.local_utc_ts < '2016-05-16 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-14 00:00:00' AND NEW.local_utc_ts < '2016-05-15 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-13 00:00:00' AND NEW.local_utc_ts < '2016-05-14 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-12 00:00:00' AND NEW.local_utc_ts < '2016-05-13 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-11 00:00:00' AND NEW.local_utc_ts < '2016-05-12 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-10 00:00:00' AND NEW.local_utc_ts < '2016-05-11 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-09 00:00:00' AND NEW.local_utc_ts < '2016-05-10 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-08 00:00:00' AND NEW.local_utc_ts < '2016-05-09 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-07 00:00:00' AND NEW.local_utc_ts < '2016-05-08 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-06 00:00:00' AND NEW.local_utc_ts < '2016-05-07 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-05 00:00:00' AND NEW.local_utc_ts < '2016-05-06 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-04 00:00:00' AND NEW.local_utc_ts < '2016-05-05 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-03 00:00:00' AND NEW.local_utc_ts < '2016-05-04 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-02 00:00:00' AND NEW.local_utc_ts < '2016-05-03 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-01 00:00:00' AND NEW.local_utc_ts < '2016-05-02 00:00:00' THEN
        INSERT INTO device_sensors_par_2016_05_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-30 00:00:00' AND NEW.local_utc_ts < '2016-05-01 00:00:00' THEN
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


-- 2016-05 shard for tracker_motion

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_01() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_01_uniq_tracker_ts ON tracker_motion_par_2016_05_01(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_01_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_01(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_01_local_utc_ts ON tracker_motion_par_2016_05_01(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_01 ADD CHECK (local_utc_ts >= '2016-05-01 00:00:00' AND local_utc_ts < '2016-05-02 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_02() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_02_uniq_tracker_ts ON tracker_motion_par_2016_05_02(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_02_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_02(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_02_local_utc_ts ON tracker_motion_par_2016_05_02(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_02 ADD CHECK (local_utc_ts >= '2016-05-02 00:00:00' AND local_utc_ts < '2016-05-03 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_03() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_03_uniq_tracker_ts ON tracker_motion_par_2016_05_03(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_03_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_03(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_03_local_utc_ts ON tracker_motion_par_2016_05_03(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_03 ADD CHECK (local_utc_ts >= '2016-05-03 00:00:00' AND local_utc_ts < '2016-05-04 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_04() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_04_uniq_tracker_ts ON tracker_motion_par_2016_05_04(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_04_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_04(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_04_local_utc_ts ON tracker_motion_par_2016_05_04(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_04 ADD CHECK (local_utc_ts >= '2016-05-04 00:00:00' AND local_utc_ts < '2016-05-05 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_05() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_05_uniq_tracker_ts ON tracker_motion_par_2016_05_05(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_05_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_05(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_05_local_utc_ts ON tracker_motion_par_2016_05_05(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_05 ADD CHECK (local_utc_ts >= '2016-05-05 00:00:00' AND local_utc_ts < '2016-05-06 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_06() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_06_uniq_tracker_ts ON tracker_motion_par_2016_05_06(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_06_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_06(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_06_local_utc_ts ON tracker_motion_par_2016_05_06(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_06 ADD CHECK (local_utc_ts >= '2016-05-06 00:00:00' AND local_utc_ts < '2016-05-07 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_07() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_07_uniq_tracker_ts ON tracker_motion_par_2016_05_07(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_07_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_07(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_07_local_utc_ts ON tracker_motion_par_2016_05_07(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_07 ADD CHECK (local_utc_ts >= '2016-05-07 00:00:00' AND local_utc_ts < '2016-05-08 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_08() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_08_uniq_tracker_ts ON tracker_motion_par_2016_05_08(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_08_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_08(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_08_local_utc_ts ON tracker_motion_par_2016_05_08(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_08 ADD CHECK (local_utc_ts >= '2016-05-08 00:00:00' AND local_utc_ts < '2016-05-09 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_09() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_09_uniq_tracker_ts ON tracker_motion_par_2016_05_09(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_09_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_09(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_09_local_utc_ts ON tracker_motion_par_2016_05_09(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_09 ADD CHECK (local_utc_ts >= '2016-05-09 00:00:00' AND local_utc_ts < '2016-05-10 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_10() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_10_uniq_tracker_ts ON tracker_motion_par_2016_05_10(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_10_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_10(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_10_local_utc_ts ON tracker_motion_par_2016_05_10(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_10 ADD CHECK (local_utc_ts >= '2016-05-10 00:00:00' AND local_utc_ts < '2016-05-11 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_11() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_11_uniq_tracker_ts ON tracker_motion_par_2016_05_11(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_11_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_11(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_11_local_utc_ts ON tracker_motion_par_2016_05_11(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_11 ADD CHECK (local_utc_ts >= '2016-05-11 00:00:00' AND local_utc_ts < '2016-05-12 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_12() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_12_uniq_tracker_ts ON tracker_motion_par_2016_05_12(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_12_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_12(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_12_local_utc_ts ON tracker_motion_par_2016_05_12(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_12 ADD CHECK (local_utc_ts >= '2016-05-12 00:00:00' AND local_utc_ts < '2016-05-13 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_13() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_13_uniq_tracker_ts ON tracker_motion_par_2016_05_13(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_13_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_13(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_13_local_utc_ts ON tracker_motion_par_2016_05_13(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_13 ADD CHECK (local_utc_ts >= '2016-05-13 00:00:00' AND local_utc_ts < '2016-05-14 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_14() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_14_uniq_tracker_ts ON tracker_motion_par_2016_05_14(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_14_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_14(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_14_local_utc_ts ON tracker_motion_par_2016_05_14(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_14 ADD CHECK (local_utc_ts >= '2016-05-14 00:00:00' AND local_utc_ts < '2016-05-15 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_15() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_15_uniq_tracker_ts ON tracker_motion_par_2016_05_15(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_15_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_15(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_15_local_utc_ts ON tracker_motion_par_2016_05_15(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_15 ADD CHECK (local_utc_ts >= '2016-05-15 00:00:00' AND local_utc_ts < '2016-05-16 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_16() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_16_uniq_tracker_ts ON tracker_motion_par_2016_05_16(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_16_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_16(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_16_local_utc_ts ON tracker_motion_par_2016_05_16(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_16 ADD CHECK (local_utc_ts >= '2016-05-16 00:00:00' AND local_utc_ts < '2016-05-17 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_17() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_17_uniq_tracker_ts ON tracker_motion_par_2016_05_17(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_17_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_17(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_17_local_utc_ts ON tracker_motion_par_2016_05_17(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_17 ADD CHECK (local_utc_ts >= '2016-05-17 00:00:00' AND local_utc_ts < '2016-05-18 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_18() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_18_uniq_tracker_ts ON tracker_motion_par_2016_05_18(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_18_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_18(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_18_local_utc_ts ON tracker_motion_par_2016_05_18(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_18 ADD CHECK (local_utc_ts >= '2016-05-18 00:00:00' AND local_utc_ts < '2016-05-19 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_19() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_19_uniq_tracker_ts ON tracker_motion_par_2016_05_19(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_19_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_19(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_19_local_utc_ts ON tracker_motion_par_2016_05_19(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_19 ADD CHECK (local_utc_ts >= '2016-05-19 00:00:00' AND local_utc_ts < '2016-05-20 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_20() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_20_uniq_tracker_ts ON tracker_motion_par_2016_05_20(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_20_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_20(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_20_local_utc_ts ON tracker_motion_par_2016_05_20(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_20 ADD CHECK (local_utc_ts >= '2016-05-20 00:00:00' AND local_utc_ts < '2016-05-21 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_21() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_21_uniq_tracker_ts ON tracker_motion_par_2016_05_21(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_21_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_21(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_21_local_utc_ts ON tracker_motion_par_2016_05_21(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_21 ADD CHECK (local_utc_ts >= '2016-05-21 00:00:00' AND local_utc_ts < '2016-05-22 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_22() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_22_uniq_tracker_ts ON tracker_motion_par_2016_05_22(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_22_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_22(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_22_local_utc_ts ON tracker_motion_par_2016_05_22(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_22 ADD CHECK (local_utc_ts >= '2016-05-22 00:00:00' AND local_utc_ts < '2016-05-23 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_23() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_23_uniq_tracker_ts ON tracker_motion_par_2016_05_23(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_23_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_23(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_23_local_utc_ts ON tracker_motion_par_2016_05_23(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_23 ADD CHECK (local_utc_ts >= '2016-05-23 00:00:00' AND local_utc_ts < '2016-05-24 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_24() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_24_uniq_tracker_ts ON tracker_motion_par_2016_05_24(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_24_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_24(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_24_local_utc_ts ON tracker_motion_par_2016_05_24(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_24 ADD CHECK (local_utc_ts >= '2016-05-24 00:00:00' AND local_utc_ts < '2016-05-25 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_25() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_25_uniq_tracker_ts ON tracker_motion_par_2016_05_25(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_25_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_25(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_25_local_utc_ts ON tracker_motion_par_2016_05_25(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_25 ADD CHECK (local_utc_ts >= '2016-05-25 00:00:00' AND local_utc_ts < '2016-05-26 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_26() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_26_uniq_tracker_ts ON tracker_motion_par_2016_05_26(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_26_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_26(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_26_local_utc_ts ON tracker_motion_par_2016_05_26(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_26 ADD CHECK (local_utc_ts >= '2016-05-26 00:00:00' AND local_utc_ts < '2016-05-27 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_27() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_27_uniq_tracker_ts ON tracker_motion_par_2016_05_27(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_27_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_27(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_27_local_utc_ts ON tracker_motion_par_2016_05_27(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_27 ADD CHECK (local_utc_ts >= '2016-05-27 00:00:00' AND local_utc_ts < '2016-05-28 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_28() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_28_uniq_tracker_ts ON tracker_motion_par_2016_05_28(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_28_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_28(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_28_local_utc_ts ON tracker_motion_par_2016_05_28(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_28 ADD CHECK (local_utc_ts >= '2016-05-28 00:00:00' AND local_utc_ts < '2016-05-29 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_29() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_29_uniq_tracker_ts ON tracker_motion_par_2016_05_29(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_29_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_29(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_29_local_utc_ts ON tracker_motion_par_2016_05_29(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_29 ADD CHECK (local_utc_ts >= '2016-05-29 00:00:00' AND local_utc_ts < '2016-05-30 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_30() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_30_uniq_tracker_ts ON tracker_motion_par_2016_05_30(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_30_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_30(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_30_local_utc_ts ON tracker_motion_par_2016_05_30(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_30 ADD CHECK (local_utc_ts >= '2016-05-30 00:00:00' AND local_utc_ts < '2016-05-31 00:00:00');

CREATE TABLE IF NOT EXISTS tracker_motion_par_2016_05_31() INHERITS (tracker_motion_master);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_31_uniq_tracker_ts ON tracker_motion_par_2016_05_31(tracker_id, ts);
CREATE UNIQUE INDEX tracker_motion_par_2016_05_31_uniq_tracker_id_account_id_ts ON tracker_motion_par_2016_05_31(tracker_id, account_id, ts);
CREATE INDEX tracker_motion_par_2016_05_31_local_utc_ts ON tracker_motion_par_2016_05_31(local_utc_ts);
ALTER TABLE tracker_motion_par_2016_05_31 ADD CHECK (local_utc_ts >= '2016-05-31 00:00:00' AND local_utc_ts < '2016-06-01 00:00:00');




CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN
    IF NEW.local_utc_ts >= '2016-05-31 00:00:00' AND NEW.local_utc_ts < '2016-06-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_31 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-30 00:00:00' AND NEW.local_utc_ts < '2016-05-31 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_30 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-29 00:00:00' AND NEW.local_utc_ts < '2016-05-30 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_29 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-28 00:00:00' AND NEW.local_utc_ts < '2016-05-29 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_28 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-27 00:00:00' AND NEW.local_utc_ts < '2016-05-28 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_27 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-26 00:00:00' AND NEW.local_utc_ts < '2016-05-27 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_26 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-25 00:00:00' AND NEW.local_utc_ts < '2016-05-26 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_25 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-24 00:00:00' AND NEW.local_utc_ts < '2016-05-25 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_24 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-23 00:00:00' AND NEW.local_utc_ts < '2016-05-24 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_23 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-22 00:00:00' AND NEW.local_utc_ts < '2016-05-23 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_22 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-21 00:00:00' AND NEW.local_utc_ts < '2016-05-22 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_21 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-20 00:00:00' AND NEW.local_utc_ts < '2016-05-21 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_20 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-19 00:00:00' AND NEW.local_utc_ts < '2016-05-20 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_19 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-18 00:00:00' AND NEW.local_utc_ts < '2016-05-19 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_18 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-17 00:00:00' AND NEW.local_utc_ts < '2016-05-18 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_17 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-16 00:00:00' AND NEW.local_utc_ts < '2016-05-17 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_16 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-15 00:00:00' AND NEW.local_utc_ts < '2016-05-16 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_15 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-14 00:00:00' AND NEW.local_utc_ts < '2016-05-15 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_14 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-13 00:00:00' AND NEW.local_utc_ts < '2016-05-14 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_13 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-12 00:00:00' AND NEW.local_utc_ts < '2016-05-13 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_12 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-11 00:00:00' AND NEW.local_utc_ts < '2016-05-12 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_11 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-10 00:00:00' AND NEW.local_utc_ts < '2016-05-11 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_10 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-09 00:00:00' AND NEW.local_utc_ts < '2016-05-10 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_09 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-08 00:00:00' AND NEW.local_utc_ts < '2016-05-09 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-07 00:00:00' AND NEW.local_utc_ts < '2016-05-08 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_07 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-06 00:00:00' AND NEW.local_utc_ts < '2016-05-07 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_06 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-05 00:00:00' AND NEW.local_utc_ts < '2016-05-06 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_05 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-04 00:00:00' AND NEW.local_utc_ts < '2016-05-05 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_04 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-03 00:00:00' AND NEW.local_utc_ts < '2016-05-04 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_03 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-02 00:00:00' AND NEW.local_utc_ts < '2016-05-03 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_02 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-05-01 00:00:00' AND NEW.local_utc_ts < '2016-05-02 00:00:00' THEN
        INSERT INTO tracker_motion_par_2016_05_01 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2016-04-30 00:00:00' AND NEW.local_utc_ts < '2016-05-01 00:00:00' THEN
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
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;
