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
public class SensorDataReductionAndInterpretation {
    public final Map<String,HiddenMarkovModel> hmmByModelName;
    public Map<String,MultipleEventModel> interpretationByOutputName;
    public final Map<String,String> modelNameMappedToOutputName;

    public SensorDataReductionAndInterpretation(Map<String, HiddenMarkovModel> hmmByModelName, Map<String, MultipleEventModel> interpretationByOutputName, Map<String, String> modelNameMappedToOutputName) {
        this.hmmByModelName = hmmByModelName;
        this.interpretationByOutputName = interpretationByOutputName;
        this.modelNameMappedToOutputName = modelNameMappedToOutputName;
    }

    //used when we load the model priors from the individualized models
    public void updateInterpretationModelPriors(final Map<String,MultipleEventModel> interpretationByOutputName) {
        this.interpretationByOutputName = interpretationByOutputName;
    }


    public Map<String,List<List<Double>>> inferProbabilitiesFromModelAndSensorData(final double [][] sensorData) {

        final Map<String,List<List<Double>>> inferredProbabiltiesByOutputName = Maps.newHashMap();

        if (sensorData.length == 0) {
            throw new AlgorithmException("sensor data length was zero!");
        }

        final int N = sensorData[0].length;
        final Map<String,ImmutableList<Integer>> pathsByModelId = Maps.newHashMap();

        //FORCE END-STATE of "0", in the future we will probably allow the model to specify the allowable end-states
        final Integer [] possibleEndStates = new Integer[1];
        possibleEndStates[0] = 0;

        //DECODE ALL SENSOR DATA INTO DISCRETE "CLASSIFICATIONS"
        for (final String modelName : hmmByModelName.keySet()) {

            final HmmDecodedResult hmmDecodedResult = hmmByModelName.get(modelName).decode(sensorData,possibleEndStates);

            pathsByModelId.put(modelName,hmmDecodedResult.bestPath);
        }


        //INFERENCE LAYER (infer probability of state of interest)
        for (final String outputId : interpretationByOutputName.keySet()) {

            final MultipleEventModel bayesModel = interpretationByOutputName.get(outputId);

            final List<List<Double>> probs = bayesModel.getJointOfForwardsAndBackwards(pathsByModelId,N);

            inferredProbabiltiesByOutputName.put(outputId,probs);
        }

        return inferredProbabiltiesByOutputName;

    }

    static public List<Double> getInverseOfNthElement(List<List<Double>> x, final int index) {
        List<Double> ret = Lists.newArrayList();
        for (List<Double> vec :  x) {
            ret.add(1.0 - vec.get(index));
        }
        return ret;
    }
}
