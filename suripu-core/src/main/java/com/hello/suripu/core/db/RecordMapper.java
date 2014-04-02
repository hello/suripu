package com.hello.suripu.core.db;

import com.hello.suripu.core.Record;
import org.joda.time.DateTime;
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
                new DateTime(r.getTimestamp("ts")),
                r.getInt("offset_millis")
        );
        return record;
    }
}
