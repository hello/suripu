package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.MultiObsSequence;
import com.hello.suripu.algorithm.hmm.MultiObsSequenceAlphabetHiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 8/20/15.
 */
public class OnlineHmmModelEvaluator {
    //how much to you weight the prior relative to the new update?
    //  new update is 1.0 / (PRIORS_WEIGHT_AS_NUMBER_OF_UPDATES + 1.0) in terms of weight.
    // ergo if this number is 5.0, you'll need more than 5 updates to dominate the prior
    // since each update can be though of a day.... that's like a work week
    final static double PRIORS_WEIGHT_AS_NUMBER_OF_UPDATES = 1.0;
    public final static String DEFAULT_MODEL_KEY = "default";


    final Optional<UUID> uuid;

    public OnlineHmmModelEvaluator(final Optional<UUID> uuid) {
        this.uuid = uuid;
    }

    private static MultiObsSequence modelPathsToMultiObsSequence(final Map<String,ImmutableList<Integer>> features,
                                                                 final Multimap<Integer, Transition> forbiddenTransitions,
                                                                 final Optional< Map<Integer, Integer>> labelsOptional) {

        Map<String, double[][]> rawmeasurements = Maps.newHashMap();

        for (final String modelId : features.keySet()) {
            final ImmutableList<Integer> featureAlphabet = features.get(modelId);

            final double [][] x = new double [1][featureAlphabet.size()];

            for (int i = 0; i < featureAlphabet.size(); i++) {
                x[0][i] = featureAlphabet.get(i);
            }

            rawmeasurements.put(modelId,x);
        }

        Map<Integer, Integer> labels = Maps.newHashMap(); //no labels

        if (labelsOptional.isPresent()) {
            labels = labelsOptional.get();
        }

        return new MultiObsSequence(rawmeasurements,labels,forbiddenTransitions);

    }

    static private String createNewModelId(final String oldModelId) {
        final String [] tokens = oldModelId.split("-");


        if (tokens.length == 1) {
            return String.format("%s-%d",tokens[0],1);
        }

        if (tokens.length == 2) {
            int number = Integer.valueOf(tokens[1]);
            number++;
            return String.format("%s-%d",tokens[0],number);
        }

        return "error";

    }
    /* input:
     * -which models were used for which output
     * -the models
     * -raw data / features
     * -forbidden transitions by time
     * -labels by output id and time
     *
     * output:
     * a model delta for each output id
     * */
    public OnlineHmmScratchPad reestimate(final Map<String,String> usedModelsByOutputId, final OnlineHmmPriors priors,final Map<String,ImmutableList<Integer>> features, final Map<String,Map<Integer,Integer>> labelsByOutputId,long currentTime) {

        final Map<String,OnlineHmmModelParams> paramsByOutputId = Maps.newHashMap();

        for (final Map.Entry<String,String> entry : usedModelsByOutputId.entrySet()) {
            final String outputId = entry.getKey();
            final String modelId = entry.getValue();


            final Map<String,OnlineHmmModelParams> paramsById = priors.modelsByOutputId.get(outputId);

            if (paramsById == null) {
                throw new AlgorithmException(String.format("output id %s does not exist",outputId));
            }

            final OnlineHmmModelParams params = paramsById.get(modelId);

            if (params == null) {
                throw new AlgorithmException(String.format("model id %s in output id %s does not exist",modelId,outputId));
            }


            //get the labels
            Optional<Map<Integer,Integer>> labelsOptional = Optional.absent();

            final Map<Integer,Integer> labels = labelsByOutputId.get(outputId)  ;

            if (labels != null) {
                labelsOptional = Optional.of(labels);
            }

            //get the transition restrictions
            final Multimap<Integer, Transition> forbiddenTransitions = ArrayListMultimap.create();

            for (TransitionRestriction restriction : params.transitionRestrictions) {
                forbiddenTransitions.putAll(restriction.getRestrictions(features));
            }

            //get the measurement sequence
            final MultiObsSequence meas = modelPathsToMultiObsSequence(features,forbiddenTransitions,labelsOptional);

            //finally  go fucking reestimate
            final MultiObsSequenceAlphabetHiddenMarkovModel hmm = new MultiObsSequenceAlphabetHiddenMarkovModel(params.logAlphabetNumerators,params.logTransitionMatrixNumerator,params.logDenominator,params.pi);

            hmm.reestimate(meas,PRIORS_WEIGHT_AS_NUMBER_OF_UPDATES);

            //create new model ID
            final String newModelId = createNewModelId(modelId);

            final OnlineHmmModelParams newParams = new OnlineHmmModelParams(hmm.getLogAlphabetNumerator(),hmm.getLogANumerator(),hmm.getLogDenominator(),hmm.getPi(),params.endStates,params.minStateDurations,params.timeCreatedUtc,currentTime,newModelId,outputId,params.transitionRestrictions);

            paramsByOutputId.put(outputId,newParams);
        }

        return new OnlineHmmScratchPad(paramsByOutputId,currentTime);

    }

    /* EVALUATES ALL THE MODELS AND PICKS THE BEST  */
    public Map<String,MultiEvalHmmDecodedResult> evaluate(final OnlineHmmPriors priors,final Map<String,ImmutableList<Integer>> features) {

        final Map<String,MultiEvalHmmDecodedResult> bestModels = Maps.newHashMap();

        //create result for each output Id
        for (final String outputId : priors.modelsByOutputId.keySet()) {


            //NOW GO THROUGH EACH MODEL IN THE LIST AND EVALUATE THEM
            double bestScore = Double.NEGATIVE_INFINITY;
            MultiObsSequenceAlphabetHiddenMarkovModel.Result bestResult = null;
            String bestModel = null;

            //get the list of models to evaluate
            final Map<String,OnlineHmmModelParams> paramsMap = priors.modelsByOutputId.get(outputId);

            //evaluate
            for (final Map.Entry<String,OnlineHmmModelParams> paramsEntry : paramsMap.entrySet()) {
                final OnlineHmmModelParams params = paramsEntry.getValue();


                try {
                    //get the transition restrictions, which is on a per-model basis
                    final Multimap<Integer, Transition> forbiddenTransitions = ArrayListMultimap.create();

                    for (TransitionRestriction restriction : params.transitionRestrictions) {
                        forbiddenTransitions.putAll(restriction.getRestrictions(features));
                    }

                    //get the measurement sequence with restrictions and labels (the labels will be empty here)
                    final MultiObsSequence meas = modelPathsToMultiObsSequence(features,forbiddenTransitions,Optional.<Map<Integer,Integer>>absent());

                    final MultiObsSequenceAlphabetHiddenMarkovModel hmm = new MultiObsSequenceAlphabetHiddenMarkovModel(params.logAlphabetNumerators,params.logTransitionMatrixNumerator,params.logDenominator,params.pi);

                    final MultiObsSequenceAlphabetHiddenMarkovModel.Result result = hmm.decodeWithConstraints(meas, params.endStates, params.minStateDurations);

                    //track the best
                    if (result.pathScore > bestScore) {
                        bestScore = result.pathScore;
                        bestResult = result;
                        bestModel = params.id;
                    }
                }
                catch (Exception e) {
                    int foo  = 3;
                    foo++;
                }


            }

            if (bestResult == null) {
                throw new AlgorithmException("somehow never evaluated any models");
            }

            //store by outputId
            bestModels.put(outputId,new MultiEvalHmmDecodedResult(bestResult.path,bestResult.pathScore,bestModel));
        }





        return bestModels;
    }



}
