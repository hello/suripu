package com.hello.suripu.core.db.responses;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.DeviceData;

/**
 * Created by jakepiccolo on 11/13/15.
 */
public class DeviceDataResponse extends Response<ImmutableList<DeviceData>> {
    public DeviceDataResponse(final ImmutableList<DeviceData> data, final Status status, final Optional<? extends Exception> exception) {
        super(data, status, exception);
    }
}
