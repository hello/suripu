package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DeviceAccountPair;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceAccountPairMapper implements ResultSetMapper<DeviceAccountPair> {
    @Override
    public DeviceAccountPair map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new DeviceAccountPair(
                r.getLong("account_id"),
                r.getLong("id"),
                r.getString("device_name")
        );
    }
}
