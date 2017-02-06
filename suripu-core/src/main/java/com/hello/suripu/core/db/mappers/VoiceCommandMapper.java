package com.hello.suripu.core.db.mappers;


import com.hello.suripu.core.models.VoiceCommandRow;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by david on 2/3/17.
 */
public class VoiceCommandMapper implements ResultSetMapper<VoiceCommandRow> {

    @Override
    public VoiceCommandRow map(final int i,
                               final ResultSet resultSet,
                               final StatementContext statementContext) throws SQLException {

        return new VoiceCommandRow(resultSet.getString("title"),
                                   resultSet.getString("description"),
                                   resultSet.getString("command_title"),
                                   resultSet.getString("command"));
    }
}
