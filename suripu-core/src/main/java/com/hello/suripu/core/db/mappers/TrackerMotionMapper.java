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
public class TrackerMotionMapper implements ResultSetMapper<TrackerMotion> {

    @Override
    public TrackerMotion map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {

        final TrackerMotion.Builder builder = new TrackerMotion.Builder();
        builder.withId(resultSet.getLong("id"));
        builder.withAccountId(resultSet.getLong("account_id"));
        builder.withTrackerId(resultSet.getLong("tracker_id"));
        builder.withTimestampMillis(new DateTime(resultSet.getTimestamp("ts"), DateTimeZone.UTC).withSecondOfMinute(0).getMillis());
        builder.withTimestampMillisNoTruncation(new DateTime(resultSet.getTimestamp("ts"), DateTimeZone.UTC).getMillis());
        builder.withOffsetMillis(resultSet.getInt("offset_millis"));
        builder.withValue(resultSet.getInt("svm_no_gravity"));
        builder.withMotionRange(resultSet.getLong("motion_range"));
        builder.withKickOffCounts(resultSet.getLong("kickoff_counts"));
        builder.withOnDurationInSeconds(resultSet.getLong("on_duration_seconds"));

        return builder.build();
    }
}
