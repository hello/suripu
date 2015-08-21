package com.hello.suripu.core.algorithmintegration;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.hmm.MultiObsSequenceAlphabetHiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.core.algorithmintegration.OnlineHmmSensorDataBinning.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.util.DeserializedFeatureExtractionWithParams;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 8/20/15.
 */
public class OnlineHmm {
    public final static int MAXIMUM_NUMBER_OF_MODELS_PER_USER_PER_OUTPUT = 5;
    public final static String DEFAULT_MODEL_KEY = "default";

    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(OnlineHmm.class);
    private final Logger LOGGER;

    final FeatureExtractionModelsDAO featureExtractionModelsDAO;
    final OnlineHmmModelsDAO userModelDAO;
    final Optional<UUID> uuid;


    public OnlineHmm(final FeatureExtractionModelsDAO featureExtractionModelsDAO,final OnlineHmmModelsDAO userModelDAO,final Optional<UUID> uuid) {
        this.featureExtractionModelsDAO = featureExtractionModelsDAO;
        this.userModelDAO = userModelDAO;
        this.uuid = uuid;
        this.LOGGER = new LoggerWithSessionId(STATIC_LOGGER,uuid);
    }

    private OnlineHmmPriors updateModelPriors(final OnlineHmmPriors models, final OnlineHmmScratchPad newModel, final long startTimeUtc) {
        final Map<String, List<OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();
        final List<Transition> forbiddenMotionTransitions = models.forbiddenMotionTransitions;


        final OnlineHmmPriors updatedModels = models.clone();

        //check to see if this scratchpad is old enough
        //old enough == it was created yesterday or earlier
        if (newModel.lastUpdateTimeUtc < startTimeUtc) {

            //go through each and every model, first matching by outputid
            for (final String outputId : newModel.paramsByOutputId.keySet()) {
                final OnlineHmmModelParams param = newModel.paramsByOutputId.get(outputId);

                //find the existing model params with this id
                if (!updatedModels.modelsByOutputId.containsKey(outputId)) {
                    LOGGER.error("did not find models with output id = {}",outputId);
                    continue;
                }

                final List<OnlineHmmModelParams> modelsForThisOutput = Lists.newArrayList(updatedModels.modelsByOutputId.get(outputId));


                //add
                modelsForThisOutput.add(param);

                //sort by date last used
                Collections.sort(modelsForThisOutput, new Comparator<OnlineHmmModelParams>() {
                    @Override
                    public int compare(final OnlineHmmModelParams o1, final OnlineHmmModelParams o2) {
                        if (o1.timeUpdatedUtc < o2.timeUpdatedUtc) {
                            return -1;
                        }

                        if (o1.timeUpdatedUtc > o2.timeUpdatedUtc) {
                            return 1;
                        }

                        return 0;
                    }
                });

                //ENFORCE MODEL LIMIT
                //exceed model count?  trim the oldest
                for (int i = modelsForThisOutput.size() - 1; i >= 0; i--) {

                    if (modelsForThisOutput.size() <= MAXIMUM_NUMBER_OF_MODELS_PER_USER_PER_OUTPUT)  {
                        break;
                    }

                    modelsForThisOutput.remove(i);
                }

                updatedModels.modelsByOutputId.put(outputId,modelsForThisOutput);

            }

        }

        return updatedModels;


    }


