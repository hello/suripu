package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.GroupedTimelineLogSummary;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by jakepiccolo on 10/5/15.
 */
public class GroupedTimelineLogsSummaryMapper implements ResultSetMapper<GroupedTimelineLogSummary> {

    @Override
    public GroupedTimelineLogSummary map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new GroupedTimelineLogSummary(
                resultSet.getInt("algorithm"),
                resultSet.getInt("error"),
                resultSet.getInt("count"),
                resultSet.getString("date_of_night")
        );
    }

}
