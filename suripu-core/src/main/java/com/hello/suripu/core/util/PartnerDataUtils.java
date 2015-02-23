package com.hello.suripu.core.util;

import com.hello.suripu.algorithm.signals.TwoPillsClassifier;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Created by benjo on 2/22/15.
 */
public class PartnerDataUtils {

    private static final int NUM_SIGNALS = 3;

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
        double [][] x;
        long t0;
    }

    static public  List<TrackerMotion> getMyMotion(final List<TrackerMotion> motData1,final List<TrackerMotion> motData2) {
        MotionDataSignalWithT0 motData = getMotionFeatureVectorsByTheMinute(motData1,motData2,true);

        int[] classes = TwoPillsClassifier.classifyPillOwnership(motData.x, NUM_SIGNALS);


        List<TrackerMotion> myMotion = new ArrayList<TrackerMotion>();

        Iterator<TrackerMotion> it = motData1.iterator();

        while (it.hasNext()) {
            TrackerMotion m = it.next();

            int idx = (int) (m.timestamp - motData.t0) / 1000 / 60;

            if (idx >= classes.length) {
                break; //this should never happen
            }

            if (classes[idx] > 0) {
                myMotion.add(m);
            }
            else {
                DateTime dt = new DateTime(m.timestamp);
                DateTime dt2 = dt.withZone(DateTimeZone.forOffsetMillis(m.offsetMillis));
                String time = dt2.toLocalDateTime().toString();


                int foo = 3;
                foo++;
            }

        }

        return myMotion;
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

        TrackerMotion first1 = motData1.get(0);
        TrackerMotion last1 = motData1.get(motData1.size()-1);

        TrackerMotion first2 = motData2.get(0);
        TrackerMotion last2 = motData2.get(motData2.size()-1);

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
        double [][] returnSignals = new double[2*NUM_SIGNALS][N];

        //zero out
        for (int isig = 0; isig < 2*NUM_SIGNALS; isig++) {
            Arrays.fill(returnSignals[isig], 0);
        }

        Iterator<TrackerMotion> iMot1 = motData1.iterator();

        //go through list an extract
        while (iMot1.hasNext()) {
            final TrackerMotion m1 = iMot1.next();

            //get minute index
            int idx = (int) ((m1.timestamp - t0) / 60 / 1000);

            for (int iExtractor = 0; iExtractor < NUM_SIGNALS; iExtractor++) {
                returnSignals[iExtractor][idx] = extractors[iExtractor].get(m1);
            }
        }


        Iterator<TrackerMotion> iMot2 = motData2.iterator();

        //go through list an extract
        while (iMot2.hasNext()) {
            final TrackerMotion m2 = iMot2.next();

            //get minute index
            int idx = (int) ((m2.timestamp - t0) / 60 / 1000);

            for (int iExtractor = 0; iExtractor < NUM_SIGNALS; iExtractor++) {
                returnSignals[iExtractor + NUM_SIGNALS][idx] = extractors[iExtractor].get(m2);
            }
        }

        MotionDataSignalWithT0 returnValues = new MotionDataSignalWithT0();

        //try and compensate for the fact that pill data might be off by one minute
        if (smearByOne) {
            double [][] smeared = clone2D(returnSignals);

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



        return returnValues;

    }
}