    public boolean predict(final long accountId,final DateTime targetDate, final long startTimeUtc, final long endTimeUtc, final int timezoneOffset,
                           OneDaysSensorData oneDaysSensorData,final ImmutableList<TimelineFeedback> feedbackList, boolean feedbackHasChanged) {

        /*  GET THE FEATURE EXTRACTION LAYER -- this will be as bunch of HMMs that will classify binned sensor data into discrete classes
        *                                    -- it's the time-series equivalent of finding which cluster a data point belongs to
        */

        final FeatureExtractionModelData serializedData = featureExtractionModelsDAO.getLatestModelForDate(accountId, targetDate, uuid);

        if (!serializedData.isValid()) {
            LOGGER.error("failed to get feature extraction layer!");
            return false;
        }

        final DeserializedFeatureExtractionWithParams featureExtractionModels = serializedData.getDeserializedData();

         /* GET THE USER-SPECIFIC MODEL PARAMETERS FOR THE ONE HMM TO RULE THEM ALL */
        final OnlineHmmData userModelData = userModelDAO.getModelDataByAccountId(accountId);


        OnlineHmmPriors modelPriors = null;

        if (!userModelData.modelPriors.isPresent()) {
            LOGGER.info("creating default model data for account {}",accountId);

            //TODO create default model data and store it in dynamo
           // userModelDAO.updateModelPriors(...)

            //TODO compare models used in feature extraction layer vs what's in the model priors
            //if there's a new model in the feature extraction layer, go get the default for that model



        }

        if (modelPriors == null) {
            LOGGER.error("somehow never got model priors for account {}",accountId);
            return false;
        }

        /*  CHECK TO SEE IF THE SCRATCH PAD SHOULD BE ADDED TO THE CURRENT MODEL */
        /*
            Here's how this is supposed to work:
               if scratchpad is old enough (i.e. yesterday's or earlier), then we will add it to the model it was based from
               we will, however, create a new model

         */
        if (userModelData.scratchPad.isPresent()) {
            final OnlineHmmScratchPad scratchPad = userModelData.scratchPad.get();

            //MANAGE THE TANGLE OF MODELS
            final OnlineHmmPriors updatedModelPriors = updateModelPriors(modelPriors,scratchPad,startTimeUtc);

            //UPDATE THE MODEL IN DYNAMO
            userModelDAO.updateModelPriorsAndZeroOutScratchpad(accountId,updatedModelPriors);

        }


         /* GET THE BINNED SENSOR DATA -- this will take sensor data and aggregate in N minute chunks, plus produce some extra differential signals (light increase, etc.)   */
        final Optional<BinnedData> binnedDataOptional = OnlineHmmSensorDataBinning.getBinnedSensorData(
                oneDaysSensorData.allSensorSampleList,
                oneDaysSensorData.trackerMotions,
                oneDaysSensorData.partnerMotions,
                featureExtractionModels.params,
                startTimeUtc,
                endTimeUtc,
                timezoneOffset,
                modelPriors.forbiddenMotionTransitions);


        if (!binnedDataOptional.isPresent()) {
            LOGGER.error("failed to get binned sensor data");
            return false;
        }


        final BinnedData binnedData = binnedDataOptional.get();

        /*  RUN THE FEATURE EXTRACTION LAYER */
        final Map<String,ImmutableList<Integer>> pathsByModelId = featureExtractionModels.sensorDataReduction.getPathsFromSensorData(binnedData.data);

        /*  FIND THE BEST MODELS */
        final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(uuid);
        final Map<String,MultiEvalHmmDecodedResult> bestDecodedResultsByOutputId = evaluator.evaluate(modelPriors,pathsByModelId,binnedData.forbiddenTransitions);

        /*  DO SOMETHING WITH THE BEST PREDICTIONS */
        for (final String outputId : bestDecodedResultsByOutputId.keySet()) {
            final MultiEvalHmmDecodedResult result = bestDecodedResultsByOutputId.get(outputId);

            if (result.transitions.size() < 2) {
                LOGGER.info("not enough transitions found for output id {}",outputId);
                continue;
            }

            switch (outputId) {

                //TODO replace with a factory
                case OnlineHmmData.OUTPUT_MODEL_SLEEP:
                {
                    final int sleepIdx = result.transitions.get(0).idx;
                    final int wakeIdx = result.transitions.get(1).idx;

                    //TODO turn this into events
                    break;
                }

                case OnlineHmmData.OUTPUT_MODEL_BED:
                {
                    final int inBedIdx = result.transitions.get(0).idx;
                    final int outOfBedIdx = result.transitions.get(1).idx;
                    //TODO turn this into events

                    break;
                }

            }


/*

filtering the scratchpad inputs:
I gave feedback for the previous night—thats okay
I gave feedback for two days ago — let’s ignore that

 */

            /*
            if (feedbackHasChanged) {
                feedbackList.get(0).
                //MAKE A NEW FUCKING MODEL SCRATCHPAD
            }
            */
        }

       // MultiObsSequenceAlphabetHiddenMarkovModel uberHmm = new MultiObsSequenceAlphabetHiddenMarkovModel()

        return false;

    }

}
