package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.MultiObsSequence;
import com.hello.suripu.algorithm.hmm.MultiObsSequenceAlphabetHiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Created by benjo on 10/18/15.
 */
public class OnlineHmmModelLearner {

    private final static double PRIORS_WEIGHT_AS_NUMBER_OF_UPDATES = 5.0;
    public final static double DEFAULT_MODEL_PRIOR_PROBABILITY = 0.2;
    private final static double FAIL_RATIO_NORMALIZER = 0.05;

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(OnlineHmmModelLearner.class);
    private final Logger LOGGER;
    final Optional<UUID> uuid;

    public OnlineHmmModelLearner(final Optional<UUID> uuid) {
        this.uuid = uuid;
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);

    }

    static double [][] getConfusionCountMatrix(final int[] path, final Map<Integer, Integer> labels, final int numStates) {
        final double [][] mtx = new double[numStates][numStates];

        for (final Map.Entry<Integer,Integer> entry : labels.entrySet()) {

            if (entry.getKey() >= path.length) {
                continue;
            }

            final int prediction = path[entry.getKey()];
            final int label = entry.getValue();

            mtx[prediction][label] += 1.0;
        }

        return mtx;
    }


    static double getModelLikelihood(final int [] path, final Map<Integer,Integer> labels, final int numStates) {
        final double [][] confusionCountMatrix = getConfusionCountMatrix(path, labels, numStates);

        //penalize mis-predictions
        double numPredictions = 0.0;
        double numMisPredictions = 0.0;

        for (int j = 0; j < numStates; j++) {
            for (int i = 0; i < numStates; i++) {

                numPredictions += confusionCountMatrix[j][i];

                if (i != j) {
                    numMisPredictions += confusionCountMatrix[j][i];
                }
            }
        }

        double likelihood = 0.0;

        //likelihood function, as failRatio becomes a multiple of FAIL_RATIO_NORMALIZER, likelihood drops to zero
        if (numPredictions > 0.0) {
            double failRatio = numMisPredictions / numPredictions / numStates;

            likelihood = 1.0 - failRatio / (FAIL_RATIO_NORMALIZER + failRatio);
        }


        return likelihood;
    }


    public final Map<String,Map<String,ModelVotingInfo>>   updateModelWeights(final EvaluationResult evaluationResult, final OnlineHmmPriors models, final Map<String,Map<Integer,Integer>> labelsByOutputId) {
        final Map<String,Map<String,ModelVotingInfo>> votingInfo = Maps.newHashMap();

        //clone voting info directly
        for (final Map.Entry<String,Map<String,ModelVotingInfo>> entry : models.votingInfo.entrySet()) {
            votingInfo.put(entry.getKey(),Maps.<String,ModelVotingInfo>newHashMap());
            votingInfo.get(entry.getKey()).putAll(entry.getValue());
        }


        for (final String outputId : labelsByOutputId.keySet()) {

            final Map<Integer,Integer> labels = labelsByOutputId.get(outputId);

            if (!evaluationResult.modelEvaluations.containsKey(outputId)) {
                continue;
            }

            //create voting info if not present
            if (!votingInfo.containsKey(outputId)) {
                votingInfo.put(outputId,Maps.<String,ModelVotingInfo>newHashMap());
            }

            final Map<String,ModelVotingInfo> votingInfoForThisOutput = votingInfo.get(outputId);

            //get all the model predictions for this label
            final Collection<MultiEvalHmmDecodedResult> allModelsPredictions = evaluationResult.modelEvaluations.get(outputId);

            //create voting info if not present
            for (final MultiEvalHmmDecodedResult result : allModelsPredictions) {

                //create voting info if not present
                if (!votingInfo.containsKey(result.originatingModel)) {
                    votingInfoForThisOutput.put(result.originatingModel, new ModelVotingInfo(DEFAULT_MODEL_PRIOR_PROBABILITY));
                }
            }

            //normalize probabilities
            double probSum = 0.0;
            for (final ModelVotingInfo info : votingInfoForThisOutput.values()) {
                probSum += info.prob;
            }

            for (final String key : votingInfoForThisOutput.keySet()) {
                final double normalizedProb = votingInfoForThisOutput.get(key).prob / probSum;
                votingInfoForThisOutput.put(key,new ModelVotingInfo(normalizedProb));
            }



            final Map<String,Double> jointLikelihoods = Maps.newHashMap();

            //evaluate model likelihoods
            for (final MultiEvalHmmDecodedResult result : allModelsPredictions) {

                final ModelVotingInfo thisModelVoteInfo = votingInfoForThisOutput.get(result.originatingModel);

                jointLikelihoods.put(result.originatingModel, getModelLikelihood(result.path, labels, result.numStates) * thisModelVoteInfo.prob);
            }

            //get sum of all model likelihoods (denominator)
            double likelihoodSum = 0.0;
            for (final Double likelihood : jointLikelihoods.values()) {
                likelihoodSum += likelihood;
            }

            for (final MultiEvalHmmDecodedResult result : allModelsPredictions) {

                //bayes update
                final double newProb = jointLikelihoods.get(result.originatingModel) / likelihoodSum;

                //store
                votingInfoForThisOutput.put(result.originatingModel,new ModelVotingInfo(newProb));
            }
        }

        return votingInfo;

    }


    /* input:
     * -which models were used for which output
     * -the models
     * -raw data / features
     * -forbidden transitions by time
     * -labels by output id and time
     *
     * output:
     * a model delta for each output ID if labels are present
     *
     *
     * usedModelsByOutputId - maps output ID to the selected model ID for that output
     * priors - all the models
     * features - the measurements
     * labelsByOutputId - the labels stored by output ID
     * currentTime - current server time in UTC
     * */
    public Map<String,OnlineHmmModelParams> reestimateForMyModel(final OnlineHmmPriors priors,final Map<String,ImmutableList<Integer>> features, final Map<String,Map<Integer,Integer>> labelsByOutputId,long currentTime) {
        final Map<String,OnlineHmmModelParams> learnedModelParams = Maps.newHashMap();

        for (final Map.Entry<String,Map<String,OnlineHmmModelParams>> entry : priors.modelsByOutputId.entrySet()) {
            final String outputId = entry.getKey();

            //skip this loop if the labels are not present
            if (!labelsByOutputId.containsKey(outputId)) {
                continue;
            }

            //skip if no models for this output ID
            final Map<String,OnlineHmmModelParams> paramsById = entry.getValue();

            if (paramsById.isEmpty()) {
                continue;
            }

            //assume only ONE model for now in the user model set
            final OnlineHmmModelParams params = paramsById.values().iterator().next();

            //get the labels
            final Map<Integer,Integer> labels = labelsByOutputId.get(outputId)  ;

            if (labels.isEmpty()) {
                continue;
            }


            //GO FOR IT!!!!!!!!!

            //get empty transition restrictions -- restrictions should not be present for learning
            final Multimap<Integer, Transition> noTransitions = ArrayListMultimap.create();

            //get the measurement sequence
            final MultiObsSequence meas = MultiObsSequence.createModelPathsToMultiObsSequence(features, noTransitions, Optional.of(labels));

            //finally go and learn on the data and labels for the user's model
            final MultiObsSequenceAlphabetHiddenMarkovModel hmm = new MultiObsSequenceAlphabetHiddenMarkovModel(params.logAlphabetNumerators,params.logTransitionMatrixNumerator,params.logDenominator,params.pi);

            hmm.reestimate(meas,PRIORS_WEIGHT_AS_NUMBER_OF_UPDATES);

            //meet the new model ID -- same as the old model ID
            final String newModelId = params.id;

            final OnlineHmmModelParams newParams = new OnlineHmmModelParams(hmm.getLogAlphabetNumerator(),hmm.getLogANumerator(),hmm.getLogDenominator(),hmm.getPi(),params.endStates,params.minStateDurations,params.timeCreatedUtc,currentTime,newModelId,outputId,params.transitionRestrictions);

            learnedModelParams.put(outputId, newParams);
        }

        return learnedModelParams;
    }


    public OnlineHmmScratchPad reestimate(final EvaluationResult evalResult,final OnlineHmmPriors priors,final Map<String,ImmutableList<Integer>> features, final Map<String,Map<Integer,Integer>> labelsByOutputId,long currentTime) {

        Map<String, OnlineHmmModelParams> learnedModelParams = Maps.newHashMap();

        //REESTIMATE FOR -MY- MODEL
        try {
            learnedModelParams = reestimateForMyModel(priors, features, labelsByOutputId, currentTime);
        }
        catch (AlgorithmException e) {
            LOGGER.error(e.getMessage());
        }

        //NOW UPDATE THE WEIGHTING
        final Map<String,Map<String,ModelVotingInfo>> updatedVotingInfo = updateModelWeights(evalResult,priors,labelsByOutputId);

        return new OnlineHmmScratchPad(learnedModelParams,updatedVotingInfo,currentTime);

    }
}
