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
        return TimelineFeedback.create(
                new DateTime(r.getTimestamp("date_of_night"), DateTimeZone.UTC),
                r.getString("old_time"),
                r.getString("new_time"),
                Event.Type.fromInteger(r.getInt("event_type"))
        );
    }
}
