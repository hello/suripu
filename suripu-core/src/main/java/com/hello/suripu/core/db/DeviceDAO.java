package com.hello.suripu.core.db;

import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

public interface DeviceDAO extends Transactional<DeviceDAO>, DeviceReadDAO {

    // TODO: I think we make the device_name and device_id wrong, now the device_name is actually device_id - Pang

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_device_map (account_id, device_name, device_id, active) VALUES(:account_id, :device_id, :device_id, true)")
    Long registerSense(@Bind("account_id") final Long accountId, @Bind("device_id") final String deviceId);

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
    
    @SqlUpdate("UPDATE account_tracker_map set account_id=:account_id WHERE device_id= :device_id")
    Integer updateAccountPairedForPill(@Bind("account_id") Long accountId, @Bind("device_id") String pillId);
}
