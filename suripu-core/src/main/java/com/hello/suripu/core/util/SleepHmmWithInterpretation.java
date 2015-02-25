package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benjo on 2/25/15.
 */
public class HiddenMarkovModelWithStateInterpretation {

    final static protected int NUM_DATA_DIMENSIONS = 3;
    final static protected int LIGHT_INDEX = 0;
    final static protected int MOT_COUNT_INDEX = 1;
    final static protected int WAVE_INDEX = 2;

    final static protected int ACCEPTABLE_GAP_IN_MINUTES_FOR_SLEEP_DISTURBANCE = 90;
    final static protected int NUM_MINUTES_IN_WINDOW = 15;
    final static protected int ACCEPTABLE_GAP_IN_INDEX_COUNTS = ACCEPTABLE_GAP_IN_MINUTES_FOR_SLEEP_DISTURBANCE / NUM_MINUTES_IN_WINDOW;

    final static private double LIGHT_PREMULTIPLIER = 4.0;

    protected final HiddenMarkovModel hmmWithStates;
    protected final Set<Integer> sleepStates;
    protected final Set<Integer> onBedStates;

    //////////////////////////////////////////
    //result classes -- internal use
    protected class BinnedData {
        double[][] data;
        long t0;
        int numMinutesInWindow;
        int timezoneOffset;
    }

    protected class SegmentPair {
        public SegmentPair(final Integer i1, final Integer i2) {
            this.i1 = i1;
            this.i2 = i2;
        }

        public final Integer i1;
        public final Integer i2;
    }

    protected class SegmentPairWithGaps {
        public SegmentPairWithGaps(SegmentPair bounds, List<SegmentPair> gaps) {
            this.bounds = bounds;
            this.gaps = gaps;
        }

        public final SegmentPair bounds;
        public final List<SegmentPair> gaps;
    }

    ///////////////////////////////

    //protected ctor -- only create from static create methods
    protected HiddenMarkovModelWithStateInterpretation(final HiddenMarkovModel hmm, final Set<Integer> sleepStates, final Set<Integer> onBedStates) {
        this.hmmWithStates = hmm;
        this.sleepStates = sleepStates;
        this.onBedStates = onBedStates;
    }



