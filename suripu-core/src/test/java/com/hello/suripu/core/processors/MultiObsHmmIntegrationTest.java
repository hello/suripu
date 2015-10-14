package com.hello.suripu.core.processors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.algorithmintegration.LabelMaker;
import com.hello.suripu.core.algorithmintegration.MultiEvalHmmDecodedResult;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.algorithmintegration.OnlineHmmModelEvaluator;
import com.hello.suripu.core.algorithmintegration.OnlineHmmSensorDataBinning;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmModelParams;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import com.hello.suripu.core.models.TimelineFeedback;
import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by benjo on 8/25/15.
 */
public class MultiObsHmmIntegrationTest {
    private static final Logger STATIC_LOGGER = LoggerFactory.getLogger(MultiObsHmmIntegrationTest.class);

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
            final byte [] protobuf = HmmUtils.loadFile("fixtures/algorithm/allfoureventsmodel.bin");
            final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(model.isPresent());

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            List<Map<String,ImmutableList<Integer>>> featureData = HmmUtils.getFeatureDataFromFile("fixtures/algorithm/1012-August2.json");
            TestCase.assertFalse(featureData.isEmpty());

            //evaluate
            final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(Optional.<UUID>absent());

            final Map<String,ImmutableList<Integer>> features = featureData.get(0);
            final Map<String,MultiEvalHmmDecodedResult> results = evaluator.evaluate(model.get(),features);

            TestCase.assertTrue(results.size() == 2);
            TestCase.assertTrue(results.containsKey("SLEEP"));
            TestCase.assertTrue(results.containsKey("BED"));

            final SleepEvents<Optional<Event>> events = OnlineHmm.getSleepEventsFromPredictions(results, 0, 5, 0, STATIC_LOGGER);

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

