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

    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id AND device_id = :device_id LIMIT 1;")
    Optional<Long> getDeviceForAccountId(@Bind("account_id") Long accountId, @Bind("device_id") String deviceId);

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO account_device_map (account_id, device_id) VALUES(:account_id, :device_id)")
    Long registerDevice(@Bind("account_id") Long accountId, @Bind("device_id") String deviceId);

    // TODO : make it work for when we own multiple devices
    @SingleValueResult(Long.class)
    @SqlQuery("SELECT id FROM account_device_map WHERE account_id = :account_id LIMIT 1;")
    Optional<Long> getByAccountId(@Bind("account_id") Long accountId);

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SqlQuery("SELECT id, account_id FROM account_device_map WHERE device_id = :device_id;")
    ImmutableList<DeviceAccountPair> getAccountIdsForDeviceId(@Bind("device_id") String device_id);
}
