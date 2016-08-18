package com.hello.suripu.core.speech;

import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Created by ksg on 8/10/16
 */
public class SpeechUtils {
    public static Set<Number> wakewordsMapToDDBAttribute(final Map<String, Float> wakeWordsMaps) {
        // get wake word confidence vector
        final Set<Number> confidences = Sets.newHashSet();
        for (final WakeWord word : WakeWord.values()) {
            if (!word.equals(WakeWord.ERROR)) {
                final String wakeWord = word.getWakeWordText();
                if (wakeWordsMaps.containsKey(wakeWord)) {
                    confidences.add(wakeWordsMaps.get(wakeWord));
                }
            }
        }
        return confidences;
    }


}
