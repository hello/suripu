package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.DataScience.SleepEventPredictionDistribution;
import org.joda.time.DateTime;

/**
 * Created by benjo on 2/19/15.
 */

public class BayesInferenceResult {
    public BayesInferenceResult() {
        this.distributions = Optional.absent();
        this.eventTime = Optional.absent();
    }

    public BayesInferenceResult(Optional<SleepEventPredictionDistribution> distributions) {
        this.distributions = distributions;
        this.eventTime = Optional.absent();
    }

    public Optional<SleepEventPredictionDistribution> distributions;
    public Optional<DateTime> eventTime;
}