    /*
CREATE CREATE CREATE
CREATE CREATE CREATE
CREATE CREATE CREATE

    Return Sleep HMM model from the SleepHMM protbuf

    */
    static public HiddenMarkovModelWithStateInterpretation createModelFromProtobuf(final SleepHmmProtos.SleepHmm hmmModelData) {

        //get the data in the form of lists
        List<SleepHmmProtos.StateModel> states = hmmModelData.getStatesList();
        List<SleepHmmProtos.BedMode> bedModes = hmmModelData.getBedModeOfStatesList();
        List<SleepHmmProtos.SleepMode> sleepModes = hmmModelData.getSleepModeOfStatesList();


        // TODO assert that numStates == length of all the lists above
        final int numStates = hmmModelData.getNumStates();

        //1-D arrays, but that matrix actually corresponds to a numStates x numStates matrix, stored in row-major format
        List<Double> stateTransitionMatrix = hmmModelData.getStateTransitionMatrixList();
        List<Double> initialStateProbabilities = hmmModelData.getInitialStateProbabilitiesList();

        //Populate the list of composite models
        //each model corresponds to a state---by order it appears in the list.
        //each model (for the moment) is a poisson, poisson, and discrete
        //for light, motion, and waves respectively
        ArrayList<HmmPdfInterface> obsModel = new ArrayList<HmmPdfInterface>();
        for (SleepHmmProtos.StateModel model : states) {
            PdfComposite pdf = new PdfComposite();

            pdf.addPdf(new PoissonPdf(model.getLight().getMean(), 0));
            pdf.addPdf(new PoissonPdf(model.getMotionCount().getMean(), 1));
            pdf.addPdf(new DiscreteAlphabetPdf(model.getWaves().getProbabilitiesList(), 2));

            obsModel.add(pdf);
        }

        //go through list of enums and turn them into sets of ints
        // i.e. state 0 means not sleeping, state 1 means you're sleeping, state 2 means you're sleeping... etc.
        //so later we can say "path[i] is in sleep set?  No? Then you're not sleeping."
        Set<Integer> sleepStates = new TreeSet<Integer>();
        Set<Integer> onBedStates = new TreeSet<Integer>();

        for (int i = 0; i < numStates; i++) {
            if (hmmModelData.getBedModeOfStates(i) == SleepHmmProtos.BedMode.ON_BED) {
                onBedStates.add(i);
            }

            if (hmmModelData.getSleepModeOfStates(i) == SleepHmmProtos.SleepMode.SLEEP) {
                sleepStates.add(i);
            }
        }

        //return the HMM
        final HiddenMarkovModel hmm = new HiddenMarkovModel(numStates, stateTransitionMatrix, initialStateProbabilities, (HmmPdfInterface[]) obsModel.toArray())
        HiddenMarkovModelWithStateInterpretation result = new HiddenMarkovModelWithStateInterpretation(hmm, sleepStates, onBedStates);

        return result;
    }

/* MAIN METHOD TO BE USED FOR DATA PROCESSING IS HERE */
    /* Use this method to get all the sleep / bed events from ALL the sensor data and ALL the pill data */
    public List<Optional<Event>> getSleepEventsUsingHMM(AllSensorSampleList sensors, List<TrackerMotion> pillData) {

        List<Optional<Event>> res = new ArrayList<Optional<Event>>();

        //get sensor data as fixed time-step array of values
        //sensor data will get put into NUM_MINUTES_IN_WINDOW duration bins, somehow (either by adding, averaging, maxing, or whatever)
        Optional<BinnedData> binnedData = getBinnedSensorData(sensors, pillData, NUM_MINUTES_IN_WINDOW);

        if (binnedData.isPresent()) {

            int[] path = hmmWithStates.getViterbiPath(binnedData.get().data);

            SegmentPairWithGaps sleep = mindTheGapsAndReturnTheLongestSegment(getSetBoundaries(path, sleepStates), ACCEPTABLE_GAP_IN_INDEX_COUNTS);

            if (sleep != null) {


            }

            SegmentPairWithGaps bed = mindTheGapsAndReturnTheLongestSegment(getSetBoundaries(path, onBedStates), ACCEPTABLE_GAP_IN_INDEX_COUNTS);

            if (bed != null) {


            }


        }

        return res;
    }


    //tells me when my events were, smoothing over gaps
    //if there are multiple candidates for segments still, pick the largest
    protected SegmentPairWithGaps mindTheGapsAndReturnTheLongestSegment(final List<SegmentPair> pairs, int acceptableGap) {

        if (pairs.isEmpty()) {
            return null;
        }

        SegmentPair pair = pairs.get(0);
        int segmentI1 = pair.i1;
        int segmentI2 = pair.i2;


        for (int i = 1; i < pairs.size(); i++) {
            pair = pairs.get(i);

            if (i != )
        }




    }


    //Returns the boundary indices (i.e. a segment) that is in the int array
    //if there is only a termination in the set, then we have the segment t0 - t2
    //if there is only a beginning in the set, then we have the segment t1 - tfinal
    protected List<SegmentPair> getSetBoundaries(final int[] path, Set<Integer> inSet) {
        boolean foundBeginning = false;


        int t1 = 0;
        int t2 = 0;

        List<SegmentPair> pairList = new ArrayList<SegmentPair>();

        for (int i = 1; i < path.length; i++) {
            int prev = path[i - 1];
            int current = path[i];


            if (inSet.contains(current) && !inSet.contains(prev)) {
                foundBeginning = true;
                t1 = prev;
            }

            if (!inSet.contains(current) && inSet.contains(prev)) {
                foundBeginning = false;

                pairList.add(new SegmentPair(t1, current));
            }
        }


        if (foundBeginning) {
            pairList.add(new SegmentPair(t1, path.length));
        }


        return pairList;
    }

