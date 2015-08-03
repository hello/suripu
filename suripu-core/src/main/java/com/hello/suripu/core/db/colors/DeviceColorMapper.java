package com.hello.suripu.core.db.colors;

import com.hello.suripu.core.models.Device;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceColorMapper implements ResultSetMapper<Device.Color> {

    public Device.Color map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return Device.Color.valueOf(r.getString("color"));
    }
}
