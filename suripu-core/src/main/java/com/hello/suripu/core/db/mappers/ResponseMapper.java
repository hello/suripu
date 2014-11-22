package com.hello.suripu.core.db.mappers;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Question;
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

        DateTime askTime = DateTime.now(DateTimeZone.UTC);
        try {
            if (r.findColumn("ask_time") > 0) {
                askTime = new DateTime(r.getTimestamp("ask_time"), DateTimeZone.UTC);
            }
        } catch (SQLException error) {
        }

        // default as no response -- use when checking for skips
        DateTime created = DateTime.now(DateTimeZone.UTC);
        Optional<Boolean> skip = null; // treat unanswered questions as skips
        Integer response_id = 0;
        Optional<Question.FREQUENCY> qfreq = null;
        if (r.getTimestamp("created") != null) {
            created = new DateTime(r.getTimestamp("created"), DateTimeZone.UTC);
            skip = Optional.fromNullable(r.getBoolean("skip"));
            response_id = r.getInt("response_id");
            qfreq = Optional.fromNullable(Question.FREQUENCY.valueOf(r.getString("question_freq").toUpperCase()));
        }

        return new Response(
                r.getLong("id"),
                r.getLong("account_id"),
                r.getInt("question_id"),
                "", // empty response string for now
                response_id,
                skip,
                created,
                r.getLong("account_question_id"),
                qfreq,
                askTime
        );
    }

}
