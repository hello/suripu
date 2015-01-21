package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by pangwu on 5/6/14.
 */
public class TrackerMotionOffsetMillisMapper implements ResultSetMapper<TrackerMotion> {

    @Override
    public TrackerMotion map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {


        final TrackerMotion.Builder builder = new TrackerMotion.Builder();
        builder.withId(resultSet.getLong("id"));
        builder.withAccountId(resultSet.getLong("account_id"));
        builder.withTrackerId(resultSet.getLong("tracker_id"));
        builder.withTimestampMillis(new DateTime(resultSet.getTimestamp("ts_bucket"), DateTimeZone.UTC).getMillis());
        builder.withOffsetMillis(resultSet.getInt("offset_millis"));
        return builder.build();
    }
}
