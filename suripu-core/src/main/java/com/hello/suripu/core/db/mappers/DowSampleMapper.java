package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Insights.DowSample;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kingshy on 12/17/14.
 */
public class DowSampleMapper implements ResultSetMapper<DowSample> {
    @Override
    public DowSample map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new DowSample(
                resultSet.getInt("day_of_week"),
                resultSet.getFloat("value")
        );
    }
}
