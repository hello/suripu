package com.hello.suripu.core.db;

import com.hello.suripu.core.Record;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RecordMapper implements ResultSetMapper<Record>{
    @Override
    public Record map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        final Record record = new Record(
                r.getFloat("ambient_temp"),
                r.getFloat("ambient_humidity"),
                r.getFloat("ambient_air_quality"),
                r.getFloat("ambient_light"),
                new DateTime(r.getTimestamp("ts"), DateTimeZone.UTC),  // This is confusing, shall we just keep it long or use the offset to create a DateTime that reflects the timezone?
                r.getInt("offset_millis")
        );
        return record;
    }
}
