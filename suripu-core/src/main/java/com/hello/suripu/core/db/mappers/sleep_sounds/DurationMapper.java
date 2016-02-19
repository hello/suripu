package com.hello.suripu.core.db.mappers.sleep_sounds;

import com.hello.suripu.core.models.sleep_sounds.Duration;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jakepiccolo on 2/18/16.
 */
public class DurationMapper implements ResultSetMapper<Duration> {
    @Override
    public Duration map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return Duration.create(
                r.getLong("id"),
                r.getString("name"),
                r.getLong("duration_seconds"));
    }
}
