package com.hello.suripu.core.util;

import com.hello.suripu.algorithm.bayes.SensorDataReduction;

/**
 * Created by benjo on 7/7/15.
 */
public class DeserializedFeatureExtractionWithParams {
    public final SensorDataReduction sensorDataReduction;
    public final OnlineHmmMeasurementParameters params;

    public DeserializedFeatureExtractionWithParams(SensorDataReduction sensorDataReduction, OnlineHmmMeasurementParameters params) {
        this.sensorDataReduction = sensorDataReduction;
        this.params = params;
    }
}
