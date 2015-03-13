package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceInfo;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceInfoMapper implements ResultSetMapper<DeviceInfo> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceInfoMapper.class);

    @Override
    public DeviceInfo map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Long id = r.getLong("id");
        final String deviceId = r.getString("device_id");
        final Long accountId = r.getLong("account_id");
        final DateTime lastUpdated = new DateTime(r.getTimestamp("last_updated"));
        final DateTime created = new DateTime(r.getTimestamp("created_at"));

        return new DeviceInfo(id, deviceId, accountId, lastUpdated, created);
    }
}