package com.hello.suripu.core.diagnostic;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CountMapper implements ResultSetMapper<Count> {
    @Override
    public Count map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new Count(new DateTime(r.getTimestamp("ts_hour"), DateTimeZone.UTC), r.getInt("cnt"));
    }
}
