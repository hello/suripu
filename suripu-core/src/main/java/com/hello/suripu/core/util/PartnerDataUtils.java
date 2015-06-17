package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import com.hello.suripu.algorithm.partner.PartnerBayesNetWithHmmInterpreter;
import com.hello.suripu.algorithm.signals.TwoPillsClassifier;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.sound.midi.Track;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by benjo on 2/22/15.
 */
public class PartnerDataUtils {

    private static final int NUM_SIGNALS = 3;
    final static protected int NUMBER_OF_MILLIS_IN_A_MINUTE = 60000;
    final static protected double probThresholdToRejectData  = 0.5;

    private static double [][] clone2D(double [][] x) {
        double [][] x2 = new double [x.length][] ;

        for (int i = 0; i < x.length; i++) {
            x2[i] = x[i].clone();
        }

        return x2;
    }


    static private interface SignalExtractor {
        double get(final TrackerMotion x);
    }

    static class ValueExtractor implements SignalExtractor {
        @Override
        public double get(TrackerMotion x) {
            return (double)x.value;
        }
    }

    static class KickoffCountExtractor implements SignalExtractor {
        @Override
        public double get(TrackerMotion x) {
            return (double)x.kickOffCounts;
        }
    }

    static class DurationExtractor implements SignalExtractor {
        @Override
        public double get(TrackerMotion x) {
            return (double)x.onDurationInSeconds;
        }
    }



    static private class MotionDataSignalWithT0 {
        double [][] x; // 6 x N
        double [][] xAppendedInTime; // 3 x 2N
        long t0;
    }

    static public class PartnerMotions {
        final public ImmutableList<TrackerMotion> myMotions;
        final public ImmutableList<TrackerMotion> yourMotions;

        public PartnerMotions(final List<TrackerMotion> myMotions, final List<TrackerMotion> yourMotions) {
            this.myMotions = ImmutableList.copyOf(myMotions);
            this.yourMotions = ImmutableList.copyOf(yourMotions);
        }

    }
    static public PartnerMotions getMyMotion(final List<TrackerMotion> motData1,final List<TrackerMotion> motData2) {
        final MotionDataSignalWithT0 motData = getMotionFeatureVectorsByTheMinute(motData1,motData2,true);

        final int[] classes = TwoPillsClassifier.classifyPillOwnershipByMovingSimilarity(motData.xAppendedInTime);


        final List<TrackerMotion> myMotion = new ArrayList<TrackerMotion>();
        final List<TrackerMotion> yourMotion = new ArrayList<TrackerMotion>();

        final Iterator<TrackerMotion> it = motData1.iterator();

        while (it.hasNext()) {
            final TrackerMotion m = it.next();

            final int idx = (int) (m.timestamp - motData.t0) / 1000 / 60;

            if (idx >= classes.length) {
                break; //this should never happen
            }

            //class 0 is uncertain, class 1 is mine -- I just say these are mine
            if (classes[idx] >= 0) {
                myMotion.add(m);
            }
            else {
                yourMotion.add(m);
            }

        }

        return new PartnerMotions(myMotion,yourMotion);
    }


