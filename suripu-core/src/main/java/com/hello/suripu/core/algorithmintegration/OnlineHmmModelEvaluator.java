package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Created by benjo on 8/20/15.
 *
 * As yoou would guess, this class will evaluate models and output predictions.
 * It will also handle using labels to create new models
 */
public class OnlineHmmModelEvaluator {
    //how much to you weight the prior relative to the new update?
    //  new update is 1.0 / (PRIORS_WEIGHT_AS_NUMBER_OF_UPDATES + 1.0) in terms of weight.
    // ergo if this number is 5.0, you'll need more than 5 updates to dominate the prior
    // since each update can be though of a day.... that's like a work week
    final static double MIN_NUM_PERIODS_ON_BED = 36;

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(OnlineHmmModelEvaluator.class);
    private final Logger LOGGER;
    final Optional<UUID> uuid;

    public OnlineHmmModelEvaluator(final Optional<UUID> uuid) {
        this.uuid = uuid;
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);

    }


    /* EVALUATES ALL THE MODELS AND PICKS THE BEST  */
    public EvaluationResult evaluate(final OnlineHmmPriors defaultEnsemble, final OnlineHmmPriors userPrior, final Map<String, ImmutableList<Integer>> features) {

        //merge models, since we are going to evaluate all of them
        final OnlineHmmPriors allModels = OnlineHmmPriors.createEmpty();
        allModels.mergeFrom(defaultEnsemble);
        allModels.mergeFrom(userPrior);


        //results for predictions
        final Map<String,MultiEvalHmmDecodedResult> predictions = Maps.newHashMap();

        final Multimap<String,MultiEvalHmmDecodedResult> modelEvaluations = ArrayListMultimap.create();

        //EVALUATE EACH MODEL, VOTING ONCE PER OUTPUT ID
        for (final Map.Entry<String,Map<String,OnlineHmmModelParams>> entryByOutput : allModels.modelsByOutputId.entrySet()) {

            //list of results
            final List<MultiEvalHmmDecodedResult> resultsForThisId = Lists.newArrayList();

            //evaluate for each model, saving results in list
            for (final Map.Entry<String,OnlineHmmModelParams> entryByModelForThisOutput : entryByOutput.getValue().entrySet()) {

                final OnlineHmmModelParams params = entryByModelForThisOutput.getValue();

                try {
                    //get the transition restrictions, which is on a per-model basis
                    final Multimap<Integer, Transition> forbiddenTransitions = ArrayListMultimap.create();

                    for (TransitionRestriction restriction : params.transitionRestrictions) {
                        forbiddenTransitions.putAll(restriction.getRestrictions(features));
                    }

                    //get the measurement sequence with restrictions and labels (the labels will be empty here)
                    final MultiObsSequence meas = MultiObsSequence.createModelPathsToMultiObsSequence(features, forbiddenTransitions, Optional.<Map<Integer, Integer>>absent());

                    final MultiObsSequenceAlphabetHiddenMarkovModel hmm = new MultiObsSequenceAlphabetHiddenMarkovModel(params.logAlphabetNumerators,params.logTransitionMatrixNumerator,params.logDenominator,params.pi);

                    final MultiObsSequenceAlphabetHiddenMarkovModel.Result decodeResult = hmm.decodeWithConstraints(meas, params.endStates, params.minStateDurations);

                    final MultiEvalHmmDecodedResult result = new MultiEvalHmmDecodedResult(decodeResult.path,decodeResult.pathScore,params.id);

                    LOGGER.info("{} cost: {}  transitions: {}",result.originatingModel,result.pathcost,result.transitions);

                    resultsForThisId.add(result);

                }
                catch (Exception e) {
                    LOGGER.error("failed to evaluate model {}",params.id);
                    LOGGER.error(e.getMessage());
                }
            }

            //TRIM silly model results
            final List<MultiEvalHmmDecodedResult> goodResults = Lists.newArrayList();
            for (final MultiEvalHmmDecodedResult result : resultsForThisId) {
                if (result.stateDurations[LabelMaker.LABEL_DURING_SLEEP] < MIN_NUM_PERIODS_ON_BED) {
                    continue;
                }
                goodResults.add(result);
            }

            if (goodResults.isEmpty()) {
                LOGGER.warn("skipping output {} because no viable predictions were found",entryByOutput.getKey());
                continue; // skip it!
            }


            //is it better?
            final int T = goodResults.get(0).path.length;
            final int N = goodResults.get(0).numStates;
            final double [][] votes = new double[N][T];
            final Map<String,ModelVotingInfo> votingInfoByOutputId = allModels.votingInfo.get(entryByOutput.getKey());

            for (final MultiEvalHmmDecodedResult result : goodResults) {
                voteByModel(votes,votingInfoByOutputId,result);
            }

            final int [] votepath = getVotedPath(votes);

            final MultiEvalHmmDecodedResult theResult = new MultiEvalHmmDecodedResult(votepath,0.0,String.format("%s-%s",entryByOutput.getKey(),"voted"));

            LOGGER.info("transitions for {} are {}",entryByOutput.getKey(), theResult.transitions);

            //store by outputId
            predictions.put(entryByOutput.getKey(), theResult);

            modelEvaluations.putAll(entryByOutput.getKey(),resultsForThisId);
        }

        return new EvaluationResult(modelEvaluations,predictions);
    }


    private static void voteByModel(final double [][] votes,final Map<String,ModelVotingInfo> voteInfoByModelId,final MultiEvalHmmDecodedResult result) {
        double voteWeight = OnlineHmmModelLearner.DEFAULT_MODEL_PRIOR_PROBABILITY;

        if (voteInfoByModelId != null) {
            final ModelVotingInfo votingInfo = voteInfoByModelId.get(result.originatingModel);

            if (votingInfo != null) {
                voteWeight = votingInfo.prob;
            }
        }

        for (int t = 0; t < result.path.length; t++) {
            votes[result.path[t]][t] += voteWeight;
        }
    }

    private static int [] getVotedPath(final double [][] votes) {
        final int N = votes.length;
        final int T = votes[0].length;

        int [] votepath = new int[T];

        for (int t = 0; t < T; t++) {
            double max = 0;
            int maxidx = 0;
            for (int i = 0; i < N; i++) {
                if (max < votes[i][t]) {
                    max = votes[i][t];
                    maxidx = i;
                }
            }

            votepath[t] = maxidx;

        }

        return votepath;
    }

}
