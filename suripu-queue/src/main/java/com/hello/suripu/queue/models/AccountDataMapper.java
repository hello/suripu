package com.hello.suripu.queue.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by ksg on 3/15/16
 */
public class AccountDataMapper implements ResultSetMapper<AccountData> {
    @Override
    public AccountData map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new AccountData(r.getLong("account_id"),
                r.getInt("offset_millis"),
                new DateTime(r.getTimestamp("ts"), DateTimeZone.UTC)
        );
    }
}
