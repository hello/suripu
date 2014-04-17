package com.hello.suripu.core.db;

import com.hello.suripu.core.Score;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ScoreMapper implements ResultSetMapper<Score> {
    @Override
    public Score map(int index, ResultSet r, StatementContext ctx) throws SQLException {


        return new Score(
            r.getLong("account_id"),
            r.getInt("ambient_temp"),
            r.getInt("ambient_humidity"),
            0, // sound
            r.getInt("ambient_air_quality"),
            r.getInt("ambient_light"),

            // The same as Record, create a timezone from offset or make an additional field?
            new DateTime(r.getTimestamp("ts"), DateTimeZone.forOffsetMillis(r.getInt("offset_millis")))
        );
    }
}
