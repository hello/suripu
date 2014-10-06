package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.DeviceStatusMapper;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.models.DeviceStatus;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

public interface DeviceDAO {

    // TODO: I think we make the device_name and device_id wrong, now the device_name is actually device_id - Pang

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND device_name = :device_name LIMIT 1;")
    Optional<Long> getDeviceForAccountId(@Bind("account_id") Long accountId, @Bind("device_name") String deviceName);

    // account to morpheus device map
    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE account_id = :account_id;")
    ImmutableList<DeviceAccountPair> getSensesForAccountId(@Bind("account_id") Long accountId);


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


    @SingleValueResult(String.class)
    @SqlQuery("SELECT device_name FROM account_device_map WHERE account_id = :account_id ORDER BY id DESC LIMIT 1;")
    Optional<String> getDeviceIdFromAccountId(@Bind("account_id") final Long accountId);

    // account to pill (aka tracker) map

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id;")
    ImmutableList<DeviceAccountPair> getPillsForAccountId(@Bind("account_id") Long accountId);

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_tracker_map WHERE account_id = :account_id AND device_id = :device_id;")
    Optional<Long> getPillIdForAccountIdDeviceId(
            @Bind("account_id") Long accountId,
            @Bind("device_id") String deviceId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id;")
    public abstract ImmutableList<DeviceAccountPair> getTrackerIds(@Bind("account_id") Long accountId);


    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_tracker_map (account_id, device_id) VALUES(:account_id, :tracker_id)")
    Long registerTracker(@Bind("account_id") Long accountId, @Bind("tracker_id") String trackerId);

    @SqlUpdate("DELETE FROM account_tracker_map WHERE id = :id")
    Integer unregisterTracker(@Bind("id") Long id);

    @SqlUpdate("UPDATE account_tracker_map SET active = FALSE, last_updated = NOW() WHERE device_id = :device_id and active = TRUE;")
    Integer unregisterTracker(@Bind("device_id") final String id);

    @SqlUpdate("UPDATE account_device_map SET active = FALSE, last_updated = NOW() WHERE device_id = :device_id and active = TRUE;")
    Integer unregisterSense(@Bind("device_id") final String id);

    @RegisterMapper(DeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT * FROM pill_status WHERE pill_id = :pill_id;")
    Optional<DeviceStatus> pillStatus(@Bind("pill_id") final Long pillId);
}
