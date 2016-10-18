package com.hello.suripu.core.models.device.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.firmware.HardwareVersion;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceStatus;
import com.hello.suripu.core.models.WifiInfo;
import com.hello.suripu.core.sense.metadata.HumanReadableHardwareVersion;
import com.hello.suripu.core.sense.metadata.SenseMetadata;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

public class Sense {

    private static ImmutableMap<HardwareVersion, HumanReadableHardwareVersion> readableHwVersion;
    static {
        final Map<HardwareVersion, HumanReadableHardwareVersion> temp = new HashMap<>();
        temp.put(HardwareVersion.SENSE_ONE, HumanReadableHardwareVersion.SENSE);
        temp.put(HardwareVersion.SENSE_ONE_FIVE, HumanReadableHardwareVersion.SENSE_WITH_VOICE);
        readableHwVersion = ImmutableMap.copyOf(temp);
    }

    public enum Color {
        UNKNOWN("UNKNOWN"),
        BLACK("BLACK"),
        WHITE("WHITE");

        private final String value;
        Color(final String value) {
            this.value = value;
        }
    }

    public static final Color DEFAULT_COLOR = Color.UNKNOWN;

    public enum State {
        NORMAL,
        UNKNOWN
    }

    private static final String DEFAULT_FW_VERSION = "--";

    @JsonIgnore
    public final Long internalId;

    @JsonProperty("id")
    public final String externalId;

    @JsonProperty("firmware_version")
    public final String firmwareVersionOptional;

    @JsonProperty("state")
    public final State state;

    @JsonProperty("last_updated")
    public final Optional<DateTime> lastUpdatedOptional;

    @JsonProperty("color")
    public final Color color;

    @JsonProperty("wifi_info")
    public final Optional<WifiInfo> wifiInfoOptional;

    private final HardwareVersion hardwareVersion;

    @JsonProperty("hw_version")
    public HumanReadableHardwareVersion hardwareVersion() {
        return readableHwVersion.getOrDefault(hardwareVersion, HumanReadableHardwareVersion.UNKNOWN);
    }



    private Sense(final DeviceAccountPair pair, final String firmwareVersionOptional, final State state, final Optional<DateTime> lastUpdatedOptional, final Color color, final Optional<WifiInfo> wifiInfoOptional, final HardwareVersion hardwareVersion) {
        this.internalId = pair.internalDeviceId;
        this.externalId = pair.externalDeviceId;
        this.firmwareVersionOptional = firmwareVersionOptional;
        this.state = state;
        this.lastUpdatedOptional = lastUpdatedOptional;
        this.color = color;
        this.wifiInfoOptional = wifiInfoOptional;
        this.hardwareVersion = hardwareVersion;
    }

    public static Sense create(final DeviceAccountPair senseAccountPair, final Optional<DeviceStatus> senseStatusOptional, Color color, final Optional<WifiInfo> wifiInfoOptional, final HardwareVersion hardwareVersion) {
        if (!senseStatusOptional.isPresent()) {
            return new Sense(senseAccountPair, DEFAULT_FW_VERSION, State.UNKNOWN, Optional.<DateTime>absent(), color, wifiInfoOptional, hardwareVersion);
        }
        final DeviceStatus senseStatus = senseStatusOptional.get();
        return new Sense(senseAccountPair, senseStatus.firmwareVersion, State.NORMAL, Optional.of(senseStatus.lastSeen), color, wifiInfoOptional, hardwareVersion);
    }

    public static Sense create(final DeviceAccountPair senseAccountPair, final Optional<DeviceStatus> senseStatusOptional, final Optional<WifiInfo> wifiInfoOptional, final SenseMetadata metadata) {
        final boolean isPrimary = metadata.hasPrimaryAccountId() && (senseAccountPair.accountId.equals(metadata.primaryAccountId()));
        if (!senseStatusOptional.isPresent()) {
            return new Sense(senseAccountPair, DEFAULT_FW_VERSION, State.UNKNOWN, Optional.<DateTime>absent(), metadata.color(), wifiInfoOptional, metadata.hardwareVersion());
        }
        final DeviceStatus senseStatus = senseStatusOptional.get();
        return new Sense(senseAccountPair, senseStatus.firmwareVersion, State.NORMAL, Optional.of(senseStatus.lastSeen), metadata.color(), wifiInfoOptional, metadata.hardwareVersion());
    }
}
