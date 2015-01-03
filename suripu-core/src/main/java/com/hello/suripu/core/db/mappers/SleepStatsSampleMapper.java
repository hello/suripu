package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Insights.SleepStatsSample;
import com.hello.suripu.core.models.SleepStats;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SleepStatsSampleMapper implements ResultSetMapper<SleepStatsSample> {

    @Override
    public SleepStatsSample map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        final SleepStats stats = new SleepStats(
                resultSet.getInt("sound_sleep"),
                resultSet.getInt("light_sleep"),
                resultSet.getInt("duration"),
                resultSet.getInt("motion"),
                resultSet.getTimestamp("sleep_time_utc").getTime(),
                resultSet.getTimestamp("wake_time_utc").getTime(),
                resultSet.getInt("time_to_sleep")
        );

        return new SleepStatsSample(stats,
                new DateTime(resultSet.getTimestamp("local_utc_date"), DateTimeZone.UTC),
                resultSet.getInt("offset_millis")
                );
    }
}
