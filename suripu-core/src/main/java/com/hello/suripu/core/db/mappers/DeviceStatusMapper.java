package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceStatusMapper implements ResultSetMapper<DeviceStatus> {
    @Override
    public DeviceStatus map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new DeviceStatus(
                r.getLong("id"),
                r.getLong("pill_id"),
                r.getString("firmware_version"),
                r.getInt("battery_level"),
                new DateTime(r.getTimestamp("last_updated"))
        );
    }
}
