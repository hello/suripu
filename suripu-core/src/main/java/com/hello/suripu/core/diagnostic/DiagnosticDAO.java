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
}
