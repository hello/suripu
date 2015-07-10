package com.hello.suripu.core.db.colors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Device;

public interface SenseColorDAO {

    Optional<Device.Color> getColorForSense(final String senseId);
    int saveColorForSense(final String senseId, final String color);
    int update(final String senseId, final String color);
    ImmutableList<String> missing();
}
