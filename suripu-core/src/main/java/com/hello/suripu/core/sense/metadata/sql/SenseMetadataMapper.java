package com.hello.suripu.core.sense.metadata.sql;

import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.sense.metadata.SenseMetadata;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SenseMetadataMapper implements ResultSetMapper<SenseMetadata>{
    @Override
    public SenseMetadata map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        return new SenseMetadata(
                resultSet.getString("sense_id"),
                Sense.Color.valueOf(resultSet.getString("color")),
                HardwareVersion.SENSE_ONE
        );
    }
}
