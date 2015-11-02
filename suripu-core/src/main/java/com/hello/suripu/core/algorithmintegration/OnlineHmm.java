package com.hello.suripu.core.algorithmintegration;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.OnlineHmmSensorDataBinning.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.logging.LoggerWithSessionId;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.DateTimeUtil;
import com.hello.suripu.core.util.DeserializedFeatureExtractionWithParams;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 8/20/15.
 *
 * This class integrates all the separate components of the online hmm predictor
 * -getting pre-classificaiton models (the feature extractors)
 * -pre-classifying
 * -loading the personalized model for the user
 * -evaluating the personalized models
 * -updating the model with labels
 * -saving the new models
 */
public class OnlineHmm {
    private final static long NUM_MILLIS_IN_A_MINUTE = 60000L;
    private static final long NUMBER_OF_MILLIS_IN_AN_HOUR = 3600000L;
    private static final long MAX_AGE_OF_TARGET_DATE_TO_UPDATE_SCRATCHPAD = 12 * NUMBER_OF_MILLIS_IN_AN_HOUR;

    public final static int MAXIMUM_NUMBER_OF_MODELS_PER_USER_PER_OUTPUT = 15;
    public final static Set<String> DEFAULT_MODEL_KEYS;

    static {
        DEFAULT_MODEL_KEYS = Sets.newHashSet();
        DEFAULT_MODEL_KEYS.add("default");
    }

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

    //static for testing purposes
    public static OnlineHmmPriors updateModelPriorsWithScratchpad(final OnlineHmmPriors existingModels, final OnlineHmmScratchPad newModel, final long startTimeUtc, boolean forceUpdate,final Logger logger) {
        final Map<String, List<OnlineHmmModelParams>> modelsByOutputId = Maps.newHashMap();


        //check to see if this scratchpad is old enough
        //old enough == it was created yesterday or earlier
        if (newModel.isEmpty()) {
            logger.info("no scratchpad");
            return OnlineHmmPriors.createEmpty();
        }

        if (newModel.lastUpdateTimeUtc >= startTimeUtc && !forceUpdate)  {
            logger.info("scratchpad not old enough -- not updating models");
            return OnlineHmmPriors.createEmpty();
        }

        final OnlineHmmPriors updatedModels = OnlineHmmPriors.createEmpty();

        //go through each output id
        for (final String outputId : existingModels.modelsByOutputId.keySet()) {

            //if this key does not exist in the updated models, just bring over the existing models
            if (!newModel.paramsByOutputId.containsKey(outputId)) {
                updatedModels.modelsByOutputId.put(outputId,existingModels.modelsByOutputId.get(outputId));
                logger.info("no update for {} found",outputId);
                continue;
            }

            //get models from the scratchpad for this output id
            final OnlineHmmModelParams param = newModel.paramsByOutputId.get(outputId);

            final List<OnlineHmmModelParams> modelsForThisOutput = Lists.newArrayList();

            //populate a new list (we will need to sort it later) for this output id
            modelsForThisOutput.addAll(existingModels.modelsByOutputId.get(outputId).values());
            
            //add the scratchpad model
            modelsForThisOutput.add(param);

            //sort by date last used and then updated
            Collections.sort(modelsForThisOutput, new Comparator<OnlineHmmModelParams>() {
                @Override
                public int compare(final OnlineHmmModelParams o1, final OnlineHmmModelParams o2) {
                    if (o1.timeUpdatedUtc < o2.timeUpdatedUtc) {
                        return 1;
                    }

                    if (o1.timeUpdatedUtc > o2.timeUpdatedUtc) {
                        return -1;
                    }

                    return 0;
                }
            });

            //ENFORCE MODEL LIMIT
            //exceed model count?  trim the oldest used
            for (int i = modelsForThisOutput.size() - 1; i >= 0; i--) {

                if (i <= MAXIMUM_NUMBER_OF_MODELS_PER_USER_PER_OUTPUT)  {
                    break;
                }

                //skip default model
                if (DEFAULT_MODEL_KEYS.contains(modelsForThisOutput.get(i).id)) {
                    continue;
                }

                logger.info("removing model {}:{} because it exceeded the model limit and was the oldest",outputId,modelsForThisOutput.get(i).id);
                modelsForThisOutput.remove(i);
            }

            //turn back into map by model id
            final Map<String,OnlineHmmModelParams> trimmedParams = Maps.newHashMap();

            for (final OnlineHmmModelParams params : modelsForThisOutput) {
                trimmedParams.put(params.id,params);
            }

            updatedModels.modelsByOutputId.put(outputId,trimmedParams);

        }



        return updatedModels;


    }

