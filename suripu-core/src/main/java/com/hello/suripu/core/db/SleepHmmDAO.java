package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.util.SleepHmmWithInterpretation;

/**
 * Created by benjo on 3/3/15.
 */
public interface SleepHmmDAO {
    Optional<SleepHmmWithInterpretation> getLatestModelForDate(long accountId, long timeOfInterestMillis);
}
