package com.hello.suripu.core.models.device.v2;


public class DeviceQueryInfo {

    public Long accountId;
    public Boolean isLastSeenDBEnabled;
    public Boolean isSensorsDBUnavailable;

    private DeviceQueryInfo(final Long accountId, final Boolean isLastSeenDBEnabled, final Boolean isSensorsDBUnavailable) {
        this.accountId = accountId;
        this.isLastSeenDBEnabled = isLastSeenDBEnabled;
        this.isSensorsDBUnavailable = isSensorsDBUnavailable;
    }

    public static DeviceQueryInfo create(final Long accountId, final Boolean isLastSeenDBEnabled, final Boolean isSensorsDBUnavailable) {
        return new DeviceQueryInfo(accountId, isLastSeenDBEnabled, isSensorsDBUnavailable);
    }
}
