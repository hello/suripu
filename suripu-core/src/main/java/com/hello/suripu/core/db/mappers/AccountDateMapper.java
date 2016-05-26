package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.AccountDate;
import com.hello.suripu.core.models.AccountQuestion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kingshy on 10/24/14.
 */
public class AccountDateMapper implements ResultSetMapper<AccountDate> {
    @Override
    public AccountDate map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new AccountDate(r.getLong("account_id"),
                new DateTime(r.getTimestamp("night"), DateTimeZone.UTC)
        );
    }
}