    protected Optional<BinnedData> getBinnedSensorData(AllSensorSampleList sensors, List<TrackerMotion> pillData, final int numMinutesInWindow) {
        List<Sample> light = sensors.get(Sensor.LIGHT);
        List<Sample> wave = sensors.get(Sensor.WAVE_COUNT);

        if (light == Collections.EMPTY_LIST || light.isEmpty()) {
            return Optional.absent();
        }

        //get start and end of window
        long t0 = light.get(0).dateTime;
        int timezoneOffset = light.get(0).offsetMillis;
        long tf = light.get(light.size() - 1).dateTime;

        int dataLength = (int) (tf - t0) / 1000 / 60 / numMinutesInWindow;

        double[][] data = new double[NUM_DATA_DIMENSIONS][dataLength];

        //zero out data
        for (int i = 0; i < NUM_DATA_DIMENSIONS; i++) {
            Arrays.fill(data[i], 0.0);
        }

        //start filling in the sensor data.  Pick the max of the 5 minute bins for light
        //compute log of light
        Iterator<Sample> it1 = light.iterator();
        while (it1.hasNext()) {
            Sample sample = it1.next();
            double value = sample.value;
            if (value < 0) {
                value = 0.0;
            }

            //TODO transform this back to raw counts before taking log
            value = Math.log(value * LIGHT_PREMULTIPLIER + 1.0) / Math.log(2);

            maxInBin(data, sample.dateTime, value, LIGHT_INDEX, t0, numMinutesInWindow);

        }


        //max of "energy"
        //add counts to bin
        Iterator<TrackerMotion> it2 = pillData.iterator();
        while (it2.hasNext()) {
            TrackerMotion m = it2.next();

            double value = m.value;

            if (value < 0) {
                value = 0;
            }

            value = Math.log(value / 2000 + 1);

            addToBin(data, m.timestamp, 1.0, MOT_COUNT_INDEX, t0, numMinutesInWindow);
            //maxInBin(data, m.timestamp, value, ENERGY_INDEX, t0, numMinutesInWindow);

        }

        Iterator<Sample> it3 = wave.iterator();
        while (it3.hasNext()) {
            Sample sample = it3.next();
            double value = sample.value;

            //either wave happened or it didn't.. value can be 1.0 or 0.0
            if (value > 0.0) {
                value = 1.0;
            }
            else {
                value = 1.0;
            }

            maxInBin(data, sample.dateTime, value, WAVE_INDEX, t0, numMinutesInWindow);
        }

        BinnedData res = new BinnedData();
        res.data = data;
        res.numMinutesInWindow = numMinutesInWindow;
        res.t0 = t0;
        res.timezoneOffset = timezoneOffset;

        return Optional.of(res);
    }


    protected DateTime getTimeFromBin(int bin, int binWidthMinutes, long t0, int offset) {
        long t = bin * binWidthMinutes;
        t *= 60 * 1000;
        t += t0;

        DateTime dt = new DateTime(t);
        return dt.withZone(DateTimeZone.forOffsetMillis(offset));
    }


    protected void maxInBin(double[][] data, long t, double value, final int idx, final long t0, final int numMinutesInWindow) {
        final int tIdx = (int) (t - t0) / 1000 / 60 / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            double v1 = data[idx][tIdx];
            double v2 = value;

            if (v1 < v2) {
                v1 = v2;
            }

            data[idx][tIdx] = v1;
        }

    }

    protected void addToBin(double[][] data, long t, double value, final int idx, final long t0, final int numMinutesInWindow) {
        final int tIdx = (int) (t - t0) / 1000 / 60 / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            data[idx][tIdx] += value;
        }
    }



}

