package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.AccountCount;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountCountMapper implements ResultSetMapper<AccountCount> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountCountMapper.class);

    @Override
    public AccountCount map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final DateTime createdDate = new DateTime(r.getTimestamp("created_date"));
        final Integer count = r.getInt("count");

        return new AccountCount(createdDate, count);
    }
}
