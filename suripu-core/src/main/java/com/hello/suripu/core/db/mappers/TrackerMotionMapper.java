package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.TrackerMotion;
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

        return new TrackerMotion(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                resultSet.getLong("tracker_id"),
                resultSet.getTimestamp("ts").getTime(),
                resultSet.getInt("svm_no_gravity"),
                resultSet.getInt("offset_millis")
        );
    }
}
