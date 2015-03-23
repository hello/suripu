package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.models.DeviceAccountPair;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

public interface DeviceDAO extends Transactional<DeviceDAO> {

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
    @SqlQuery("SELECT * FROM account_tracker_map WHERE active = :is_active;")
    ImmutableList<DeviceAccountPair> getAllPills(@Bind("is_active") Boolean isActive);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SingleValueResult(DeviceAccountPair.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE device_id = :pill_id AND active = TRUE ORDER BY id DESC LIMIT 1;")
    Optional<DeviceAccountPair> getInternalPillId(@Bind("pill_id") final String pillId);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_tracker_map (account_id, device_id, active) VALUES(:account_id, :tracker_id, true)")
    Long registerPill(@Bind("account_id") Long accountId, @Bind("tracker_id") String trackerId);

    @SqlUpdate("DELETE FROM account_tracker_map WHERE id = :id")
    Integer unregisterPillByInternalPillId(@Bind("id") Long id);

    @SqlUpdate("DELETE FROM account_tracker_map WHERE device_id = :device_id and account_id = :account_id;")
    Integer deletePillPairing(@Bind("device_id") final String id, @Bind("account_id") Long accountId);

    @Transaction(TransactionIsolationLevel.REPEATABLE_READ)
    @SqlUpdate("DELETE FROM account_tracker_map WHERE account_id = :account_id;")
    Integer deletePillPairingByAccount(@Bind("account_id") final Long accountId);

    @Transaction(TransactionIsolationLevel.REPEATABLE_READ)
    @SqlUpdate("DELETE FROM account_device_map WHERE device_id = :sense_id;")
    Integer unlinkAllAccountsPairedToSense(@Bind("sense_id") final String senseId);

    @SqlUpdate("DELETE FROM account_device_map WHERE device_id = :device_id and account_id = :account_id;")
    Integer deleteSensePairing(@Bind("device_id") final String senseId, @Bind("account_id") Long accountId);

}
