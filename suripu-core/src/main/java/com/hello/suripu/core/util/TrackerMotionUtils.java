package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.interpretation.IdxPair;
import com.hello.suripu.algorithm.outlier.OnBedBounding;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(TrackerMotionUtils.class);

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

    /*
      @param  time timestamp
      @param interval (period)
      @the index of timestamp with relation to t0.
    */
    public static int getIndex(final Long timestamp, final Long t0,final Long period, final int max) {
        int idx = (int) ((timestamp - t0) / period);
        if (idx >= max || idx < 0) {
            throw new IllegalArgumentException("getIndex time out of bounds");
        }

        return idx;

    }

    /*
       @param on duration seconds,both input and output,  and add or subtract that to the appropriate time bin
       @param t0, the time of index 0
       @param period the duration of one bin
       @param list of tracker motion data
       @param  sign determines if you are adding (1) or subtracting (-1), really just a multiplier
       @param "smear" means smear the data out over adjacent minutes of data (so one second of "on duration" becomes 0.333 seconds in the previous bin, the current bin, and the next bin)
     */
    public static void fillBinsWithTrackerDurations(final Double [] bins, final Long t0, final Long period, final ImmutableList<TrackerMotion> data, int sign, boolean smear) {

        Iterator<TrackerMotion> it = data.iterator();

        while(it.hasNext()) {
            final TrackerMotion m1 = it.next();

            final int idx = getIndex(m1.timestamp,t0,period,bins.length);
            double normalizer = 1.0;

            if (smear) {
                normalizer = 3.0;
            }

            if (idx >= 0) {
                bins[idx] += (sign * m1.onDurationInSeconds) / normalizer;
            }

            if (smear) {

                final int idx1 = getIndex(m1.timestamp - 1 * DateTimeConstants.MILLIS_PER_MINUTE,t0,period,bins.length);
                final int idx2 = getIndex(m1.timestamp + 1 * DateTimeConstants.MILLIS_PER_MINUTE,t0,period,bins.length);

                if (idx1 >= 0) {
                    bins[idx1] += (sign * m1.onDurationInSeconds) / normalizer;

                }

                if (idx2 >= 0) {
                    bins[idx2] += (sign * m1.onDurationInSeconds) / normalizer;
                }
            }
        }
    }

    //basic idea is to drop motions that occur near the pairing times of the pills
    public static ImmutableList<TrackerMotion> filterPillPairingMotionsWithTimes(final ImmutableList<TrackerMotion> motions, final List<DateTime> pairTimes) {

        final long timeAfterCreationToFilter = 15 * DateTimeConstants.MILLIS_PER_MINUTE;
        final long timeBeforeCreationToFilter = 5 * DateTimeConstants.MILLIS_PER_MINUTE;
        final List<TrackerMotion> filteredMotions = Lists.newArrayList();

        MOTION_LOOP:
        for (final TrackerMotion m : motions) {

            for (final DateTime t : pairTimes) {
                final long tp = t.withZone(DateTimeZone.UTC).getMillis();

                if (m.timestamp >= tp - timeBeforeCreationToFilter && m.timestamp <= tp + timeAfterCreationToFilter) {
                    continue MOTION_LOOP;
                }
            }


            filteredMotions.add(m);

        }

        LOGGER.info("action=filtering_tracker_motion num_points_dropped={}",motions.size() - filteredMotions.size());

        return ImmutableList.copyOf(filteredMotions);
    }

    public static ImmutableList<TrackerMotion> filterOffBedMotions(final ImmutableList<TrackerMotion> motions) {
        if (motions.isEmpty()) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        final long t0 = motions.get(0).timestamp;
        final long tf = motions.get(motions.size()-1).timestamp;
        final long period = NUMBER_OF_MILLIS_IN_A_MINUTE;
        final int maxIdx = (int)((tf - t0) / period) + 1;

        final Double [] binsTemp = new Double[maxIdx];
        Arrays.fill(binsTemp,0.0);
        fillBinsWithTrackerDurations(binsTemp,t0,period,motions,1,false);

        final double [] durations = new double[maxIdx];

        for (int i = 0; i < binsTemp.length; i++) {
            durations[i] = binsTemp[i].doubleValue();
        }

        final Optional<IdxPair> bedIndicesOptional = OnBedBounding.getIndicesOnBedBounds(durations);

        if (!bedIndicesOptional.isPresent()) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        final IdxPair bedIndices = bedIndicesOptional.get();

        LOGGER.info("action=found_on_bed_indices idx1={} idx2={}",bedIndices.i1,bedIndices.i2);

        final List<TrackerMotion> filteredMotions = Lists.newArrayList();

        for (final TrackerMotion m : motions) {
            final int idx = getIndex(m.timestamp,t0,period,maxIdx);

            //only filter beginning
            if (idx < bedIndices.i1) {
                continue;
            }

            filteredMotions.add(m);
        }


        LOGGER.info("action=filtering_tracker_motion num_points_dropped={}",motions.size() - filteredMotions.size());
        return ImmutableList.copyOf(filteredMotions);


    }
}
