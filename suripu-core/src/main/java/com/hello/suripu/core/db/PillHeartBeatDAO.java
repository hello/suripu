package com.hello.suripu.core.db;

import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PillHeartBeatDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillHeartBeatDAO.class);

    @SqlUpdate("INSERT INTO pill_status (pill_id, battery_level, uptime, fw_version) VALUES(:pill_id, :battery_level, :uptime, :firmware_version);")
    abstract int insert(@Bind("pill_id") final Long internalPillId, @Bind("battery_level") final Integer batteryLevel, @Bind("uptime") final Integer uptime, @Bind("firmware_version") final Integer firmwareVersion);

    public void silentInsert(final Long internalPillId, final Integer batteryLevel, final Integer uptime, final Integer firmwareVersion) {
        try {
            insert(internalPillId, batteryLevel, uptime, firmwareVersion);
        } catch (UnableToExecuteStatementException exception) {
            LOGGER.error("{}", exception.getMessage());
        }
    }
}
