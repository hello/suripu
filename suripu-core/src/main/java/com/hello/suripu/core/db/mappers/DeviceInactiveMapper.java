package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceInactive;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceInactiveMapper implements ResultSetMapper<DeviceInactive> {

    @Override
    public DeviceInactive map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final String deviceId = r.getString("device_id");
        final Long id = r.getLong("id");
        final String diff = r.getString("diff");
        final DateTime maxTs = new DateTime(r.getTimestamp("max_ts"), DateTimeZone.UTC);

        return new DeviceInactive(deviceId, id, diff, maxTs);
    }
}
