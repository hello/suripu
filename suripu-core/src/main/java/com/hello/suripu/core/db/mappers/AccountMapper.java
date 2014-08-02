package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Account;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountMapper implements ResultSetMapper<Account> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMapper.class);
    @Override
    public Account map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Account.Builder builder = new Account.Builder();
        builder.withName(r.getString("name"));
        builder.withEmail(r.getString("email"));
        builder.withHeight(r.getInt("height"));
        builder.withWeight(r.getInt("weight"));
        builder.withId(r.getLong("id"));
        builder.withTzOffsetMillis(r.getInt("tz_offset"));
        builder.withPassword(r.getString("password_hash"));
        builder.withGender(r.getString("gender"));

        final Account account = builder.build();

        LOGGER.debug("{}", account);
        return account;
    }
}
