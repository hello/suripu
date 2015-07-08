package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.util.DeserializedSleepHmmBayesNetWithParams;
import org.joda.time.DateTime;

/**
 * Created by benjo on 7/7/15.
 */
public interface BayesNetModelDAO {
    public Optional<DeserializedSleepHmmBayesNetWithParams> getLatestModelForDate(Long accountId, DateTime dateTimeLocalUTC);
}
