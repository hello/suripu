package com.hello.suripu.core.analytics;

import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.device.v2.Pill;

public interface AnalyticsTracker {
    void trackLowBattery(final Pill pill, final Account account);
}
