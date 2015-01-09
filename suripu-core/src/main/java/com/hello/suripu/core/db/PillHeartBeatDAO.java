package com.hello.suripu.core.db;

import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PillHeartBeatDAO {

    private final static Logger LOGGER = LoggerFactory.getLogger(PillHeartBeatDAO.class);

    @SqlUpdate("INSERT INTO pill_status (pill_id, battery_level, uptime, fw_version, last_updated) VALUES(:pill_id, :battery_level, :uptime, :firmware_version, :last_updated);")
    abstract int insert(@Bind("pill_id") final Long internalPillId, @Bind("battery_level") final Integer batteryLevel, @Bind("uptime") final Integer uptime, @Bind("firmware_version") final Integer firmwareVersion, @Bind("last_updated") final DateTime lastUpdated);

    public void silentInsert(final Long internalPillId, final Integer batteryLevel, final Integer uptime, final Integer firmwareVersion, final DateTime lastUpdated) {
        try {
            LOGGER.debug("last updated: {}", lastUpdated);
            insert(internalPillId, batteryLevel, uptime, firmwareVersion, lastUpdated);
        } catch (UnableToExecuteStatementException exception) {
            LOGGER.error("{}", exception.getMessage());
        }
    }
}
