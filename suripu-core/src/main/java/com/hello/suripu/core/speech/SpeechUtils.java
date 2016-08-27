package com.hello.suripu.core.speech;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Created by ksg on 8/10/16
 */
public class SpeechUtils {
    public static List<String> wakewordsMapToDDBAttribute(final Map<String, Float> wakeWordsMaps) {
        // get wake word confidence vector
        final List<String> confidences = Lists.newArrayList();

        for (final WakeWord word : WakeWord.values()) {
            if (!word.equals(WakeWord.ERROR)) {
                final String wakeWord = word.getWakeWordText();
                if (wakeWordsMaps.containsKey(wakeWord)) {
                    final String value = String.format("%s_%s", wakeWord.toLowerCase(), wakeWordsMaps.get(wakeWord).toString());
                    confidences.add(value);
                }
            }
        }
        return confidences;
    }


}
