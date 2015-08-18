package com.hello.suripu.core.util;

import com.hello.suripu.algorithm.bayes.SensorDataReduction;

/**
 * Created by benjo on 7/7/15.
 */
public class DeserializedSleepHmmBayesNetWithParams {
    public final SensorDataReduction sensorDataReduction;
    public final HmmBayesNetMeasurementParameters params;

    public DeserializedSleepHmmBayesNetWithParams(SensorDataReduction sensorDataReduction, HmmBayesNetMeasurementParameters params) {
        this.sensorDataReduction = sensorDataReduction;
        this.params = params;
    }
}
