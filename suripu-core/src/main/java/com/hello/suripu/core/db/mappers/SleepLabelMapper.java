package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.SleepLabel;
import com.hello.suripu.core.models.SleepRating;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SleepLabelMapper implements ResultSetMapper<SleepLabel> {

    @Override
    public SleepLabel map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new SleepLabel(
                resultSet.getLong("id"),
                resultSet.getLong("account_id"),
                new DateTime(resultSet.getTimestamp("date_utc"), DateTimeZone.UTC),
                SleepRating.fromInteger(resultSet.getInt("rating")),
                new DateTime(resultSet.getTimestamp("sleep_at_utc"), DateTimeZone.UTC),
                new DateTime(resultSet.getTimestamp("wakeup_at_utc"), DateTimeZone.UTC),
                resultSet.getInt("offset_millis")
        );
    }
}
