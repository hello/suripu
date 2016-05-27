package com.hello.suripu.core.profile;

import com.hello.suripu.core.models.MultiDensityImage;
import org.joda.time.DateTime;
import org.immutables.value.Value;

@Value.Immutable
public interface ProfilePhoto {

    Long accountId();
    DateTime createdAt();
    MultiDensityImage photo();
}
