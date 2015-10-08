package com.hello.suripu.core.db.colors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.device.v2.Sense;

public interface SenseColorDAO {

    Optional<Device.Color> getColorForSense(final String senseId);
    Optional<Sense.Color> get(final String senseId);  // for v2
    int saveColorForSense(final String senseId, final String color);
    int update(final String senseId, final String color);
    ImmutableList<String> missing();
}
