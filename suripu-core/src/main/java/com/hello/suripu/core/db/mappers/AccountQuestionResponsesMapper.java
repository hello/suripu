package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.AccountQuestionResponses;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kingshy on 10/24/14.
 */
public class AccountQuestionResponsesMapper implements ResultSetMapper<AccountQuestionResponses> {
    @Override
    public AccountQuestionResponses map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        final Boolean responded = r.getTimestamp("response_created") != null;
        return new AccountQuestionResponses(r.getLong("id"),
                r.getLong("account_id"),
                r.getInt("question_id"),
                new DateTime(r.getTimestamp("created_local_utc_ts"), DateTimeZone.UTC),
                responded,
                new DateTime(r.getTimestamp("created"), DateTimeZone.UTC));
    }
}
