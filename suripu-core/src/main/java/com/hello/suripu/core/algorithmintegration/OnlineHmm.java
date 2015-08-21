package com.hello.suripu.core.algorithmintegration;
import com.hello.suripu.algorithm.hmm.MultiObsSequenceAlphabetHiddenMarkovModel;
import com.hello.suripu.core.algorithmintegration.OnlineHmmSensorDataBinning.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.util.DeserializedFeatureExtractionWithParams;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 8/20/15.
 */
public class OnlineHmm {
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
        if (userModelData.scratchPad.isPresent()) {
            final OnlineHmmScratchPad scratchPad = userModelData.scratchPad.get();

            //check to see if this scratchpad is old enough
            //old enough == it was created yesterday or earlier
            //if (scratchPad.createdTimeUtc < startTimeUtc - 60000L * 60L


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
