package com.hello.suripu.core.db;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;

public interface DeviceDAO {

    @SqlQuery("SELECT id FROM account_device_map WHERE device_id = :device_id;")
    Long getDeviceForAccountId(@Bind("device_id") String deviceId);
}
