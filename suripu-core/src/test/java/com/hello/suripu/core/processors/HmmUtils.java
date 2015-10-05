package com.hello.suripu.core.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.hello.suripu.core.algorithmintegration.LabelMaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 9/15/15.
 */
public class HmmUtils {

    public static byte[] loadFile(String path) throws IOException {
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


    public static List<Map<String,ImmutableList<Integer>>> getFeatureDataFromFile(final String path) {
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

    public static Map<String,List<Long>> feedbacksToEventTimesByTime(final List<FeedbackAsIndices> feedbackAsIndices) {

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

    public static List<Map<String,Map<Integer,Integer>>> getLabelsFromFile(final String path) {
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

                result.add(labelMaker.getSleepLabelsFromEvents(0, 60000L * 60 * 16, 5, wakes, sleeps));

            }

            return result;


        }
        catch (Exception e) {
            return result;
        }
    }
}
