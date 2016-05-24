package com.hello.suripu.core.db;

import com.hello.suripu.core.models.SleepScoreParameters;
import org.joda.time.DateTime;

/**
 * Created by ksg on 5/24/16
 */
public class SleepScoreParametersDynamoDB implements SleepScoreParametersDAO{

    @Override
    public SleepScoreParameters getSleepScoreParameters(final Long accountId, final DateTime nightDate) {

        return new SleepScoreParameters(accountId, nightDate, SleepScoreParameters.MISSING_DURATION_THRESHOLD);
    }

    @Override
    public Boolean updateSleepScoreParameters(final Long accountId, final SleepScoreParameters parameters) {
        return false;
    }
}
