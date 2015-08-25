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
import com.hello.suripu.core.algorithmintegration.MultiEvalHmmDecodedResult;
import com.hello.suripu.core.algorithmintegration.OnlineHmmModelEvaluator;
import com.hello.suripu.core.algorithmintegration.OnlineHmmSensorDataBinning;
import com.hello.suripu.core.models.OnlineHmmPriors;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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


    @Test
    public void testMultiDayEvaluation() {
        //reference from C++ code
        final Transition [] wakes = {
                new Transition(1,2,148),
                new Transition(1,2,141),
                new Transition(1,2,151),
                new Transition(1,2,132),
                new Transition(1,2,104),
                new Transition(1,2,151),
                new Transition(1,2,167),
                new Transition(1,2,142),
                new Transition(1,2,145),
                new Transition(1,2,159)};

        final Transition [] sleeps = {
                new Transition(0,1,55),
                new Transition(0,1,85),
                new Transition(0,1,54),
                new Transition(0,1,51),
                new Transition(0,1,45),
                new Transition(0,1,42),
                new Transition(0,1,70),
                new Transition(0,1,73),
                new Transition(0,1,48),
                new Transition(0,1,35)};




        try {
            //get model
            final byte [] protobuf = loadFile("fixtures/algorithm/default_model.proto");
            final Optional<OnlineHmmPriors> model = OnlineHmmPriors.createFromProtoBuf(protobuf);
            TestCase.assertTrue(model.isPresent());

            //get feature data -- it should be a list of days, each day has a bunch of key value pairs that correspond to different sensor data streams for that day
            List<Map<String,ImmutableList<Integer>>> featureData = getFeatureDataFromFile("fixtures/algorithm/1012-August2.json");
            TestCase.assertFalse(featureData.isEmpty());

            //evaluate
            final OnlineHmmModelEvaluator evaluator = new OnlineHmmModelEvaluator(Optional.<UUID>absent());

            int count = 0;
            for (final Map<String,ImmutableList<Integer>> features : featureData) {
                final ImmutableList<Integer> motionList = features.get("motion");

                TestCase.assertFalse(motionList == null);

                double [] motion = new double[motionList.size()];
                for (int i = 1; i < motionList.size(); i++) {
                    final int state = motionList.get(i);

                    if (state == 6 || state == 0) {
                        motion[i - 1] = 0;
                    }
                    else {
                        motion[i - 1] = 1;
                    }
                }

                final Map<String,Multimap<Integer, Transition>> forbiddenTransitionByOutputId = Maps.newHashMap();

                final String outputId = "SLEEP";

                //map forbidden transitions for motion into a map by time
                forbiddenTransitionByOutputId.put(outputId, OnlineHmmSensorDataBinning.getMotionForbiddenTransitions(model.get().forbiddenMotionTransitionsByOutputId.get(outputId),motion));

                final Map<String,MultiEvalHmmDecodedResult> results = evaluator.evaluate(model.get(),features,forbiddenTransitionByOutputId);

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
            e.printStackTrace();
        }


    }
}
