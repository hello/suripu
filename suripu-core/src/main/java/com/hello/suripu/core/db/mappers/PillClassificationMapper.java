package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.PillClassification;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by pangwu on 6/23/15.
 */
public class PillClassificationMapper implements ResultSetMapper<PillClassification> {
    @Override
    public PillClassification map(int index, final ResultSet r, final StatementContext ctx) throws SQLException {
        final PillClassification classification = new PillClassification(
                r.getLong("id"),
                r.getLong("internal_pill_id"),
                r.getString("pill_id"),

                r.getTimestamp("last_24pt_window_ts").getTime(),
                r.getTimestamp("last_72pt_window_ts").getTime(),

                PillClassification.intToFloat(r.getInt("last_update_batt")),

                PillClassification.intToFloat(r.getInt("max_24hr_diff")),
                PillClassification.intToFloat(r.getInt("max_72hr_diff")),

                r.getInt("class")
        );
        return classification;
    }
}
