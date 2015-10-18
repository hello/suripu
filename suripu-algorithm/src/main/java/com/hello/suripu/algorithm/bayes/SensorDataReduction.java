package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/22/15.
 */
public class SensorDataReduction {
    public final Map<String,HiddenMarkovModel> hmmByModelName;

    public SensorDataReduction(Map<String, HiddenMarkovModel> hmmByModelName) {
        this.hmmByModelName = hmmByModelName;
    }



    public Map<String,ImmutableList<Integer>> getPathsFromSensorData(final double [][] sensorData) {

        final Map<String,List<List<Double>>> inferredProbabiltiesByOutputName = Maps.newHashMap();

        if (sensorData.length == 0) {
            throw new AlgorithmException("sensor data length was zero!");
        }

        final int N = sensorData[0].length;
        final Map<String,ImmutableList<Integer>> pathsByModelId = Maps.newHashMap();

        //FORCE END-STATE of "0", in the future we will probably allow the model to specify the allowable end-states

        for (final Map.Entry<String,HiddenMarkovModel> entry : hmmByModelName.entrySet()) {
        //DECODE ALL SENSOR DATA INTO DISCRETE "CLASSIFICATIONS"

            final Integer [] possibleEndStates = new Integer[1];
            possibleEndStates[0] = entry.getValue().numStates - 1;

            final HmmDecodedResult hmmDecodedResult = entry.getValue().decode(sensorData, possibleEndStates);

            pathsByModelId.put(entry.getKey(),hmmDecodedResult.bestPath);
        }

        return pathsByModelId;
    }

}
