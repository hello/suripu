package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

public interface DeviceDAO {

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE device_id = :device_id;")
    Optional<Long> getDeviceForAccountId(@Bind("device_id") String deviceId);
}
