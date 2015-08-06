package com.hello.suripu.core.util;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by pangwu on 3/24/15.
 */
public class TrackerMotionUtils {
    final static protected int NUMBER_OF_MILLIS_IN_A_MINUTE = 60000;
    final static protected int MIN_SPACING = 55000;

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

    static public List<TrackerMotion> removeDuplicatesAndInvalidValues(final List<TrackerMotion> trackerMotions) {
        Set<TrackerMotion> trackerMotionSet = new TreeSet<TrackerMotion>(new Comparator<TrackerMotion>() {
            @Override
            public int compare(final TrackerMotion m1, final TrackerMotion m2) {
                final long t1 = m1.timestamp / NUMBER_OF_MILLIS_IN_A_MINUTE;
                final long t2 = m2.timestamp / NUMBER_OF_MILLIS_IN_A_MINUTE;
                final int f1 = m1.value;
                final int f2 = m1.value;

                if (t1 < t2) {
                    return -1;
                }

                if (t1 > t2) {
                    return 1;
                }

                return 0;

            }
        });


        for (final TrackerMotion m : trackerMotions) {
            if (m.value != -1) {
                trackerMotionSet.add(m);
            }
        }


        List<TrackerMotion> uniqueValues = new ArrayList<>();

        uniqueValues.addAll(trackerMotionSet);

        return uniqueValues;
    }

    static public List<TrackerMotion> removeDuplicatesAndInvalidValuesBySpacing(final List<TrackerMotion> trackerMotions) {

        final List<TrackerMotion> spacedValues = new ArrayList<>();

        TrackerMotion last = null;
        for (final TrackerMotion m : trackerMotions ) {

            if (last == null) {
                spacedValues.add(m);
                last = m;
                continue;
            }

            final long deltaTime = m.timestampNoTruncation - last.timestampNoTruncation;

            if (deltaTime < MIN_SPACING) {
                continue;
            }

            spacedValues.add(m);
            last = m;
        }


        return spacedValues;
    }
}
