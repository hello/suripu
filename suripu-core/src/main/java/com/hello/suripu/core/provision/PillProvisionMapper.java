package com.hello.suripu.core.provision;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PillProvisionMapper implements ResultSetMapper<PillProvision>{
    @Override
    public PillProvision map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return PillProvision.create(r.getString("sn"), r.getString("device_id"), new DateTime(r.getTimestamp("created"), DateTimeZone.UTC));
    }
}
