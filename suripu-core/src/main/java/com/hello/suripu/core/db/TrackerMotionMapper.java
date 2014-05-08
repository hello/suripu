package com.hello.suripu.core.db;

import com.hello.suripu.core.TrackerMotion;
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

        long timestamp = resultSet.getTimestamp("ts").getTime();

        return new TrackerMotion(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                resultSet.getString("tracker_id"),
                new DateTime(timestamp, DateTimeZone.UTC),
                resultSet.getInt("svm_no_gravity"),
                resultSet.getInt("offset_millis")
        );
    }
}
