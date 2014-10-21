package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceData;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceDataMapper implements ResultSetMapper<DeviceData>{
    @Override
    public DeviceData map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        final DeviceData deviceData = new DeviceData(
                r.getLong("account_id"),
                r.getLong("device_id"),
                r.getInt("ambient_temp"),
                r.getInt("ambient_humidity"),
                r.getInt("ambient_air_quality"),
                r.getInt("ambient_air_quality_raw"),
                r.getInt("ambient_light"),
                r.getInt("ambient_light_variance"),
                r.getInt("ambient_light_peakiness"),
                new DateTime(r.getTimestamp("ts"), DateTimeZone.UTC),
                //new DateTime(r.getTimestamp("local_utc_ts"), DateTimeZone.UTC),
                r.getInt("offset_millis")
        );
        return deviceData;
    }
}
