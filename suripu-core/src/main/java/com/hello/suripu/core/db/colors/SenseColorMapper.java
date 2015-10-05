package com.hello.suripu.core.db.colors;

import com.hello.suripu.core.models.device.v2.Sense;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SenseColorMapper implements ResultSetMapper<Sense.Color> {

    public Sense.Color map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return Sense.Color.valueOf(r.getString("color"));
    }
}
