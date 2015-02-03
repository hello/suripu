package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.DataScience.UserLabel;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserLabelMapper implements ResultSetMapper<UserLabel> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserLabelMapper.class);

    @Override
    public UserLabel map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final String email = r.getString("email");
        final String night = new DateTime(r.getTimestamp("night_date"), DateTimeZone.UTC).toString(DateTimeUtil.DYNAMO_DB_DATE_FORMAT);
        final Long ts = new DateTime(r.getTimestamp("utc_ts"), DateTimeZone.UTC).getMillis();
        final int durationMillis = r.getInt("duration");
        final String label = r.getString("label");
        final int tzOffsetMillis  = r.getInt("tz_offset");
        final String note = r.getString("note");

        return new UserLabel(email, night, ts, durationMillis, label, tzOffsetMillis, note);
    }
}
