package com.hello.suripu.core.provision;

import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class PillProvision {

    public final String serialNumber;
    public final String deviceId;
    public final DateTime created;

    private PillProvision(final String serialNumber, final String deviceId, final DateTime created) {
        this.serialNumber = serialNumber;
        this.deviceId = deviceId;
        this.created = created;
    }

    public static PillProvision create(final String serialNumber, final String deviceId, final DateTime created) {
        return new PillProvision(serialNumber, deviceId, created);
    }


    @Override
    public String toString() {
        return Objects.toStringHelper(PillProvision.class)
                .add("SN", serialNumber)
                .add("DeviceID", deviceId)
                .add("created", created).toString();
    }
}