    public OnlineHmmData getReconciledModelsForUser(final long accountId, final DateTime evening) {
        final OnlineHmmData emptyResult = OnlineHmmData.createEmpty();

        /* GET THE USER-SPECIFIC MODEL PARAMETERS FOR THE ONE HMM TO RULE THEM ALL */
        final OnlineHmmData userModelData = userModelDAO.getModelDataByAccountId(accountId,evening);

        //sort out the differences between the default model and the user models
        OnlineHmmPriors modelPriors = userModelData.modelPriors;

        final Optional<OnlineHmmPriors> defaultPriorOptional = OnlineHmmPriors.createDefaultPrior();

        if (!defaultPriorOptional.isPresent() && userModelData.modelPriors.isEmpty()) {
            LOGGER.error("could not get valid default prior.  This is astoundingly bad.");
            return emptyResult;
        }

        final OnlineHmmPriors defaultPrior = defaultPriorOptional.get();

        /*  CREATE DEFAULT MODELS IF NECESSARY */
        if (userModelData.modelPriors.isEmpty()) {
            LOGGER.info("creating default model data for account {}",accountId);

            //straight out assign
            modelPriors = defaultPrior;

            //update dynamo
            userModelDAO.updateModelPriorsAndZeroOutScratchpad(accountId,evening,modelPriors);

        }


        /*  intentionally commented out -- BEJ 2015-10-09
        else {

            //verify that the default prior doesn't contain any more output ids
            //otherwise that means there are new outputs available in the default models
            modelPriors = userModelData.modelPriors;

            final Set<String> defaultKeys = Sets.newHashSet(defaultPrior.modelsByOutputId.keySet());
            defaultKeys.removeAll(modelPriors.modelsByOutputId.keySet());

            if (!defaultKeys.isEmpty()) {
                //ach, someone added a new model to the default
                LOGGER.info("updating user models with new default models with output ids {} ",defaultKeys);

                for (final String key : defaultKeys) {
                    modelPriors.modelsByOutputId.put(key,defaultPrior.modelsByOutputId.get(key));
                }
            }


            // TODO  last but not least, got through each model and see if it is missing anything that is in the default
            //i.e. I could have updated the feature extraction layer with a model that replaces "light2" with one called "light3"
            // "light3" does not exist in the user models, so we copy over that one from the default
        }*/


        return new OnlineHmmData(modelPriors,userModelData.scratchPad);
    }

    static private long indexToTimestamp(final long t0, final int periodInMinutes, final int idx) {
        return idx * periodInMinutes * NUM_MILLIS_IN_A_MINUTE  + t0;
    }

