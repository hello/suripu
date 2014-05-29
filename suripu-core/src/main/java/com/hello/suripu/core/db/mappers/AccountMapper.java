package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Account;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

public class AccountMapper implements ResultSetMapper<Account> {
    @Override
    public Account map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final String timeZone = r.getString("tz");
        final TimeZone tz = (timeZone == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZone);

        final Account account = new Account(
                r.getLong("id"),
                r.getString("email"),
                r.getString("password_hash"),
                tz
        );

        return account;
    }
}
