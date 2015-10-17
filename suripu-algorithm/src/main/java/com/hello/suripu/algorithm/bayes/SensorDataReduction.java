package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelInterface;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.LogMath;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/22/15.
 */
public class SensorDataReduction {
    public final Map<String,HiddenMarkovModelInterface> hmmByModelName;
    public final double MIN_LIKELIHOOD_FOR_TRANSITIONS = 1e-320;
    public SensorDataReduction(Map<String, HiddenMarkovModelInterface> hmmByModelName) {
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

        for (final Map.Entry<String,HiddenMarkovModelInterface> entry : hmmByModelName.entrySet()) {
        //DECODE ALL SENSOR DATA INTO DISCRETE "CLASSIFICATIONS"

            final Integer [] possibleEndStates = new Integer[1];

            //TODO have model specify end states
            possibleEndStates[0] = entry.getValue().getNumberOfStates() - 1;

            final HmmDecodedResult hmmDecodedResult = entry.getValue().decode(sensorData, possibleEndStates, MIN_LIKELIHOOD_FOR_TRANSITIONS);

            pathsByModelId.put(entry.getKey(),hmmDecodedResult.bestPath);
        }

        return pathsByModelId;
    }

}