    //static for easy external testing
    public static SleepEvents<Optional<Event>> getSleepEventsFromPredictions(final Map<String,MultiEvalHmmDecodedResult> bestDecodedResultsByOutputId, final long t0,final int numMinutesInPeriod, final int tzOffset, final Logger logger) {
          /*  DO SOMETHING WITH THE BEST PREDICTIONS */

        Optional<Event> sleep = Optional.absent();
        Optional<Event> wake = Optional.absent();
        Optional<Event> inbed = Optional.absent();
        Optional<Event> outofbed = Optional.absent();

        for (final String outputId : bestDecodedResultsByOutputId.keySet()) {
            final MultiEvalHmmDecodedResult result = bestDecodedResultsByOutputId.get(outputId);

            if (result.transitions.size() < 2) {
                logger.info("not enough transitions found for output id {}",outputId);
                continue;
            }

            switch (outputId) {

                //TODO replace with a factory
                case OnlineHmmData.OUTPUT_MODEL_SLEEP:
                {
                    final int sleepIdx = result.transitions.get(0).idx;
                    final int wakeIdx = result.transitions.get(1).idx;

                    final long sleepTime = indexToTimestamp(t0,numMinutesInPeriod,sleepIdx);
                    final long wakeTime = indexToTimestamp(t0,numMinutesInPeriod,wakeIdx);

                    sleep = Optional.of(Event.createFromType(Event.Type.SLEEP, sleepTime, sleepTime + NUM_MILLIS_IN_A_MINUTE, tzOffset, Optional.of(English.FALL_ASLEEP_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));
                    wake = Optional.of(Event.createFromType(Event.Type.WAKE_UP,wakeTime,wakeTime + NUM_MILLIS_IN_A_MINUTE,tzOffset, Optional.of(English.WAKE_UP_MESSAGE),Optional.<SleepSegment.SoundInfo>absent(),Optional.<Integer>absent()));

                    break;
                }

                case OnlineHmmData.OUTPUT_MODEL_BED:
                {
                    final int inBedIdx = result.transitions.get(0).idx;
                    final int outOfBedIdx = result.transitions.get(1).idx;

                    final long inBedTime = indexToTimestamp(t0,numMinutesInPeriod,inBedIdx);
                    final long outOfBedTime = indexToTimestamp(t0,numMinutesInPeriod,outOfBedIdx);

                    inbed = Optional.of(Event.createFromType(Event.Type.IN_BED, inBedTime, inBedTime + NUM_MILLIS_IN_A_MINUTE, tzOffset, Optional.of(English.IN_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));
                    outofbed = Optional.of(Event.createFromType(Event.Type.OUT_OF_BED, outOfBedTime, outOfBedTime + NUM_MILLIS_IN_A_MINUTE, tzOffset, Optional.of(English.OUT_OF_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));


                    break;
                }

            }

        }

        //reconcile bed and sleep, giving priority to sleep

        if (inbed.isPresent() && sleep.isPresent()) {
            if (inbed.get().getStartTimestamp() >= sleep.get().getStartTimestamp()) {
                //need to adjust in-bed
                final long inBedTime = sleep.get().getStartTimestamp() - NUM_MILLIS_IN_A_MINUTE;
                inbed = Optional.of(Event.createFromType(Event.Type.IN_BED, inBedTime, inBedTime + NUM_MILLIS_IN_A_MINUTE, tzOffset, Optional.of(English.IN_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));
            }
        }

        if (outofbed.isPresent() && wake.isPresent()) {
            if (outofbed.get().getStartTimestamp() <= wake.get().getStartTimestamp()) {
                final long outOfBedTime = wake.get().getStartTimestamp() + NUM_MILLIS_IN_A_MINUTE;
                outofbed = Optional.of(Event.createFromType(Event.Type.OUT_OF_BED, outOfBedTime, outOfBedTime + NUM_MILLIS_IN_A_MINUTE, tzOffset, Optional.of(English.OUT_OF_BED_MESSAGE), Optional.<SleepSegment.SoundInfo>absent(), Optional.<Integer>absent()));
            }
        }

        return SleepEvents.create(inbed,sleep,wake,outofbed);
    }

    static ImmutableList<TimelineFeedback> filterFeedbackInValidTimeRange(final DateTime date, final ImmutableList<TimelineFeedback> feedbacks, final long startTimeUtc,final long endTimeUtc) {
        final List<TimelineFeedback> feedbackListFiltered = Lists.newArrayList();

        for (final TimelineFeedback feedback : feedbacks) {
            if (!feedback.dateOfNight.equals(date)) {
                continue;
            }

            if (!feedback.created.isPresent()) {
                continue;
            }

            final long createdTime = feedback.created.get();
            if (createdTime >= startTimeUtc && createdTime <= endTimeUtc) {
                feedbackListFiltered.add(feedback);
            }

        }

        return ImmutableList.copyOf(feedbackListFiltered);
    }


    public SleepEvents<Optional<Event>> predictAndUpdateWithLabels(final long accountId,final DateTime evening, final DateTime startTimeLocalUtc, final DateTime endTimeLocalUtc,
                           OneDaysSensorData oneDaysSensorData, boolean feedbackHasChanged,boolean forceLearning) {

        /*  GET THE FEATURE EXTRACTION LAYER -- this will be as bunch of HMMs that will classify binned sensor data into discrete classes
        *                                    -- it's the time-series equivalent of finding which cluster a data point belongs to
        */

        SleepEvents<Optional<Event>> predictions = SleepEvents.create(Optional.<Event>absent(),Optional.<Event>absent(),Optional.<Event>absent(),Optional.<Event>absent());

        final int timezoneOffset = oneDaysSensorData.sensorDataTimeSpanInfo.getOffsetAtTime(oneDaysSensorData.sensorDataTimeSpanInfo.startTimeUTC);
        final long startTimeUtc = startTimeLocalUtc.minusMillis(timezoneOffset).getMillis();
        final long endTimeUtc = endTimeLocalUtc.minusMillis(timezoneOffset).getMillis();
        final long endTimeToUpdateFeedback = endTimeUtc + MAX_AGE_OF_TARGET_DATE_TO_UPDATE_SCRATCHPAD;
        final ImmutableList<TimelineFeedback> feedbackList = oneDaysSensorData.feedbackList;

        final FeatureExtractionModelData serializedData = featureExtractionModelsDAO.getLatestModelForDate(accountId, startTimeLocalUtc, uuid);

        if (!serializedData.isValid()) {
            LOGGER.error("failed to get feature extraction layer!");
            return predictions;
        }

        final DeserializedFeatureExtractionWithParams featureExtractionModels = serializedData.getDeserializedData();


        final OnlineHmmData userModelData = getReconciledModelsForUser(accountId,evening);

        if (userModelData.modelPriors.isEmpty()) {
            LOGGER.error("somehow we did not get a model prior, so we are not outputting anything");
            return predictions;
        }

        OnlineHmmPriors modelPriors = userModelData.modelPriors;

        if (modelPriors == null) {
            LOGGER.error("somehow never got model priors for account {}",accountId);
            return predictions;
        }

        /*  CHECK TO SEE IF THE SCRATCH PAD SHOULD BE ADDED TO THE CURRENT MODEL */
        if (!userModelData.scratchPad.isEmpty()) {
            final OnlineHmmScratchPad scratchPad = userModelData.scratchPad;

            if (!scratchPad.isEmpty()) {

                //MANAGE THE TANGLE OF MODELS
                final OnlineHmmPriors updatedModelPriors = updateModelPriorsWithScratchpad(modelPriors, scratchPad, startTimeUtc, false,LOGGER);

                if (!updatedModelPriors.isEmpty()) {
                    //UPDATE THE MODEL IN DYNAMO
                    userModelDAO.updateModelPriorsAndZeroOutScratchpad(accountId, evening,updatedModelPriors);

                    //USE THE NEW PRIORS
                    modelPriors = updatedModelPriors;
                }

            }
        }


         /* GET THE BINNED SENSOR DATA -- this will take sensor data and aggregate in N minute chunks, plus produce some extra differential signals (light increase, etc.)   */
        final Optional<BinnedData> binnedDataOptional = OnlineHmmSensorDataBinning.getBinnedSensorData(
                oneDaysSensorData.allSensorSampleList,
                oneDaysSensorData.trackerMotions,
                oneDaysSensorData.partnerMotions,
                featureExtractionModels.params,
                startTimeUtc,
                endTimeUtc,
                timezoneOffset);


        if (!binnedDataOptional.isPresent()) {
            LOGGER.error("failed to get binned sensor data");
            return predictions;
        }


        final BinnedData binnedData = binnedDataOptional.get();

        /*  RUN THE FEATURE EXTRACTION LAYER */
        final Map<String,ImmutableList<Integer>> pathsByModelId = featureExtractionModels.sensorDataReduction.getPathsFromSensorData(binnedData.data);

        /*  EVALUATE AND FIND THE BEST MODELS */
        final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(uuid);

        final Map<String,MultiEvalHmmDecodedResult> bestDecodedResultsByOutputId = evaluator.evaluate(modelPriors,pathsByModelId);


        /* GET PREDICTIONS  */
        predictions = getSleepEventsFromPredictions(bestDecodedResultsByOutputId,binnedData.t0,binnedData.numMinutesInWindow,timezoneOffset,LOGGER);


        //get filtered feedback
        final ImmutableList<TimelineFeedback> filteredTimelineFeedback =  filterFeedbackInValidTimeRange(evening,oneDaysSensorData.feedbackList,startTimeUtc,endTimeToUpdateFeedback);

        boolean isFeedbackReady = feedbackHasChanged && !filteredTimelineFeedback.isEmpty();

        //override
        if (forceLearning && !filteredTimelineFeedback.isEmpty()) {
            isFeedbackReady = true;
        }


        /* PROCESS FEEDBACK, but only if it's ready  */
        if (isFeedbackReady) {
            //1) turn feedback into labels
            final LabelMaker labelMaker = new LabelMaker(uuid);
            final Map<String,Map<Integer,Integer>> labelsByOutputId = labelMaker.getLabelsFromEvents(timezoneOffset, binnedData.t0, endTimeUtc, binnedData.numMinutesInWindow, feedbackList);

            //2) reestimate on top of the models that were actually used by the user
            final Map<String,String> usedModelsByOutputId = Maps.newHashMap();

            for (final Map.Entry<String,MultiEvalHmmDecodedResult> entry : bestDecodedResultsByOutputId.entrySet()) {
                usedModelsByOutputId.put(entry.getKey(),entry.getValue().originatingModel);
            }

            final OnlineHmmScratchPad scratchPad = evaluator.reestimate(usedModelsByOutputId, modelPriors, pathsByModelId, labelsByOutputId, startTimeUtc);

            if (scratchPad.isEmpty()) {
                LOGGER.error("scratchpad is empty!!!");
            }
            else {
                final List<String> modelIds = Lists.newArrayList();
                for (final Map.Entry<String,OnlineHmmModelParams> entry : scratchPad.paramsByOutputId.entrySet()) {
                    modelIds.add(entry.getValue().id);
                }

                LOGGER.info("new scratchpad models: {}",modelIds);
                LOGGER.info("existing models: {}",modelPriors.getModelIds());

            }

            //3) update scratchpad in dynamo
            if (forceLearning) {
                //update right now
                final OnlineHmmPriors updatedModelPriors = updateModelPriorsWithScratchpad(modelPriors, scratchPad, startTimeUtc, true,LOGGER);

                if (!updatedModelPriors.isEmpty()) {
                    LOGGER.info("force updating models for date {}", DateTimeUtil.dateToYmdString(evening));
                    userModelDAO.updateModelPriorsAndZeroOutScratchpad(accountId, evening, updatedModelPriors);
                }
            }
            else {
                //otherwise just update the scratchpad
                userModelDAO.updateScratchpad(accountId,evening,scratchPad);
            }
        }


        return predictions;

    }

}
