package com.hello.suripu.core.models;

import com.google.common.base.Optional;

import javax.validation.constraints.NotNull;

/**
 * Created by jakepiccolo on 11/9/15.
 */
public class DeviceId {
    public final Optional<Long> internalDeviceId;
    public final Optional<String> externalDeviceId;

    private DeviceId(final Optional<Long> internalDeviceId, final Optional<String> externalDeviceId) {
        this.internalDeviceId = internalDeviceId;
        this.externalDeviceId = externalDeviceId;
    }

    //What's the best way for not confusing the below two different "types" of DeviceId?
    public static DeviceId create(@NotNull final Long internalDeviceId) {
        return new DeviceId(Optional.of(internalDeviceId), Optional.<String>absent());
    }

    public static DeviceId create(@NotNull final String externalDeviceId) {
        return new DeviceId(Optional.<Long>absent(), Optional.of(externalDeviceId));
    }
}
