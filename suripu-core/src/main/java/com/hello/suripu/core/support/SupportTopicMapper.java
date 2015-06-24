package com.hello.suripu.core.support;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SupportTopicMapper implements ResultSetMapper<SupportTopic>{
    @Override
    public SupportTopic map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return SupportTopic.create(r.getInt("id"), r.getString("topic"), r.getString("display_name"));
    }
}
