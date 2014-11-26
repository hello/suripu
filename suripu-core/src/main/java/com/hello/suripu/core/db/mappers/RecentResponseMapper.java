package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kingshy on 10/30/14.
 */
public class RecentResponseMapper implements ResultSetMapper<Response> {
    @Override
    public Response map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Response.Builder builder = new Response.Builder();

        builder.withAccountId(r.getLong("account_id"));
        builder.withQuestionId(r.getInt("question_id"));
        builder.withResponseId(r.getInt("response_id"));
        builder.withSkip(r.getBoolean("skip"));
        builder.withAccountQuestionId(r.getLong("account_question_id"));
        builder.withQuestionFreq(r.getString("question_freq"));

        if (r.getTimestamp("ask_time") != null) {
            builder.withAskTime(new DateTime(r.getTimestamp("ask_time"), DateTimeZone.UTC));
        }

        final Response response = builder.build();
        return response;
    }

}
