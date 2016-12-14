package com.hello.suripu.core.alerts;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AlertMapper implements ResultSetMapper<Alert> {
    @Override
    public Alert map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return Alert.unreachable(
                r.getLong("id"),
                r.getLong("account_id"),
                r.getString("title"),
                r.getString("body"),
                new DateTime(r.getTimestamp("created_at"), DateTimeZone.UTC)
        );
    }
}
