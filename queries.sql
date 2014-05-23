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

explain
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