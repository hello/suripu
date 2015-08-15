import sys
from calendar import monthrange
from datetime import datetime, timedelta

def date_range(start_date, end_date):
    for n in range(int((end_date - start_date).days)+1):
        yield end_date - timedelta(n)

def get_year_months(start_date, end_date):
    diff_days = (end_date - start_date).days
    approx_months = diff_days/30 + 5

    current_year = end_date.year
    current_month = end_date.month + 1
    for i in range(approx_months):
        current_month -= 1
        if current_month <= 0:
            current_year -= 1
            current_month = 12

        prev_month = current_month - 1
        prev_year = current_year
        if prev_month <= 0:
            prev_year -= 1
            prev_month = 12

        current_date = datetime(current_year, current_month, 1)
        prev_date = datetime(prev_year, prev_month, 1)
        if prev_date < start_date:
            break
        yield prev_date, current_date

if len(sys.argv) < 3:
    print "\nUsage: python device_sensor_sharding.py YYYY MM\n"
    sys.exit()

year = sys.argv[1]
if len(year) != 4:
    print "\nUsage: python device_sensor_sharding.py YYYY MM\n"
    sys.exit()

month = sys.argv[2]

# get number of days per month
m_range = monthrange(int(year), int(month))
num_days = m_range[1]

DAILY_SHARDING_START = datetime(year=2015, month=7, day = 1)
start_sharding_date = datetime(year=int(year), month=int(month), day = 1)
end_sharding_date = datetime(year=int(year), month=int(month), day = num_days)

# sql file
filename = "sensors/device_sensors/sharding/shard_daily_%s_%s.sql" % (year, month)
fp = open(filename, "w")
fp.write("\n-- %s-%s shard for device_sensors\n\n" % (year, month))

# create the tables for the whole month
create_table_str = "CREATE TABLE IF NOT EXISTS device_sensors_par_%s() INHERITS (device_sensors_master);"
unique_index_str = "CREATE UNIQUE INDEX device_sensors_par_%s_uniq_device_id_account_id_ts on device_sensors_par_%s(device_id, account_id, ts);"
index_str = "CREATE INDEX device_sensors_par_%s_ts_idx on device_sensors_par_%s(ts);"
check_str = "ALTER TABLE device_sensors_par_%s ADD CHECK (local_utc_ts >= '%s 00:00:00' AND local_utc_ts < '%s 00:00:00');"

for i in range(num_days):
    date = start_sharding_date + timedelta(i)
    date_string = datetime.strftime(date, "%Y_%m_%d")

    fp.write(create_table_str % (date_string) + "\n")
    fp.write(unique_index_str % (date_string, date_string) + "\n")

    gte = datetime.strftime(date, "%Y-%m-%d")
    lt = datetime.strftime(date + timedelta(1), "%Y-%m-%d")
    fp.write(index_str % (date_string, date_string) + "\n")

    # add date checks
    gte = datetime.strftime(date, "%Y-%m-%d")
    lt = datetime.strftime(date + timedelta(1), "%Y-%m-%d")
    fp.write(check_str % (date_string, gte, lt) + "\n")
    fp.write("\n")

fp.write("\n")


# create trigger function
start_trigger_str = """
CREATE OR REPLACE FUNCTION device_sensors_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN"""
fp.write(start_trigger_str + "\n")


trigger_condition = "NEW.local_utc_ts >= '%s 00:00:00' AND NEW.local_utc_ts < '%s 00:00:00' THEN"

n = 0
for current_date in date_range(DAILY_SHARDING_START, end_sharding_date):
    gte_str = datetime.strftime(current_date, "%Y-%m-%d")
    lt_str = datetime.strftime(current_date + timedelta(1), "%Y-%m-%d")

    if n == 0:
        fp.write("    IF " + trigger_condition % (gte_str, lt_str) + "\n")
    else:
        fp.write("    ELSIF " + trigger_condition % (gte_str, lt_str) + "\n")

    table_name = "device_sensors_par_%s" % datetime.strftime(current_date, "%Y_%m_%d") 
    fp.write("        INSERT INTO %s VALUES (NEW.*);" % table_name + "\n")
    n += 1
    

