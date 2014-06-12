package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Account;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

public class AccountMapper implements ResultSetMapper<Account> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMapper.class);
    @Override
    public Account map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final String timeZone = r.getString("tz");
        final TimeZone tz = (timeZone == null) ? TimeZone.getDefault() : TimeZone.getTimeZone(timeZone);

        final Account.Builder builder = new Account.Builder();
        builder.withFirstname(r.getString("firstname"));
        builder.withLastname(r.getString("lastname"));
        builder.withEmail(r.getString("email"));
        builder.withHeight(r.getInt("height"));
        builder.withWeight(r.getInt("weight"));
        builder.withId(r.getLong("id"));
        builder.withTimeZone(tz);
        builder.withPassword(r.getString("password_hash"));

        final Account account = builder.build();

        LOGGER.debug("{}", account);
        return account;
    }
}
