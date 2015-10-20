package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.models.DeviceAccountPair;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

public interface DeviceReadDAO {
    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND device_id = :device_name LIMIT 1;")
    Optional<Long> getDeviceForAccountId(@Bind("account_id") Long accountId, @Bind("device_id") final String deviceName);

    // account to morpheus device map
    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE account_id = :account_id AND active = TRUE ORDER BY id DESC;")
    ImmutableList<DeviceAccountPair> getSensesForAccountId(@Bind("account_id") Long accountId);

    // Returns the latest active Sense connected to this account, in the case of multiple senses
    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND active = TRUE ORDER BY id DESC LIMIT 1;")
    Optional<Long> getMostRecentSenseByAccountId(@Bind("account_id") Long accountId);


    @SingleValueResult(DeviceAccountPair.class)
    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE account_id = :account_id ORDER BY id DESC LIMIT 1;")
    Optional<DeviceAccountPair> getMostRecentSensePairByAccountId(@Bind("account_id") Long accountId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_device_map WHERE device_name = :device_name ORDER BY account_id ASC;")
    ImmutableList<DeviceAccountPair> getAccountIdsForDeviceId(@Bind("device_name") String deviceName);

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND device_name = :device_name;")
    Optional<Long> getIdForAccountIdDeviceId(
            @Bind("account_id") Long accountId,
            @Bind("device_name") String deviceName);


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

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE device_id = :pill_id AND active = TRUE;")
    ImmutableList<DeviceAccountPair> getLinkedAccountFromPillId(@Bind("pill_id") String deviceId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE active = :is_active ORDER BY id;")
    ImmutableList<DeviceAccountPair> getAllPills(@Bind("is_active") Boolean isActive);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SingleValueResult(DeviceAccountPair.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE device_id = :pill_id AND active = TRUE ORDER BY id DESC LIMIT 1;")
    Optional<DeviceAccountPair> getInternalPillId(@Bind("pill_id") final String pillId);
}
