package com.hello.suripu.core.processors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.core.algorithmintegration.LabelMaker;
import com.hello.suripu.core.algorithmintegration.MultiEvalHmmDecodedResult;
import com.hello.suripu.core.algorithmintegration.OnlineHmm;
import com.hello.suripu.core.algorithmintegration.OnlineHmmModelEvaluator;
import com.hello.suripu.core.algorithmintegration.OnlineHmmSensorDataBinning;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 8/25/15.
 */
public class MultiObsHmmIntegrationTest {

    private static byte[] loadFile(String path) throws IOException {
        final URL fileUrl = Resources.getResource(path);
        final File file = new File(fileUrl.getFile());
        final InputStream is = new FileInputStream(file);

        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
        byte[] bytes = new byte[(int)length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file "+file.getName());
        }

        is.close();
        return bytes;
    }


    private static List<Map<String,ImmutableList<Integer>>> getFeatureDataFromFile(final String path) {
        final ObjectMapper mapper = new ObjectMapper();
        final URL fileUrl = Resources.getResource(path);
        final File file = new File(fileUrl.getFile());

        try {
            List<Map<String,ImmutableList<Integer>>> result = Lists.newArrayList();

            final AlphabetsAndLabels [] paths = mapper.readValue(file, AlphabetsAndLabels [].class);

            for (int i = 0; i < paths.length; i++) {

                final Map<String,ImmutableList<Integer>> featuresById = Maps.newHashMap();

                for (Map.Entry<String,List<Integer>> entry : paths[i].alphabets.entrySet()) {
                    featuresById.put(entry.getKey(),ImmutableList.copyOf(entry.getValue()));
                }

                result.add(featuresById);
            }

            return result;


        }
        catch (Exception e) {
            return Lists.newArrayList();
        }
    }

    private static Map<String,List<Long>> feedbacksToEventTimesByTime(final List<FeedbackAsIndices> feedbackAsIndices) {

        Map<String,List<Long>> results = Maps.newHashMap();

        for (final FeedbackAsIndices feedback : feedbackAsIndices) {
            final Long time = feedback.updatedIndex * 5 * 60000L;

            if (results.get(feedback.type) == null) {
                results.put(feedback.type,Lists.<Long>newArrayList());
            }

            results.get(feedback.type).add(time);

        }

        return results;
    }

    private static List<Map<String,Map<Integer,Integer>>> getLabelsFromFile(final String path) {
        final ObjectMapper mapper = new ObjectMapper();
        final URL fileUrl = Resources.getResource(path);
        final File file = new File(fileUrl.getFile());

        final LabelMaker labelMaker = new LabelMaker(Optional.<UUID>absent());

        final List<Map<String,Map<Integer,Integer>>> result = Lists.newArrayList();

        try {

            final AlphabetsAndLabels [] paths = mapper.readValue(file, AlphabetsAndLabels [].class);

            for (int i = 0; i < paths.length; i++) {

                final Map<String,List<Long>> feedbacks = feedbacksToEventTimesByTime(paths[i].feedback);

                List<Long> wakes = feedbacks.get("WAKE_UP");
                List<Long> sleeps = feedbacks.get("SLEEP");

                if (wakes == null) {
                    wakes = Lists.newArrayList();
                }

                if (sleeps == null) {
                    sleeps = Lists.newArrayList();
                }

                result.add(labelMaker.getLabelsFromEvent(0, 60000L * 60 * 16, 5, wakes, sleeps));

            }

            return result;


        }
        catch (Exception e) {
            return result;
        }
    }


    @Test
    public void testMultiDayEvaluation() {
        //reference from C++ code
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
            final byte [] protobuf = loadFile("fixtures/algorithm/default_model.bin");
            final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(model.isPresent());

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            List<Map<String,ImmutableList<Integer>>> featureData = getFeatureDataFromFile("fixtures/algorithm/1012-August2.json");
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


    @Test
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

            //get model
            final byte[] protobuf = loadFile("fixtures/algorithm/default_model.bin");
            final Optional<OnlineHmmPriors> modelOptional = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(modelOptional.isPresent());
            final OnlineHmmPriors model = modelOptional.get();

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            final String modelFilename = "fixtures/algorithm/36584.json";
            final List<Map<String, ImmutableList<Integer>>> featureData = getFeatureDataFromFile(modelFilename);
            final List<Map<String,Map<Integer,Integer>>> labels = getLabelsFromFile(modelFilename);
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
                        TestCase.assertEquals(wakes[count].idx,transition.idx,1);
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

    void testUpdateModelsWithScratchpad() {

      //  OnlineHmmPriors onlineHmmPriors = OnlineHmmPriors.createFromProtoBuf()
       // OnlineHmm.updateModelPriorsWithScratchpad()
    }

}
