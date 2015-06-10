package com.hello.suripu.core.diagnostic;

import com.google.common.collect.ImmutableList;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

public interface DiagnosticDAO {

    @RegisterMapper(CountMapper.class)
    @SqlQuery("SELECT date_trunc('hour', local_utc_ts) AS ts_hour, COUNT(*) AS cnt FROM device_sensors_master WHERE " +
            "account_id = :account_id AND device_id = :device_id and ts > now() - interval '10 days' GROUP BY ts_hour ORDER BY ts_hour;")
    public ImmutableList<Count> uptime(@Bind("account_id") final Long accountId, @Bind("device_id") final Long deviceId);

    @RegisterMapper(CountMapper.class)
    @SqlQuery("WITH filled_hours AS (\n"+
            "SELECT hour, 0 AS blank_count FROM generate_series(date_trunc('hour', now() - interval '10 days'), date_trunc('hour', now()), '1 hour') AS hour),\n"+
            "uptime_readings AS (\n"+
                "SELECT date_trunc('hour', ts) AS ts_hour, COUNT(*) AS cnt FROM device_sensors_master WHERE " +
            "account_id = :account_id AND device_id = :device_id and ts > now() - interval '10 days' GROUP BY ts_hour ORDER BY ts_hour)\n"+
            "\n"+
            "select filled_hours.hour as ts_hour, coalesce(uptime_readings.cnt, filled_hours.blank_count) AS cnt\n"+
            "FROM filled_hours\n"+
            "LEFT OUTER JOIN uptime_readings ON filled_hours.hour = uptime_readings.ts_hour\n"+
            "ORDER BY filled_hours.hour;")
    public ImmutableList<Count> uptimePadded(@Bind("account_id") final Long accountId, @Bind("device_id") final Long deviceId);
}
