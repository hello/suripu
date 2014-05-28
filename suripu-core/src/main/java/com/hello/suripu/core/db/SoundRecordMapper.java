package com.hello.suripu.core.db;

import com.hello.suripu.core.SoundRecord;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SoundRecordMapper implements ResultSetMapper<SoundRecord> {
    @Override
    public SoundRecord map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new SoundRecord(
                r.getLong("device_id"),
                r.getInt("max_amplitude"),
                new DateTime(r.getTimestamp("ts_trunc"))
            );
    }
}
