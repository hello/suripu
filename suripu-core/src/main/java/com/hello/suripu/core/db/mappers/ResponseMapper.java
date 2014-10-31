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
public class ResponseMapper implements ResultSetMapper<Response> {
    @Override
    public Response map(int index, ResultSet r, StatementContext ctx) throws SQLException {


        return new Response(
                r.getLong("id"),
                r.getLong("account_id"),
                r.getInt("account_id"),
                r.getString("response"),
                r.getBoolean("skip"),
                new DateTime(r.getTimestamp("created"), DateTimeZone.UTC),
                r.getLong("account_question_id"),
                new DateTime(r.getTimestamp("ask_time"), DateTimeZone.UTC)
        );
    }

}
