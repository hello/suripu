package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TimelineFeedback;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TimelineFeedbackMapper implements ResultSetMapper<TimelineFeedback>{
    @Override
    public TimelineFeedback map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        final DateTime dateOfNight = new DateTime(r.getTimestamp("date_of_night"), DateTimeZone.UTC);
        final Long accountId = r.getLong("account_id");
        final String oldTime = r.getString("old_time");
        final String newTime = r.getString("new_time");
        final Long created = r.getTimestamp("created").getTime();
        final Event.Type eventType = Event.Type.fromInteger(r.getInt("event_type"));
        final Long id = r.getLong("id");

        return TimelineFeedback.create(
                dateOfNight,
                oldTime,
                newTime,
                eventType,
                accountId,
                created,
                id
        );
    }
}
