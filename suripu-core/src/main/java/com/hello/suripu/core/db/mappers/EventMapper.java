package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Event;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by pangwu on 5/8/14.
 */
public class EventMapper implements ResultSetMapper<Event> {

    @Override
    public Event map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        /*
        return new Event(
                Event.Type.fromInteger(resultSet.getInt("event_type")),
                resultSet.getTimestamp("start_time_utc").getTime(),
                resultSet.getTimestamp("end_time_utc").getTime(),
                resultSet.getInt("offset_millis")
        );
        */

        throw new RuntimeException("Not implemented.");
    }
}
