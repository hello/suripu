package com.hello.suripu.core.db;

import com.hello.suripu.core.TrackerMotionSample;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by pangwu on 5/6/14.
 */
public class TrackerMotionMapper implements ResultSetMapper<TrackerMotionSample> {

    @Override
    public TrackerMotionSample map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        int timezoneOffset = resultSet.getInt("offset_millis");
        long timestamp = resultSet.getTimestamp("ts").getTime();
        DateTimeZone userLocalTimeZone = DateTimeZone.forOffsetMillis(timezoneOffset);

        return new TrackerMotionSample(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                resultSet.getString("tracker_id"),
                new DateTime(timestamp, userLocalTimeZone),
                resultSet.getInt("svm_no_gravity")
        );
    }
}
