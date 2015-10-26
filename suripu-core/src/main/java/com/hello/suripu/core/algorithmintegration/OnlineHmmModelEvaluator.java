package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hello.suripu.algorithm.hmm.MultiObsSequence;
import com.hello.suripu.algorithm.hmm.MultiObsSequenceAlphabetHiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.util.OnlineHmmMeasurementParameters;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    final static double MIN_VOTE_PERCENT_TO_BE_IN_SLEEP = 0.85; //be sure
    final static double MIN_VOTE_PERCENT_TO_BE_IN_BED = 0.50; //majority suffices


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

            final DateTime startTime = DateTime.now();

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

                    resultsForThisId.add(result);

                }
                catch (Exception e) {
                    LOGGER.error("failed to evaluate model {}",params.id);
                    LOGGER.error(e.getMessage());
                }
            }

            final DateTime endTime = DateTime.now();
            final long elapsedMillis = endTime.minus(startTime.getMillis()).getMillis();
            LOGGER.info("done evaluating {}. {}ms elapsed.",entryByOutput.getKey(),elapsedMillis);


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


            final int T = goodResults.get(0).path.length;
            final int N = goodResults.get(0).numStates;
            final float [][] votes = new float[N][T];
            final Map<String,ModelVotingInfo> votingInfoByOutputId = allModels.votingInfo.get(entryByOutput.getKey());

            for (final MultiEvalHmmDecodedResult result : goodResults) {
                voteByModel(votes,votingInfoByOutputId,result);
            }

            normalizeVotes(votes);

            LOGGER.info("votes for {}",entryByOutput.getKey());
            for (int istate = 0; istate < N; istate++) {
                LOGGER.info("{}",votes[istate]);
            }

            final int [] votepath = getVotedPathWithConstraints(votes,entryByOutput.getKey().equals(OnlineHmmData.OUTPUT_MODEL_SLEEP));

            final MultiEvalHmmDecodedResult theResult = new MultiEvalHmmDecodedResult(votepath,0.0,String.format("%s-%s",entryByOutput.getKey(),"voted"));

            LOGGER.info("transitions for {} are {}",entryByOutput.getKey(), theResult.transitions);

            //store by outputId
            predictions.put(entryByOutput.getKey(), theResult);

            modelEvaluations.putAll(entryByOutput.getKey(),resultsForThisId);
        }

        return new EvaluationResult(modelEvaluations,predictions);
    }


    private static void voteByModel(final float [][] votes,final Map<String,ModelVotingInfo> voteInfoByModelId,final MultiEvalHmmDecodedResult result) {
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

    private static void normalizeVotes(final float [][] votes) {
        final int N = votes.length;

        if (N == 0) {
            return;
        }

        final int T = votes[0].length;

        for (int t = 0; t < T; t++) {
            double sum = 0.0;
            for (int i = 0; i < N; i++) {
                sum += votes[i][t];
            }

            if (sum > 0.0) {
                for (int i = 0; i < N; i++) {
                    votes[i][t] /= sum;
                }
            }
        }
    }

    private static class ScoreWithIndex implements Comparable<ScoreWithIndex> {
        public ScoreWithIndex(double score, int index) {
            this.score = score;
            this.index = index;
        }

        private final double score;
        private final int index;

        //sort descending
        @Override
        public int compareTo(final ScoreWithIndex o) {
            if (this.score < o.score) {
                return 1;
            }

            if (this.score > o.score) {
                return -1;
            }

            return 0;
        }
    }

    private static int [] getVotedPathWithConstraints(final float[][] votes, boolean isSleep) {
        final int N = votes.length;
        final int T = votes[0].length;

        int [] votepath = new int[T];

        for (int t = 0; t < T; t++) {
            double max = 0;

            final List<ScoreWithIndex> scoreList = Lists.newArrayList();
            for (int i = 0; i < N; i++) {
                scoreList.add(new ScoreWithIndex(votes[i][t],i));
            }

            Collections.sort(scoreList);

            int maxIdx = scoreList.get(0).index;

            if (scoreList.get(0).index == LabelMaker.LABEL_DURING_SLEEP && isSleep) {
                if (scoreList.get(0).score < MIN_VOTE_PERCENT_TO_BE_IN_SLEEP) {
                    maxIdx = scoreList.get(1).index;
                }
            }

            if (!isSleep && votes[LabelMaker.LABEL_DURING_BED ][t] > MIN_VOTE_PERCENT_TO_BE_IN_BED) {
                maxIdx = LabelMaker.LABEL_DURING_BED;
            }


            //TODO vote on based on probability transitions to find various hypotheses



            votepath[t] = maxIdx;

        }

        return votepath;
    }

}
