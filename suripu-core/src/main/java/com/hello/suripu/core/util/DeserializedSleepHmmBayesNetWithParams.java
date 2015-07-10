package com.hello.suripu.core.util;

import com.hello.suripu.algorithm.bayes.SensorDataReductionAndInterpretation;

/**
 * Created by benjo on 7/7/15.
 */
public class DeserializedSleepHmmBayesNetWithParams {
    public final SensorDataReductionAndInterpretation sensorDataReductionAndInterpretation;
    public final HmmBayesNetMeasurementParameters params;

    public DeserializedSleepHmmBayesNetWithParams(SensorDataReductionAndInterpretation sensorDataReductionAndInterpretation, HmmBayesNetMeasurementParameters params) {
        this.sensorDataReductionAndInterpretation = sensorDataReductionAndInterpretation;
        this.params = params;
    }
}
