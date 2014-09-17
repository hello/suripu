package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.SleepScore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SleepScoreMapper implements ResultSetMapper<SleepScore> {

    @Override
    public SleepScore map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new SleepScore(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                new DateTime(resultSet.getTimestamp("date_bucket_utc"), DateTimeZone.UTC),
                resultSet.getLong("pill_id"),
                resultSet.getInt("sleep_duration"),
                resultSet.getInt("bucket_score"),
                resultSet.getBoolean("custom"),
                resultSet.getInt("agitation_num"),
                resultSet.getInt("agitation_tot"),
                new DateTime(resultSet.getTimestamp("updated"), DateTimeZone.UTC),
                resultSet.getInt("offset_millis")
        );
    }
}