remaining_str = """    ELSIF NEW.local_utc_ts >= '2015-06-01 00:00:00' AND NEW.local_utc_ts < '2015-07-01 00:00:00' THEN
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
$BODY$;"""
fp.write(remaining_str + "\n")


fp.write("\n\n-- %s-%s shard for tracker_motion\n\n" % (year, month))

# Create monthly shard for tracker Motion
YYYY_MM = datetime.strftime(start_sharding_date, "%Y_%m")


# create the tables for the whole month
t_create_table_str = "CREATE TABLE IF NOT EXISTS tracker_motion_par_%s() INHERITS (tracker_motion_master);"

t_unique_index_ts_str = "CREATE UNIQUE INDEX " + \
        "tracker_motion_par_%s_uniq_tracker_ts ON " + \
        "tracker_motion_par_%s(tracker_id, ts);"

t_unique_index_all_str = "CREATE UNIQUE INDEX " + \
        "tracker_motion_par_%s_uniq_tracker_id_account_id_ts ON " + \
        "tracker_motion_par_%s(tracker_id, account_id, ts);"

t_index_str = "CREATE INDEX tracker_motion_par_%s_local_utc_ts ON tracker_motion_par_%s(local_utc_ts);"

t_check_str = "ALTER TABLE tracker_motion_par_%s ADD CHECK " + \
        "(local_utc_ts >= '%s 00:00:00' AND local_utc_ts < '%s 00:00:00');"

for i in range(num_days):
    date = start_sharding_date + timedelta(i)
    date_string = datetime.strftime(date, "%Y_%m_%d")

    fp.write(t_create_table_str % (date_string) + "\n")
    fp.write(t_unique_index_ts_str % (date_string, date_string) + "\n")
    fp.write(t_unique_index_all_str % (date_string, date_string) + "\n")
    fp.write(t_index_str % (date_string, date_string) + "\n")

    # add date checks
    gte = datetime.strftime(date, "%Y-%m-%d")
    lt = datetime.strftime(date + timedelta(1), "%Y-%m-%d")
    fp.write(t_check_str % (date_string, gte, lt) + "\n")
    fp.write("\n")

fp.write("\n")



# create tracker_motion_master trigger function
start_trigger_str = """
CREATE OR REPLACE FUNCTION tracker_motion_master_insert_function() RETURNS TRIGGER LANGUAGE plpgsql AS
$BODY$
DECLARE
    table_name text;
BEGIN"""
fp.write("\n" + start_trigger_str + "\n")


trigger_condition = "NEW.local_utc_ts >= '%s 00:00:00' AND NEW.local_utc_ts < '%s 00:00:00' THEN"

n = 0
for current_date in date_range(DAILY_SHARDING_START, end_sharding_date):
    gte_str = datetime.strftime(current_date, "%Y-%m-%d")
    lt_str = datetime.strftime(current_date + timedelta(1), "%Y-%m-%d")

    if n == 0:
        fp.write("    IF " + trigger_condition % (gte_str, lt_str) + "\n")
    else:
        fp.write("    ELSIF " + trigger_condition % (gte_str, lt_str) + "\n")

    table_name = "tracker_motion_par_%s" % datetime.strftime(current_date, "%Y_%m_%d") 
    fp.write("        INSERT INTO %s VALUES (NEW.*);" % table_name + "\n")
    n += 1
    

remaining_str = """
    ELSIF NEW.local_utc_ts >= '2015-08-01 00:00:00' AND NEW.local_utc_ts < '2015-09-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_08 VALUES (NEW.*);
    ELSIF NEW.local_utc_ts >= '2015-07-01 00:00:00' AND NEW.local_utc_ts < '2015-08-01 00:00:00' THEN
        INSERT INTO tracker_motion_par_2015_07 VALUES (NEW.*);
    ELSE
        INSERT INTO tracker_motion_par_default VALUES (NEW.*);
    END IF;
    RETURN NULL;
END
$BODY$;"""


fp.write(remaining_str + "\n")

fp.close()
