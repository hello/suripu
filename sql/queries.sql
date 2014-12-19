-- Query the avg temp for the last 7 days and group per hour

select
    avg(ambient_temp) / 1000 as temp,
    date_trunc('hour', ts) as ts_trunc
from device_sensors
where
    device_id = 1 AND
    ts >= current_date - interval '7 days'
group by
    ts_trunc
order by
    ts_trunc asc;



-- Generate all hours between now and 7 days ago

select day, 0 as blank_count from generate_series(current_date - interval '7 days', current_date, '1 hour') as day;


-- now lets merge these two

-- explain
with filled_hours as (
    select hour, 0 as blank_count from generate_series(current_date - interval '7 days', current_date, '1 hour') as hour
),

temp_readings as (
    select
        avg(ambient_temp) / 1000 as temp,
        date_trunc('hour', ts) as ts_trunc
    from device_sensors
    where
        device_id = 1 AND
        ts >= current_date - interval '7 days'
    group by
        ts_trunc
    order by
        ts_trunc asc
)

select filled_hours.hour,
       coalesce(temp_readings.temp, filled_hours.blank_count) as final_temp
  from filled_hours
    left outer join temp_readings on filled_hours.hour = temp_readings.ts_trunc
  order by filled_hours.hour;


-- including proper 7 days (168 hours) rolling window

-- explain
with filled_hours as (
    select  date_trunc('hour', hour) as hour, 0 as blank_count from generate_series(CURRENT_TIMESTAMP - interval '168 hours', CURRENT_TIMESTAMP, '1 hour') as hour
),

sensor_readings as (
    select
        round(avg(ambient_temp) / 1000) as sensor, -- ambient_temp needs to be replaced by whichever sensor you want
        date_trunc('hour', ts) as ts_trunc
    from device_sensors
    where
        device_id = 1 AND
        ts >= current_date - interval '168 hours'
    group by
        ts_trunc
    order by
        ts_trunc asc
)

select filled_hours.hour,
       coalesce(sensor_readings.sensor, filled_hours.blank_count) as final_sensor
  from filled_hours
    left outer join sensor_readings on filled_hours.hour = sensor_readings.ts_trunc
  order by filled_hours.hour;


--  Expected results
--
--            hour          |    final_temp
--  ------------------------+------------------
--   2014-05-22 22:00:00-07 | 25.0263076923077
--   2014-05-22 23:00:00-07 |           24.926
--   2014-05-23 00:00:00-07 |                0
--   2014-05-23 01:00:00-07 |                0
--   2014-05-23 02:00:00-07 |                0
--   2014-05-23 03:00:00-07 |                0




-- including proper 7 days (168 hours) rolling window timezone aware

-- explain
with filled_hours as (
    select  date_trunc('hour', hour) as hour, 0 as blank_count from generate_series(CURRENT_TIMESTAMP at time zone 'UTC' - interval '168 hours', CURRENT_TIMESTAMP at time zone 'UTC', '1 hour') as hour
),

sensor_readings as (
    select
        round(avg(ambient_temp) / 1000) as sensor, -- ambient_temp needs to be replaced by whichever sensor you want
        date_trunc('hour', ts) as ts_trunc,
        min(offset_millis) as offset_millis
    from device_sensors
    where
        device_id = 1 AND
        ts >= current_date at time zone 'UTC' - interval '168 hours'
    group by
        ts_trunc
    order by
        ts_trunc asc
)

select filled_hours.hour,
       coalesce(sensor_readings.sensor, filled_hours.blank_count) as final_sensor,
       sensor_readings.offset_millis
  from filled_hours
    left outer join sensor_readings on filled_hours.hour = sensor_readings.ts_trunc
  order by filled_hours.hour;





-- Migrating
-- not used, saved for historical reasons

INSERT INTO device_sensors_batch (account_id, device_id, ambient_temp, ambient_light,ambient_humidity, ambient_air_quality, ts, offset_millis)
SELECT MAX(account_id) AS account_id, MAX(device_id) AS device_id,
array_agg(ambient_temp ORDER BY ts ASC) AS ambient_temp,
array_agg(ambient_light ORDER BY ts ASC) AS ambient_light,
array_agg(ambient_humidity ORDER BY ts ASC) AS ambient_humidity,
array_agg(ambient_air_quality ORDER BY ts ASC) AS ambient_air_quality,
date_trunc('hour', ts) + (date_part('minute', ts)::int / 5) * interval '5 min' AS tst,
MIN(offset_millis)
FROM device_sensors
WHERE account_id = 1
GROUP BY account_id, tst;




-- Temperature queries

SELECT
min(ambient_light) as min_light,
max(ambient_light) as max_light,
date_trunc('day', ts) as light_day
FROM device_sensors_master
WHERE account_id = 7
AND ts > '2014-05-13'
AND ts < '2014-06-20'
AND extract(hour from ts) < 7
GROUP BY light_day;
