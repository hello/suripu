package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.mappers.DeviceAccountPairMapper;
import com.hello.suripu.core.db.mappers.DeviceStatusMapper;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

public interface DeviceDAOAdmin extends Transactional<DeviceDAOAdmin> {

    @RegisterMapper(DeviceStatusMapper.class)
    @SingleValueResult(DeviceStatus.class)
    @SqlQuery("SELECT id, pill_id, fw_version as firmware_version, battery_level, last_updated as last_seen, uptime FROM pill_status WHERE pill_id = :pill_id AND last_updated <= :end_ts ORDER BY id DESC LIMIT 168;")
    public ImmutableList<DeviceStatus> pillStatusBeforeTs(@Bind("pill_id") final Long pillId, @Bind("end_ts") final DateTime endTs);

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM account_device_map as m JOIN accounts as a ON (a.id = m.account_id) WHERE m.device_name = :device_id LIMIT :max_devices;")
    public ImmutableList<Account> getAccountsBySenseId(
            @Bind("device_id") final String deviceId,
            @Bind("max_devices") final Long maxDevices
    );

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM account_tracker_map as m JOIN accounts as a ON (a.id = m.account_id) WHERE m.device_id = :device_id LIMIT :max_devices;")
    public ImmutableList<Account> getAccountsByPillId(
            @Bind("device_id") final String deviceId,
            @Bind("max_devices") final Long maxDevices
    );

    @RegisterMapper(DeviceAccountPairMapper.class)
    @SingleValueResult(DeviceAccountPair.class)
    @SqlQuery("SELECT * FROM account_tracker_map WHERE device_id LIKE '%'||:pill_id||'%' ORDER BY id LIMIT 10;")
    public ImmutableList<DeviceAccountPair> getPillsByPillIdHint(@Bind("pill_id") final String pillId);

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM account_device_map AS s LEFT OUTER JOIN " +
              "account_tracker_map AS p ON s.account_id = p.account_id LEFT OUTER JOIN " +
              "accounts AS a ON s.account_id = a.id WHERE p.account_id IS NULL ORDER BY p.id DESC LIMIT :limit;")
    public ImmutableList<Account> getAccountsWithSenseWithoutPill(
            @Bind("limit") final Integer limit
    );

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT a.* FROM account_tracker_map AS m LEFT OUTER JOIN " +
              "accounts AS a ON a.id = m.account_id WHERE m.id IN " +
              "(SELECT ps2.pill_id FROM pill_status ps1 INNER JOIN " +
              "(SELECT pill_id, MAX(last_updated) AS max_last_updated FROM pill_status " +
              "WHERE battery_level < :critical_battery_level GROUP BY pill_id) ps2 " +
              "ON ps1.pill_id = ps2.pill_id AND ps2.max_last_updated = ps1.last_updated) " +
              "AND a.email IS NOT NULL ORDER BY a.id DESC LIMIT :limit;")
    ImmutableList<Account> getAccountsWithLowPillBattery(
            @Bind("critical_battery_level") final Integer criticalBatteryLevel,
            @Bind("limit") final Integer limit
    );
}
