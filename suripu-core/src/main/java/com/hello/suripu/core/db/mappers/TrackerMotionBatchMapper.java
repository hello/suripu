package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.TrackerMotion;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 6/23/14.
 */
public class TrackerMotionBatchMapper implements ResultSetMapper<TrackerMotion.Batch> {

    @Override
    public TrackerMotion.Batch map(int index, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        final Array motionAmplitudes = resultSet.getArray("amplitudes");
        final Long accountId = resultSet.getLong("account_id");
        final Long startTimestamp = resultSet.getTimestamp("ts").getTime();
        final Integer offsetMillis = resultSet.getInt("offset_millis");

        final List<TrackerMotion> trackerMotions = new ArrayList<TrackerMotion>();
        if(motionAmplitudes != null) {
            final Integer[] amplitudesArray = (Integer[])motionAmplitudes.getArray();
            for (int i = 0; i < amplitudesArray.length; i++){
                trackerMotions.add(new TrackerMotion(accountId, startTimestamp + i * 60 * 1000, amplitudesArray[i], offsetMillis));
            }
        }

        return new TrackerMotion.Batch(accountId, startTimestamp, offsetMillis, trackerMotions);

    }

}
