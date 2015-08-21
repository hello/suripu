package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.MultiObsSequence;
import com.hello.suripu.algorithm.hmm.MultiObsSequenceAlphabetHiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 8/20/15.
 */
public class OnlineHmmModelEvaluator {
    final Optional<UUID> uuid;

    public OnlineHmmModelEvaluator(final Optional<UUID> uuid) {
        this.uuid = uuid;
    }

    private static MultiObsSequence modelPathsToMultiObsSequence(final Map<String,ImmutableList<Integer>> features,
                                                                 final Multimap<Integer, Transition> forbiddenTransitions) {

        Map<String, double[][]> rawmeasurements = Maps.newHashMap();

        for (final String modelId : features.keySet()) {
            final ImmutableList<Integer> featureAlphabet = features.get(modelId);

            final double [][] x = new double [1][featureAlphabet.size()];

            for (int i = 0; i < featureAlphabet.size(); i++) {
                x[0][i] = featureAlphabet.get(i);
            }

            rawmeasurements.put(modelId,x);

        }

        final Map<Integer, Integer> labels = Maps.newHashMap(); //no labels

        return new MultiObsSequence(rawmeasurements,labels,forbiddenTransitions);

    }

    /* evaluates all models and picks the best  */
    public Map<String,MultiEvalHmmDecodedResult> evaluate(final OnlineHmmPriors priors,final Map<String,ImmutableList<Integer>> features,final Multimap<Integer, Transition> forbiddenTransition) {

        final MultiObsSequence meas = modelPathsToMultiObsSequence(features,forbiddenTransition);

        final Map<String,MultiEvalHmmDecodedResult> bestModels = Maps.newHashMap();

        for (final String outputId : priors.modelsByOutputId.keySet()) {

            double bestScore = Double.NEGATIVE_INFINITY;
            MultiObsSequenceAlphabetHiddenMarkovModel.Result bestResult = null;
            String bestModel = null;

            final List<OnlineHmmModelParams> paramsList = priors.modelsByOutputId.get(outputId);

            for (final OnlineHmmModelParams params : paramsList) {
                final MultiObsSequenceAlphabetHiddenMarkovModel hmm = new MultiObsSequenceAlphabetHiddenMarkovModel(params.logAlphabetNumerators,params.logTransitionMatrixNumerator,params.logDenominator,params.pi);

                final MultiObsSequenceAlphabetHiddenMarkovModel.Result result =  hmm.decodeWithConstraints(meas, params.endStates, params.minStateDurations);

                if (result.pathScore > bestScore) {
                    bestScore = result.pathScore;
                    bestResult = result;
                    bestModel = params.id;
                }
            }

            if (bestResult == null) {
                throw new AlgorithmException("somehow never evaluated any models");
            }

            bestModels.put(outputId,new MultiEvalHmmDecodedResult(bestResult.path,bestResult.pathScore,bestModel));
        }

        return bestModels;
    }



}
