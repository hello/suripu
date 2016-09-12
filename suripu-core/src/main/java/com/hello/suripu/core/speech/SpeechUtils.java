package com.hello.suripu.core.speech;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.speech.models.WakeWord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by ksg on 8/10/16
 */
public class SpeechUtils {
    public static List<String> wakewordsMapToDDBAttribute(final Map<String, Float> wakeWordsMaps) {
        // get wake word confidence vector
        final List<String> confidences = Lists.newArrayList();

        for (final WakeWord word : WakeWord.values()) {
            if (!word.equals(WakeWord.NULL)) {
                final String wakeWord = word.getWakeWordText();
                if (wakeWordsMaps.containsKey(wakeWord)) {
                    final String value = String.format("%s:%s", wakeWord.toLowerCase(), wakeWordsMaps.get(wakeWord).toString());
                    confidences.add(value);
                }
            }
        }
        return confidences;
    }

    public static Map<String, Float> wakeWordsConfidenceFromDDBItem(final Set<String> wakeWordConfidenceSet) {
        final Map<String, Float> confidences = Maps.newHashMap();
        for (final String value : wakeWordConfidenceSet) {
            final String[] parts = value.split(":");

            if (parts.length != 2) {
                // legacy stuff, previous separator was '_'
                return Collections.emptyMap();
            }

            final String wakeWord = WakeWord.fromWakeWordText(parts[0]).getWakeWordText();
            confidences.put(wakeWord, Float.valueOf(parts[1]));
        }
        return confidences;
    }


}
