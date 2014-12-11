package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceAccountPair;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceAccountPairMapper implements ResultSetMapper<DeviceAccountPair> {

    @Override
    public DeviceAccountPair map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        final Long accountId = r.getLong("account_id");
        final Long id = r.getLong("id");
        final String deviceId = r.getString("device_id");

        return new DeviceAccountPair(accountId, id, deviceId);
    }
}
