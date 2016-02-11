package com.hello.suripu.core.db.mappers;

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
public class AccountQuestionMapper implements ResultSetMapper<AccountQuestion> {
    @Override
    public AccountQuestion map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new AccountQuestion(r.getLong("id"),
                r.getLong("account_id"),
                r.getInt("question_id"),
                new DateTime(r.getTimestamp("created_local_utc_ts"), DateTimeZone.UTC),
                new DateTime(r.getTimestamp("created"), DateTimeZone.UTC)
        );
    }
}
