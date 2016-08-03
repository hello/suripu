package com.hello.suripu.core.models.device.v2;


import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;

public class DeviceQueryInfo {

    public final Long accountId;
    public final Boolean isLastSeenDBEnabled;
    public final Boolean isSensorsDBUnavailable;

    public final Optional<Account> account;

    private DeviceQueryInfo(final Long accountId, final Boolean isLastSeenDBEnabled, final Boolean isSensorsDBUnavailable, final Optional<Account> account) {
        this.accountId = accountId;
        this.isLastSeenDBEnabled = isLastSeenDBEnabled;
        this.isSensorsDBUnavailable = isSensorsDBUnavailable;
        this.account = account;
    }

    public static DeviceQueryInfo create(final Long accountId, final Boolean isLastSeenDBEnabled, final Boolean isSensorsDBUnavailable) {
        return new DeviceQueryInfo(accountId, isLastSeenDBEnabled, isSensorsDBUnavailable, Optional.<Account>absent());
    }

    public static DeviceQueryInfo create(final Long accountId, final Boolean isLastSeenDBEnabled, final Boolean isSensorsDBUnavailable, final Account account) {
        return new DeviceQueryInfo(accountId, isLastSeenDBEnabled, isSensorsDBUnavailable, Optional.of(account));
    }
}
