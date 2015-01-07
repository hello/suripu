package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.db.mappers.DeviceStatusMapper;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
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
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND device_id = :device_name LIMIT 1;")
    Optional<Long> getDeviceForAccountId(@Bind("account_id") Long accountId, @Bind("device_id") final String deviceName);

    // account to morpheus device map
    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE account_id = :account_id AND active = TRUE ORDER BY id DESC;")
    ImmutableList<DeviceAccountPair> getSensesForAccountId(@Bind("account_id") Long accountId);


    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_device_map (account_id, device_name, device_id, active) VALUES(:account_id, :device_id, :device_id, true)")
    Long registerSense(@Bind("account_id") final Long accountId, @Bind("device_id") final String deviceId);

    // Returns the latest active Sense connected to this account, in the case of multiple senses
    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND active = TRUE ORDER BY id DESC LIMIT 1;")
    Optional<Long> getMostRecentSenseByAccountId(@Bind("account_id") Long accountId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE device_name = :device_name;")
    ImmutableList<DeviceAccountPair> getAccountIdsForDeviceId(@Bind("device_name") String deviceName);

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND device_name = :device_name;")
    Optional<Long> getIdForAccountIdDeviceId(
            @Bind("account_id") Long accountId,
            @Bind("device_name") String deviceName);


    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE account_id = :account_id AND active = TRUE ORDER BY id DESC;")
    ImmutableList<DeviceAccountPair> getDeviceAccountMapFromAccountId(@Bind("account_id") final Long accountId);

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT account_id FROM account_device_map WHERE " +
            "account_id != :account_id AND device_name = " +
            "(SELECT device_name FROM account_device_map WHERE " +
            "account_id = :account_id ORDER BY id DESC LIMIT 1) " +
            "ORDER BY id DESC LIMIT 1")
    Optional<Long> getPartnerAccountId(@Bind("account_id") final Long accountId);

    // account to pill (aka tracker) map

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id AND active = true;")
    ImmutableList<DeviceAccountPair> getPillsForAccountId(@Bind("account_id") Long accountId);

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_tracker_map WHERE account_id = :account_id AND device_id = :device_id;")
    Optional<Long> getPillIdForAccountIdDeviceId(
            @Bind("account_id") Long accountId,
            @Bind("device_id") String deviceId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE account_id = :account_id;")
    public abstract ImmutableList<DeviceAccountPair> getTrackerIds(@Bind("account_id") Long accountId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SingleValueResult(DeviceAccountPair.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE device_id = :pill_id AND active = TRUE ORDER BY id DESC LIMIT 1;")
    Optional<DeviceAccountPair> getInternalPillId(@Bind("pill_id") final String pillId);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_tracker_map (account_id, device_id, active) VALUES(:account_id, :tracker_id, true)")
    Long registerTracker(@Bind("account_id") Long accountId, @Bind("tracker_id") String trackerId);

    @SqlUpdate("DELETE FROM account_tracker_map WHERE id = :id")
    Integer unregisterTracker(@Bind("id") Long id);

    @SqlUpdate("UPDATE account_tracker_map SET active = FALSE, last_updated = NOW() WHERE device_id = :device_id and active = TRUE;")
    Integer unregisterTracker(@Bind("device_id") final String id);

    @SqlUpdate("UPDATE account_device_map SET active = FALSE, last_updated = NOW() WHERE device_id = :device_id and active = TRUE;")
    Integer unregisterSense(@Bind("device_id") final String id);

    //    @SqlQuery("SELECT * FROM pill_status WHERE pill_id = :pill_id;")
    @RegisterMapper(DeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT id, tracker_id AS pill_id, '1' AS firmware_version, 100 AS battery_level, ts AS last_seen, 0 AS uptime from tracker_motion_master WHERE tracker_id = :pill_id ORDER BY id DESC LIMIT 1;")
    Optional<DeviceStatus> pillStatus(@Bind("pill_id") final Long pillId);

    @RegisterMapper(DeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT id, pill_id, fw_version as firmware_version, battery_level, last_updated as last_seen, uptime FROM pill_status WHERE pill_id = :pill_id ORDER BY id DESC LIMIT 1000;")
    ImmutableList<DeviceStatus> pillStatusWithBatteryLevel(@Bind("pill_id") final Long pillId);

    //    @SqlQuery("SELECT * FROM pill_status WHERE pill_id = :pill_id;")
    @RegisterMapper(DeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT id, device_id AS pill_id, firmware_version AS firmware_version, 100 AS battery_level, ts AS last_seen, 0 AS uptime from device_sensors_master WHERE device_id = :sense_id ORDER BY id DESC LIMIT 1;")
    Optional<DeviceStatus> senseStatus(@Bind("sense_id") final Long senseId);

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM account_device_map as m JOIN accounts as a ON (a.id = m.account_id) WHERE m.device_name = :device_id LIMIT :max_devices;")
    ImmutableList<Account> getAccountsByDevices(
            @Bind("device_id") final String deviceId,
            @Bind("max_devices") final Long maxDevices
    );
}
