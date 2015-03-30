package com.hello.suripu.core.util;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.core.models.Sample;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 3/30/15.
 */
public class SoundUtils {
    public static List<AmplitudeData> sampleToAmplitudeData(final List<Sample> samples){
        final List<AmplitudeData> result = new ArrayList<>();
        for(final Sample sample:samples){
            result.add(new AmplitudeData(sample.dateTime, sample.value, sample.offsetMillis));
        }
        return result;
    }
}
