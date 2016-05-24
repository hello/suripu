package com.hello.suripu.core.db;

import com.hello.suripu.core.models.SleepScoreParameters;
import org.joda.time.DateTime;

/**
 * Created by ksg on 5/24/16
 */
public interface SleepScoreParametersDAO {

    SleepScoreParameters getSleepScoreParameters(final Long accountId, final DateTime nightDate);
    Boolean updateSleepScoreParameters(final Long accountId, final SleepScoreParameters parameters);
}
