package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceStatusMapper implements ResultSetMapper<DeviceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceStatusMapper.class);

    @Override
    public DeviceStatus map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Long id = r.getLong("id");
        final Long pillId = r.getLong("pill_id");
        final String firmwareVersion = r.getString("firmware_version");
        final Integer batteryLevel = r.getInt("battery_level");
        final DateTime lastUpdated = new DateTime(r.getTimestamp("last_seen"));
        final Integer uptime = r.getInt("uptime");

        LOGGER.debug("mapping id:  {}", id);
        LOGGER.debug("mapping pillId:  {}", pillId);
        LOGGER.debug("mapping firmwareVersion:  {}", firmwareVersion);
        LOGGER.debug("mapping batterLevel:  {}", batteryLevel);
        LOGGER.debug("mapping lastUpdated:  {}", uptime);

        return new DeviceStatus(id, pillId, firmwareVersion, batteryLevel, lastUpdated, uptime);
    }
}