    //turns two list of motion data into an MxN array of data
    //array is by the minute, where N = number of minutes between the two data sets
    //M is number of signals--right now that's 3 per
    static private MotionDataSignalWithT0 getMotionFeatureVectorsByTheMinute(final List<TrackerMotion> motData1,final List<TrackerMotion> motData2,boolean smearByOne) {

        final SignalExtractor [] extractors = {new ValueExtractor(),new KickoffCountExtractor(),new DurationExtractor()};

        if (motData1.size() == 0) {
            MotionDataSignalWithT0 ret = new MotionDataSignalWithT0();
            ret.x = new double[3][0];
            ret.t0 = 0;
            return ret;
        }

        final TrackerMotion first1 = motData1.get(0);
        final TrackerMotion last1 = motData1.get(motData1.size()-1);

        final TrackerMotion first2 = motData2.get(0);
        final TrackerMotion last2 = motData2.get(motData2.size()-1);

        long t0 = first1.timestamp;
        long tf = last1.timestamp;

        //find earliest and latest time stamps between the two lists
        if (first2.timestamp < t0) {
            t0 = first2.timestamp;
        }

        if (last2.timestamp > tf) {
            tf = last2.timestamp;
        }

        final int N =(int)( (tf - t0) / 1000 / 60 + 1);

        // TODO check to see that N is a sane value for this allocation
        final double [][] returnSignals = new double[2*NUM_SIGNALS][N];

        //zero out
        for (int isig = 0; isig < 2*NUM_SIGNALS; isig++) {
            Arrays.fill(returnSignals[isig], 0);
        }

        Iterator<TrackerMotion> iMot1 = motData1.iterator();

        //go through list an extract
        while (iMot1.hasNext()) {
            final TrackerMotion m1 = iMot1.next();

            //get minute index
            final int idx = (int) ((m1.timestamp - t0) / 60 / 1000);

            for (int iExtractor = 0; iExtractor < NUM_SIGNALS; iExtractor++) {
                returnSignals[iExtractor][idx] = extractors[iExtractor].get(m1);
            }
        }


        Iterator<TrackerMotion> iMot2 = motData2.iterator();

        //go through list an extract
        while (iMot2.hasNext()) {
            final TrackerMotion m2 = iMot2.next();

            //get minute index
            final int idx = (int) ((m2.timestamp - t0) / 60 / 1000);

            for (int iExtractor = 0; iExtractor < NUM_SIGNALS; iExtractor++) {
                returnSignals[iExtractor + NUM_SIGNALS][idx] = extractors[iExtractor].get(m2);
            }
        }

        MotionDataSignalWithT0 returnValues = new MotionDataSignalWithT0();

        //try and compensate for the fact that pill data might be off by one minute
        if (smearByOne) {
            final double [][] smeared = clone2D(returnSignals);

            for (int i = 1; i < N-1; i++) {
                for (int j = 0;j < 2*NUM_SIGNALS; j++) {
                    final double [] row = returnSignals[j];
                    smeared[j][i] = row[i-1] + row[i] + row[i+1];
                }
            }

            returnValues.x = smeared;
            returnValues.t0 = t0;
        }
        else {
            returnValues.x = returnSignals;
            returnValues.t0 = t0;
        }

        final double [][] xAppendedInTime = new double[NUM_SIGNALS][2*N];

        for (int j = 0; j < NUM_SIGNALS; j++) {
            for (int i = 0; i < N; i++) {
                xAppendedInTime[j][i] = returnValues.x[j][i];
                xAppendedInTime[j][i + N] = returnValues.x[j + NUM_SIGNALS][i];

            }
        }


        returnValues.xAppendedInTime = xAppendedInTime;





        return returnValues;

    }

    private static int getIndex(final Long timestamp, final Long t0,final Long period) {
        return (int) ((timestamp - t0) / period);

    }

    private static void fillBinsWithTrackerDurations(final Double [] bins, final Long t0,final Long period,final List<TrackerMotion> data, int sign) {

        Iterator<TrackerMotion> it = data.iterator();

        while(it.hasNext()) {
            final TrackerMotion m1 = it.next();

            final int idx = getIndex(m1.timestamp,t0,period);

            if (idx >= 0 && idx < bins.length) {
                bins[idx] += sign * m1.onDurationInSeconds;
            }

        }

    }
    static ImmutableList<TrackerMotion> partnerFilterWithDurationsDiffHmm(ImmutableList<TrackerMotion> myMotions, ImmutableList<TrackerMotion> yourMotions) {

        if (yourMotions.isEmpty() || myMotions.isEmpty()) {
            return myMotions;
        }

        //de-dup tracker motion
        final List<TrackerMotion> myMotionsDeDuped = TrackerMotionUtils.removeDuplicatesAndInvalidValues(myMotions.asList());
        final List<TrackerMotion> yourMotionsDeDuped = TrackerMotionUtils.removeDuplicatesAndInvalidValues(yourMotions.asList());



        //construct 5 minute bins of duration difference
        Long t0 = myMotionsDeDuped.get(0).timestamp;
        Long t02 = yourMotionsDeDuped.get(0).timestamp;

        if (t0 > t02) {
            t0 = t02;
        }

        Long tf = myMotionsDeDuped.get(myMotionsDeDuped.size() - 1).timestamp;
        Long tf2 = yourMotionsDeDuped.get(yourMotionsDeDuped.size() - 1).timestamp;

        if (tf < tf2) {
            tf = tf2;
        }

        final long period = NUMBER_OF_MILLIS_IN_A_MINUTE * 5;
        final int durationInIntervals = (int) ((tf - t0) / period);


        final Double data [] = new Double[durationInIntervals];

        Arrays.fill(data,0);

        fillBinsWithTrackerDurations(data,t0,period,myMotionsDeDuped,1);
        fillBinsWithTrackerDurations(data,t0,period,yourMotionsDeDuped,-1);

        final PartnerBayesNetWithHmmInterpreter partnerHmmFilter = new PartnerBayesNetWithHmmInterpreter();
        final List<Double> probs = partnerHmmFilter.interpretDurationDiff(ImmutableList.copyOf(data));

        //iterate through my motion and reject

        Iterator<TrackerMotion> it = myMotionsDeDuped.iterator();


        List<TrackerMotion> myFilteredMotion = Lists.newArrayList();

        while (it.hasNext()) {
            final TrackerMotion m = it.next();

            final double probItsMine = probs.get(getIndex(m.timestamp,t0,period));

            if (probItsMine < probThresholdToRejectData) {
                continue;
            }

            myFilteredMotion.add(m);

        }

        return ImmutableList.copyOf(myFilteredMotion);

    }
}
