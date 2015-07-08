package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.util.HmmBayesNetData;
import org.joda.time.DateTime;

import java.util.UUID;

/**
 * Created by benjo on 7/7/15.
 */
public interface BayesNetModelDAO {
    public HmmBayesNetData getLatestModelForDate(Long accountId, DateTime dateTimeLocalUTC, Optional<UUID> uuidForLogger);
}
