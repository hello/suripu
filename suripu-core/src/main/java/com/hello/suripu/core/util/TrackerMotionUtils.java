package com.hello.suripu.core.util;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 3/24/15.
 */
public class TrackerMotionUtils {

    public static List<AmplitudeData> trackerMotionToAmplitudeData(final List<TrackerMotion> trackerMotions){
        final List<AmplitudeData> converted = new ArrayList<>();
        for(final TrackerMotion trackerMotion:trackerMotions){
            converted.add(new AmplitudeData(trackerMotion.timestamp, trackerMotion.value, trackerMotion.offsetMillis));
        }

        return converted;
    }

    public static List<AmplitudeData> trackerMotionToKickOffCounts(final List<TrackerMotion> trackerMotions){
        final List<AmplitudeData> converted = new ArrayList<>();
        for(final TrackerMotion trackerMotion:trackerMotions){
            converted.add(new AmplitudeData(trackerMotion.timestamp, trackerMotion.kickOffCounts, trackerMotion.offsetMillis));
        }

        return converted;
    }
}
