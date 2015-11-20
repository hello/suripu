package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.EvaluationResult;
import com.hello.suripu.core.algorithmintegration.LabelMaker;
import com.hello.suripu.core.algorithmintegration.ModelVotingInfo;
import com.hello.suripu.core.algorithmintegration.MultiEvalHmmDecodedResult;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.algorithmintegration.OnlineHmmModelEvaluator;
import com.hello.suripu.core.algorithmintegration.OnlineHmmModelLearner;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.TimelineFeedback;
import com.hello.suripu.core.util.FeatureExtractionModelData;
import junit.framework.TestCase;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 8/25/15.
 */
public class MultiObsHmmIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiObsHmmIntegrationTest.class);

    final static class LocalDefaultModelEnsembleDAO implements com.hello.suripu.core.db.DefaultModelEnsembleDAO {

        @Override
        public OnlineHmmPriors getDefaultModelEnsemble() {

            //get model
            try {
                final byte [] protobuf = HmmUtils.loadFile("fixtures/algorithm/normal3ensemble.model",false);
                final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);

                TestCase.assertTrue(model.isPresent());
                return model.get();

            } catch (IOException exception) {
                TestCase.assertTrue(false);
            }

            return OnlineHmmPriors.createEmpty();
        }

        @Override
        public OnlineHmmPriors getSeedModel() {
            //get model
            try {
                final byte [] protobuf = HmmUtils.loadFile("fixtures/algorithm/normal3.model",false);
                final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);

                TestCase.assertTrue(model.isPresent());
                return model.get();

            } catch (IOException exception) {
                TestCase.assertTrue(false);
            }

            return OnlineHmmPriors.createEmpty();
        }
    }

    final static class LocalFeatureExtractionDAO implements FeatureExtractionModelsDAO {
        FeatureExtractionModelData deserialization = null;

        public LocalFeatureExtractionDAO() {
            try {
                deserialization = new FeatureExtractionModelData(Optional.<UUID>absent());
                deserialization.deserialize(HmmUtils.loadFile("fixtures/algorithm/featureextractionlayer.bin",true));
            }
            catch (IOException exception) {
                TestCase.assertTrue(false);
                deserialization = null;
            }
        }

        @Override
        public FeatureExtractionModelData getLatestModelForDate(Long accountId, DateTime dateTimeLocalUTC, Optional<UUID> uuidForLogger) {
            return deserialization;
        }
    };

    static private Set<Integer> getUniqueValues( final Map<Integer,Integer> labels) {

        final Set<Integer> uniqueValues = Sets.newHashSet();
        for (final Map.Entry<Integer,Integer> entry : labels.entrySet()) {
            uniqueValues.add(entry.getValue());
        }

        return uniqueValues;
    }

    @Test
    public void testLabelMaker() {
        final LabelMaker labelMaker = new LabelMaker(Optional.<UUID>absent());

        final TimelineFeedback inbed = TimelineFeedback.create("1970-01-01","22:00","22:00",Event.Type.IN_BED,42L);
        final TimelineFeedback sleep = TimelineFeedback.create("1970-01-01","23:00","23:00",Event.Type.SLEEP,42L);
        final TimelineFeedback wake = TimelineFeedback.create("1970-01-01","07:00","07:00",Event.Type.WAKE_UP,42L);
        final TimelineFeedback outofbed = TimelineFeedback.create("1970-01-01","08:00","08:00",Event.Type.OUT_OF_BED,42L);

        final List<TimelineFeedback> partialSleep = Lists.newArrayList(sleep);
        final List<TimelineFeedback> partialWake = Lists.newArrayList(wake);
        final List<TimelineFeedback> allSleep = Lists.newArrayList(sleep,wake);

        final List<TimelineFeedback> partialInBed = Lists.newArrayList(inbed);
        final List<TimelineFeedback> partialOutOfBed = Lists.newArrayList(outofbed);
        final List<TimelineFeedback> allBeds = Lists.newArrayList(inbed,outofbed);

        final List<TimelineFeedback> allEvents = Lists.newArrayList(inbed,sleep, wake, outofbed);


        final long t1 = 3600000L * 20;
        final long t2 = t1 + 3600000L * 16;
        final int interval = 5;
        final int tzOffset = 0;
        final int idxInBed = 12 * 2 + 1;
        final int idxSleep = 12 * 3 + 1;
        final int idxWake = 12 * 11 - 1;
        final int idxOutOfBed = 12 * 12 - 1;

        final int expectedNumberOfLabels = (int) ((t2 - t1) / 60000L / interval);

        {
            final Map<String, Map<Integer, Integer>> labels = labelMaker.getLabelsFromEvents(tzOffset,t1,t2,interval, ImmutableList.copyOf(allEvents));

            TestCase.assertTrue(labels.keySet().contains(OnlineHmmData.OUTPUT_MODEL_SLEEP));
            TestCase.assertEquals(expectedNumberOfLabels,labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).size());
            TestCase.assertEquals(3,getUniqueValues(labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP)).size());

            TestCase.assertEquals(LabelMaker.LABEL_PRE_SLEEP, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).get(0));
            TestCase.assertEquals(LabelMaker.LABEL_DURING_SLEEP, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).get(idxSleep));
            TestCase.assertEquals(LabelMaker.LABEL_POST_SLEEP, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).get(expectedNumberOfLabels - 1));


            TestCase.assertTrue(labels.keySet().contains(OnlineHmmData.OUTPUT_MODEL_BED));
            TestCase.assertEquals(expectedNumberOfLabels,labels.get(OnlineHmmData.OUTPUT_MODEL_BED).size());
            TestCase.assertEquals(3,getUniqueValues(labels.get(OnlineHmmData.OUTPUT_MODEL_BED)).size());
            TestCase.assertEquals(LabelMaker.LABEL_PRE_BED, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_BED).get(0));
            TestCase.assertEquals(LabelMaker.LABEL_DURING_BED, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_BED).get(idxInBed));
            TestCase.assertEquals(LabelMaker.LABEL_POST_BED, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_BED).get(expectedNumberOfLabels - 1));


        }

        {
            final Map<String, Map<Integer, Integer>> labels = labelMaker.getLabelsFromEvents(tzOffset,t1,t2,interval, ImmutableList.copyOf(partialSleep));
            TestCase.assertTrue(labels.keySet().contains(OnlineHmmData.OUTPUT_MODEL_SLEEP));
            TestCase.assertFalse(labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).isEmpty());
            TestCase.assertEquals(2,getUniqueValues(labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP)).size());

            //make sure first is there
            TestCase.assertEquals(LabelMaker.LABEL_PRE_SLEEP, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).get(0));
            TestCase.assertEquals(LabelMaker.LABEL_DURING_SLEEP, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).get(idxSleep));
        }

        {
            final Map<String, Map<Integer, Integer>> labels = labelMaker.getLabelsFromEvents(tzOffset,t1,t2,interval, ImmutableList.copyOf(partialWake));
            TestCase.assertTrue(labels.keySet().contains(OnlineHmmData.OUTPUT_MODEL_SLEEP));
            TestCase.assertFalse(labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).isEmpty());
            TestCase.assertEquals(2,getUniqueValues(labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP)).size());

            //make sure last is there
            TestCase.assertEquals(LabelMaker.LABEL_POST_SLEEP, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).get(expectedNumberOfLabels - 1));
            TestCase.assertEquals(LabelMaker.LABEL_DURING_SLEEP, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_SLEEP).get(idxWake - 2));
        }

        {
            final Map<String, Map<Integer, Integer>> labels = labelMaker.getLabelsFromEvents(tzOffset,t1,t2,interval, ImmutableList.copyOf(partialInBed));
            TestCase.assertTrue(labels.keySet().contains(OnlineHmmData.OUTPUT_MODEL_BED));
            TestCase.assertFalse(labels.get(OnlineHmmData.OUTPUT_MODEL_BED).isEmpty());
            TestCase.assertEquals(2,getUniqueValues(labels.get(OnlineHmmData.OUTPUT_MODEL_BED)).size());

            //make sure first is there
            TestCase.assertEquals(LabelMaker.LABEL_PRE_BED, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_BED).get(0));
            TestCase.assertEquals(LabelMaker.LABEL_DURING_BED, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_BED).get(idxInBed));

        }

        {
            final Map<String, Map<Integer, Integer>> labels = labelMaker.getLabelsFromEvents(tzOffset,t1,t2,interval, ImmutableList.copyOf(partialOutOfBed));
            TestCase.assertTrue(labels.keySet().contains(OnlineHmmData.OUTPUT_MODEL_BED));
            TestCase.assertFalse(labels.get(OnlineHmmData.OUTPUT_MODEL_BED).isEmpty());
            TestCase.assertEquals(2,getUniqueValues(labels.get(OnlineHmmData.OUTPUT_MODEL_BED)).size());

            //make sure last is there
            TestCase.assertEquals(LabelMaker.LABEL_POST_BED, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_BED).get(expectedNumberOfLabels - 1));
            TestCase.assertEquals(LabelMaker.LABEL_DURING_BED, (int) labels.get(OnlineHmmData.OUTPUT_MODEL_BED).get(idxOutOfBed - 2));

        }
    }

    @Test
    public void testAllFourEvents() {
        try {
            //get model

            final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new LocalDefaultModelEnsembleDAO();
            final byte [] protobuf = HmmUtils.loadFile("fixtures/algorithm/normal3.model",false);
            final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(model.isPresent());

            final OnlineHmmPriors defaultEnsemble = defaultModelEnsembleDAO.getDefaultModelEnsemble();
            TestCase.assertFalse(defaultEnsemble.isEmpty());

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            List<Map<String,ImmutableList<Integer>>> featureData = HmmUtils.getFeatureDataFromFile("fixtures/algorithm/1012-August.json");
            TestCase.assertFalse(featureData.isEmpty());

            //evaluate
            final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(Optional.<UUID>absent());

            final Map<String,ImmutableList<Integer>> features = featureData.get(0);
            final EvaluationResult evaluationResult = evaluator.evaluate(defaultEnsemble, model.get(),features);
            final Map<String,MultiEvalHmmDecodedResult> results = evaluationResult.predictions;

            TestCase.assertTrue(results.size() == 2);
            TestCase.assertTrue(results.containsKey("SLEEP"));
            TestCase.assertTrue(results.containsKey("BED"));

            final SleepEvents<Optional<Event>> events = OnlineHmm.getSleepEventsFromPredictions(results, 0, 5, 0, LOGGER);

            TestCase.assertTrue(events.goToBed.get().getStartTimestamp() < events.fallAsleep.get().getStartTimestamp());
            TestCase.assertTrue(events.outOfBed.get().getStartTimestamp() > events.wakeUp.get().getStartTimestamp());


            //now we do something sneaky, by putting a copy of bed events in for sleep
            results.put("SLEEP",results.get("BED"));

            final MultiEvalHmmDecodedResult sleepResult = results.get("SLEEP");
            final MultiEvalHmmDecodedResult bedResult = results.get("BED");

            Collections.sort(sleepResult.transitions);
            Collections.sort(bedResult.transitions);

            TestCase.assertTrue(sleepResult.transitions.get(0).idx == bedResult.transitions.get(0).idx);
            TestCase.assertTrue(sleepResult.transitions.get(1).idx == bedResult.transitions.get(1).idx);

            final SleepEvents<Optional<Event>> eventsMatched = OnlineHmm.getSleepEventsFromPredictions(results, 0, 5, 0, LOGGER);
            TestCase.assertTrue(eventsMatched.goToBed.get().getStartTimestamp() + OnlineHmm.NUM_MINUTES_IT_TAKES_TO_FALL_ASLEEP*60000L == eventsMatched.fallAsleep.get().getStartTimestamp());
            TestCase.assertTrue(eventsMatched.outOfBed.get().getStartTimestamp() == eventsMatched.wakeUp.get().getStartTimestamp() + 60000L);

        } catch (IOException e) {
            TestCase.assertTrue(false);
        }

    }

    @Test
    public void testMultiDayEvaluation() {

        //just test and make sure sleep is greater than 4 hours.  Easy.
        try {
            //get model
            final byte [] protobuf = HmmUtils.loadFile("fixtures/algorithm/normal3.model",false);
            final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(model.isPresent());

            final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new LocalDefaultModelEnsembleDAO();
            final OnlineHmmPriors defaultEnsemble = defaultModelEnsembleDAO.getDefaultModelEnsemble();
            TestCase.assertFalse(defaultEnsemble.isEmpty());

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            List<Map<String,ImmutableList<Integer>>> featureData = HmmUtils.getFeatureDataFromFile("fixtures/algorithm/1012-August.json");
            TestCase.assertFalse(featureData.isEmpty());

            //evaluate
            final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(Optional.<UUID>absent());
            final String outputId = "SLEEP";

            for (final Map<String,ImmutableList<Integer>> features : featureData) {

                final EvaluationResult evaluationResult = evaluator.evaluate(defaultEnsemble, model.get(),features);
                final Map<String,MultiEvalHmmDecodedResult> results = evaluationResult.predictions;

                final int durationOfInterest = results.get(outputId).stateDurations[1];

                TestCase.assertTrue (durationOfInterest > 4 * 12.0);
            }


        } catch (IOException e) {
            TestCase.assertTrue(false);
        }


    }


    @Test
    public void testReestimation() {

        /*
            Test of one guy's data to see if learning makes the voter trust this guy's own custom model more.

         */

        try {
            final DefaultModelEnsembleDAO defaultModelEnsembleDAO = new LocalDefaultModelEnsembleDAO();
            final OnlineHmmPriors defaultEnsemble = defaultModelEnsembleDAO.getDefaultModelEnsemble();
            TestCase.assertFalse(defaultEnsemble.isEmpty());

            //get model
            final byte[] protobuf = HmmUtils.loadFile("fixtures/algorithm/normal3.model",false);
            final Optional<OnlineHmmPriors> modelOptional = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(modelOptional.isPresent());
            final OnlineHmmPriors model = modelOptional.get();

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            final String modelFilename = "fixtures/algorithm/36584.json";
            final List<Map<String, ImmutableList<Integer>>> featureData = HmmUtils.getFeatureDataFromFile(modelFilename);
            final List<Map<String,Map<Integer,Integer>>> labels = HmmUtils.getLabelsFromFile(modelFilename);
            TestCase.assertFalse(featureData.isEmpty());
            TestCase.assertTrue(labels.size() == featureData.size());

            final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(Optional.<UUID>absent());
            final OnlineHmmModelLearner learner = new OnlineHmmModelLearner(Optional.<UUID>absent());

            final String outputId = "SLEEP";
            String modelId = "default";

            final Map<String,String> modelIds = Maps.newHashMap();
            modelIds.put(outputId, modelId);

            int feedbackCount = 0;
            int bettercount = 0;
            for (int count = 0;  count < featureData.size(); count++) {
                final EvaluationResult evaluationResult = evaluator.evaluate(defaultEnsemble, model, featureData.get(count));
                final OnlineHmmScratchPad scratchPad = learner.reestimate(evaluationResult, model, featureData.get(count), labels.get(count), 0);



                if (!scratchPad.paramsByOutputId.containsKey(outputId)) {
                    continue;
                }

                if (!scratchPad.votingInfo.containsKey(outputId)) {
                    continue;
                }

                feedbackCount++;

                modelId = scratchPad.paramsByOutputId.get(outputId).id;

                model.modelsByOutputId.get(outputId).remove(modelId);
                model.modelsByOutputId.get(outputId).put(modelId, scratchPad.paramsByOutputId.get(outputId));
                modelIds.put(outputId,modelId);


                final Map<String,ModelVotingInfo> votingInfoMap =  scratchPad.votingInfo.get(outputId);

                final double voteWeightOfMyCustomModel = votingInfoMap.get(modelId).prob;

                double sum = 0.0;
                int modelcount = 0;
                for (final ModelVotingInfo info : votingInfoMap.values()) {
                    sum += info.prob;
                    modelcount++;
                }

                sum /= modelcount;

                //IS BETTER THAN AVERAGE MODEL WEIGHT?
                if (voteWeightOfMyCustomModel > sum) {
                    bettercount++;
                }

            }

            double successFraction = (double) bettercount / (double) feedbackCount;

            TestCase.assertTrue(successFraction > 0.5);



        } catch (IOException e) {
            TestCase.assertTrue(false);
        }
    }

}
