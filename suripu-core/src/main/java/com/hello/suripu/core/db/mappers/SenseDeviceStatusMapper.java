package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SenseDeviceStatusMapper implements ResultSetMapper<DeviceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SenseDeviceStatusMapper.class);

    @Override
    public DeviceStatus map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Long id = r.getLong("id");
        final Long senseId = r.getLong("device_id");
        final String firmwareVersion = Integer.toHexString(r.getInt("firmware_version"));
        final Integer batteryLevel = 100; // sense does not run on battery :)
        final DateTime lastUpdated = new DateTime(r.getTimestamp("last_seen"), DateTimeZone.UTC);
        final Integer uptime = 0; // TODO: grab this from protobuf upload

        return new DeviceStatus(id, senseId, firmwareVersion, batteryLevel, lastUpdated, uptime);
    }
}
