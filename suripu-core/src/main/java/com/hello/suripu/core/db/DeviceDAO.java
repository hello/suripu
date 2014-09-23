package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

public interface DeviceDAO {

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE account_id = :account_id;")
    ImmutableList<DeviceAccountPair> getSensesForAccountId(@Bind("account_id") Long accountId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id;")
    ImmutableList<DeviceAccountPair> getPillsForAccountId(@Bind("account_id") Long accountId);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_device_map (account_id, device_name, device_id) VALUES(:account_id, :device_name, :device_id)")
    Long registerSense(@Bind("account_id") Long accountId, @Bind("device_name") String deviceName, @Bind("device_id") String deviceId);

    // Returns the latest sense connected to this account, in the case of multiple senses
    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id ORDER BY id DESC LIMIT 1;")
    Optional<Long> getByAccountId(@Bind("account_id") Long accountId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE device_name = :device_name;")
    ImmutableList<DeviceAccountPair> getAccountIdsForDeviceId(@Bind("device_name") String deviceName);

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND device_name = :device_name;")
    Optional<Long> getIdForAccountIdDeviceId(
            @Bind("account_id") Long accountId,
            @Bind("device_name") String deviceName);

    @GetGeneratedKeys
    @SingleValueResult(Integer.class)
    @SqlUpdate("INSERT INTO account_tracker_map (account_id, device_id) VALUES(:account_id, :tracker_id)")
    Integer registerTracker(@Bind("account_id") Long accountId, @Bind("tracker_id") String trackerId);
}