            final SleepEvents<Optional<Event>> eventsMatched = OnlineHmm.getSleepEventsFromPredictions(results, 0, 5, 0, STATIC_LOGGER);
            TestCase.assertTrue(eventsMatched.goToBed.get().getStartTimestamp() + 60000L == eventsMatched.fallAsleep.get().getStartTimestamp());
            TestCase.assertTrue(eventsMatched.outOfBed.get().getStartTimestamp() == eventsMatched.wakeUp.get().getStartTimestamp() + 60000L);

        } catch (IOException e) {
            TestCase.assertTrue(false);
        }

    }

    @Test
    public void testMultiDayEvaluation() {
        //reference from C++ code
        /*  for wakes that require two consecutive periods
        final Transition [] wakes = {
                new Transition( 1,2,158),
                new Transition(1,2,147),
                new Transition(1,2,151),
                new Transition(1,2,132),
                new Transition(1,2,143),
                new Transition(1,2,151),
                new Transition(1,2,174),
                new Transition(1,2,142),
                new Transition(1,2,145),
                new Transition(1,2,159)};
         */
        final Transition [] wakes = {
                new Transition( 1,2,166),
                new Transition(1,2,147),
                new Transition(1,2,158),
                new Transition(1,2,132),
                new Transition(1,2,145),
                new Transition(1,2,151),
                new Transition(1,2,174),
                new Transition(1,2,142),
                new Transition(1,2,160),
                new Transition(1,2,177)};

        final Transition [] sleeps = {
                new Transition(0,1,55),
                new Transition(0,1,85),
                new Transition(0,1,54),
                new Transition(0,1,51),
                new Transition(0,1,44),
                new Transition(0,1,42),
                new Transition(0,1,70),
                new Transition(0,1,73),
                new Transition(0,1,48),
                new Transition(0,1,35)};




        try {
            //get model
            final byte [] protobuf = HmmUtils.loadFile("fixtures/algorithm/default_model.bin");
            final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(model.isPresent());

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            List<Map<String,ImmutableList<Integer>>> featureData = HmmUtils.getFeatureDataFromFile("fixtures/algorithm/1012-August2.json");
            TestCase.assertFalse(featureData.isEmpty());

            //evaluate
            final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(Optional.<UUID>absent());
            final String outputId = "SLEEP";

            int count = 0;
            for (final Map<String,ImmutableList<Integer>> features : featureData) {

                final Map<String,MultiEvalHmmDecodedResult> results = evaluator.evaluate(model.get(),features);

                final List<Transition> transitions = results.get(outputId).transitions;

                for (final Transition transition : transitions) {
                    if (transition.fromState == 0) {
                        TestCase.assertEquals(sleeps[count].idx,transition.idx,1);
                    }
                    else if (transition.fromState == 1) {
                        TestCase.assertEquals(wakes[count].idx,transition.idx,1);
                    }
                }

                count++;
            }


        } catch (IOException e) {
            TestCase.assertTrue(false);
        }


    }



    public void testReestimation() {

        try {
            final Transition [] sleeps = {
                    new Transition(0,1,51),
                    new Transition(0,1,39),
                    new Transition(0,1,39),
                    new Transition(0,1,39),
                    new Transition(0,1,36),
                    new Transition(0,1,45),
                    new Transition(0,1,46),
                    new Transition(0,1,44),
                    new Transition(0,1,39),
                    new Transition(0,1,41),
                    new Transition(0,1,41),
                    new Transition(0,1,28),
                    new Transition(0,1,42),
                    new Transition(0,1,32),
                    new Transition(0,1,44),
                    new Transition(0,1,33),
                    new Transition(0,1,36),
                    new Transition(0,1,34),
                    new Transition(0,1,39),
                    new Transition(0,1,39)};
/* if there are two motion periods required to wake
            final Transition [] wakes = {
                    new Transition(1,2,124),
                    new Transition(1,2,137),
                    new Transition(1,2,124),
                    new Transition(1,2,180),
                    new Transition(1,2,127),
                    new Transition(1,2,124),
                    new Transition(1,2,132),
                    new Transition(1,2,131),
                    new Transition(1,2,113),
                    new Transition(1,2,114),
                    new Transition(1,2,113),
                    new Transition(1,2,113),
                    new Transition(1,2,125),
                    new Transition(1,2,127),
                    new Transition(1,2,136),
                    new Transition(1,2,117),
                    new Transition(1,2,101),
                    new Transition(1,2,113),
                    new Transition(1,2,113),
                    new Transition(1,2,116)};
*/

            final Transition [] wakes = {
                    new Transition(1,2,127),
                    new Transition(1,2,137),
                    new Transition(1,2,129),
                    new Transition(1,2,170),
                    new Transition(1,2,127),
                    new Transition(1,2,124),
                    new Transition(1,2,132),
                    new Transition(1,2,131),
                    new Transition(1,2,113),
                    new Transition(1,2,116),
                    new Transition(1,2,113),
                    new Transition(1,2,107),
                    new Transition(1,2,125),
                    new Transition(1,2,127),
                    new Transition(1,2,136),
                    new Transition(1,2,117),
                    new Transition(1,2,101),
                    new Transition(1,2,113),
                    new Transition(1,2,113),
                    new Transition(1,2,116)};
            //get model
            final byte[] protobuf = HmmUtils.loadFile("fixtures/algorithm/default_model.bin");
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

            final String outputId = "SLEEP";
            String modelId = "default";

            final Map<String,String> modelIds = Maps.newHashMap();
            modelIds.put(outputId, modelId);

            for (int count = 0;  count < featureData.size(); count++) {
                final OnlineHmmScratchPad scratchPad = evaluator.reestimate(modelIds, model, featureData.get(count), labels.get(count), 0);

                if (!scratchPad.paramsByOutputId.containsKey(outputId)) {
                    continue;
                }

                modelId = scratchPad.paramsByOutputId.get(outputId).id;

                model.modelsByOutputId.get(outputId).remove(modelId);
                model.modelsByOutputId.get(outputId).put(modelId, scratchPad.paramsByOutputId.get(outputId));
                modelIds.put(outputId,modelId);

            }

            for (int count = 0;  count < featureData.size(); count++) {
                final Map<String, MultiEvalHmmDecodedResult> results = evaluator.evaluate(model, featureData.get(count));

                //System.out.print(String.format("COST: %f\n",results.get(outputId).pathcost));
                List<Transition> transitions = results.get(outputId).transitions;

                for (final Transition transition : transitions) {

                    if (transition.fromState == 0) {
                        TestCase.assertEquals(sleeps[count].idx,transition.idx,1);
                    }
                    else if (transition.fromState == 1) {
                        TestCase.assertEquals(wakes[count].idx, transition.idx, 1);
                    }
                }

                /*
                for (Transition t : transitions) {
                    int hour = t.idx / 12;
                    int frac = t.idx % 12;

                    int minute = frac * 5;

                    hour -= 4;

                    if (hour < 0) {
                        hour += 24;
                    }

                    String foo = String.format("%d --> %d at %2d:%02d\n",t.fromState,t.toState,hour,minute);
                    System.out.print(foo);
                }

                System.out.print("----------------\n\n");


                */

            }


        } catch (IOException e) {
            TestCase.assertTrue(false);
        }
    }

    @Test
    public void testUpdateModelsWithScratchpad() {
        //get model
        try {
            final byte[] protobuf = HmmUtils.loadFile("fixtures/algorithm/34124.bin");
            final Optional<OnlineHmmPriors> modelOptional = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(modelOptional.isPresent());
            final OnlineHmmPriors model = modelOptional.get();

            final Map<String,OnlineHmmModelParams> modelUpdates = Maps.newHashMap();
            final OnlineHmmModelParams params = model.modelsByOutputId.get("SLEEP").get("default-2");
            final OnlineHmmModelParams params2 = model.modelsByOutputId.get("SLEEP").get("default-3");

            modelUpdates.put("SLEEP",params);

            final Map<String,OnlineHmmModelParams> existingModel = model.modelsByOutputId.get("SLEEP");

            for (final String key : existingModel.keySet()) {
                STATIC_LOGGER.info("existing model: {}", key);
            }

            for (int i = 0; i < OnlineHmm.MAXIMUM_NUMBER_OF_MODELS_PER_USER_PER_OUTPUT; i++) {
                final String newId = String.format("foobars%03d", i);
                STATIC_LOGGER.debug("adding model {}", newId);

                model.modelsByOutputId.get("SLEEP").put(newId,params2.clone(newId));
            }

            final OnlineHmmScratchPad scratchPad = new OnlineHmmScratchPad(modelUpdates,0);

            OnlineHmmPriors updateModel = OnlineHmm.updateModelPriorsWithScratchpad(model, scratchPad, 1, false, STATIC_LOGGER);

            for (final String key : updateModel.modelsByOutputId.keySet()) {
                STATIC_LOGGER.info("{}", key);
            }


            final Map<String,OnlineHmmModelParams> sleepModels = updateModel.modelsByOutputId.get("SLEEP");

            TestCase.assertFalse(sleepModels == null);


            final int numberOfModelsExpected = OnlineHmm.MAXIMUM_NUMBER_OF_MODELS_PER_USER_PER_OUTPUT + 1;
            STATIC_LOGGER.info("number of sleep models: {}, expected: {}", sleepModels.size(),numberOfModelsExpected);
            final int size = sleepModels.size();

            TestCase.assertTrue(size == numberOfModelsExpected);
            
        }
        catch (IOException e) {
            TestCase.assertTrue(false);
        }

      //  OnlineHmmPriors onlineHmmPriors = OnlineHmmPriors.createFromProtoBuf()
       // OnlineHmm.updateModelPriorsWithScratchpad()
    }

}
