package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.GammaPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.translations.English;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class SleepHmmWithInterpretation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepHmmWithInterpretation.class);

    final static public int NUM_MINUTES_IN_WINDOW = 15;

    final static protected int NUM_DATA_DIMENSIONS = 3;
    final static protected int LIGHT_INDEX = 0;
    final static protected int MOT_COUNT_INDEX = 1;
    final static protected int DISTURBANCE_INDEX = 2;

    final static protected int ACCEPTABLE_GAP_IN_MINUTES_FOR_SLEEP_DISTURBANCE = 45;
    final static protected int ACCEPTABLE_GAP_IN_INDEX_COUNTS = ACCEPTABLE_GAP_IN_MINUTES_FOR_SLEEP_DISTURBANCE / NUM_MINUTES_IN_WINDOW;

    final static protected int SLEEP_DEPTH_NONE = 0;
    final static protected int SLEEP_DEPTH_LIGHT = 66;
    final static protected int SLEEP_DEPTH_REGULAR = 100;
    final static protected int SLEEP_DEPTH_DISTURBED = 33;


    final static protected int NUMBER_OF_MILLIS_IN_A_MINUTE = 60000;

    final static private double LIGHT_PREMULTIPLIER = 4.0;
    final static private int RAW_PILL_MAGNITUDE_DISTURBANCE_THRESHOLD = 15000;
    final static private double SOUND_DISTURBANCE_MAGNITUDE_DB = 55.0;

    protected final HiddenMarkovModel hmmWithStates;
    protected final Set<Integer> sleepStates;
    protected final Set<Integer> onBedStates;
    protected final Set<Integer> allowableEndingStates;

    protected final List<Integer> sleepDepthByStates;

    ////////////////////////////
    //Externally available results class

    public static class SleepHmmResult {

        public final Optional<Event> inBed;
        public final Optional<Event> fallAsleep;
        public final Optional<Event> wakeUp;
        public final Optional<Event> outOfBed;
        public final List<Event> disturbances;
        public final ImmutableList<Integer> path;

        public SleepHmmResult(Optional<Event> inBed,
                              Optional<Event> fallAsleep,
                              Optional<Event> wakeUp,
                              Optional<Event> outOfBed,
                              List<Event> disturbances,
                              ImmutableList<Integer> path) {

            this.inBed = inBed;
            this.fallAsleep = fallAsleep;
            this.wakeUp = wakeUp;
            this.outOfBed = outOfBed;
            this.disturbances = disturbances;
            this.path = path;
        }
    }

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
    protected SleepHmmWithInterpretation(final HiddenMarkovModel hmm, final Set<Integer> sleepStates, final Set<Integer> onBedStates,final Set<Integer> allowableEndingStates,final List<Integer> sleepDepthByStates) {
        this.hmmWithStates = hmm;
        this.sleepStates = sleepStates;
        this.onBedStates = onBedStates;
        this.allowableEndingStates = allowableEndingStates;
        this.sleepDepthByStates = sleepDepthByStates;
    }



    /*
CREATE CREATE CREATE
CREATE CREATE CREATE
CREATE CREATE CREATE

    Return Sleep HMM model from the SleepHMM protbuf

    */
    static public Optional<SleepHmmWithInterpretation> createModelFromProtobuf(final SleepHmmProtos.SleepHmm hmmModelData) {

        String source = "no_source";

        if (hmmModelData.hasSource()) {
            source = hmmModelData.getSource();
        }

        String id = "no_id";

        if (hmmModelData.hasUserId()) {
            id = hmmModelData.getUserId();
        }

        //get the data in the form of lists
        final List<SleepHmmProtos.StateModel> states = hmmModelData.getStatesList();

        // TODO assert that numStates == length of all the lists above
        final int numStates = hmmModelData.getNumStates();

        //1-D arrays, but that matrix actually corresponds to a numStates x numStates matrix, stored in row-major format
        final List<Double> stateTransitionMatrix = hmmModelData.getStateTransitionMatrixList();
        final List<Double> initialStateProbabilities = hmmModelData.getInitialStateProbabilitiesList();


        //go through list of enums and turn them into sets of ints
        // i.e. state 0 means not sleeping, state 1 means you're sleeping, state 2 means you're sleeping... etc.
        //so later we can say "path[i] is in sleep set?  No? Then you're not sleeping."
        final Set<Integer> sleepStates = new TreeSet<Integer>();
        final Set<Integer> onBedStates = new TreeSet<Integer>();
        final Set<Integer> allowableEndingStates = new TreeSet<Integer>();

        final List<Integer> sleepDepthsByState = new ArrayList<Integer>();


        //Populate the list of composite models
        //each model corresponds to a state---by order it appears in the list.
        //each model (for the moment) is a poisson, poisson, and discrete
        //for light, motion, and waves respectively
        final HmmPdfInterface [] obsModel = new HmmPdfInterface[numStates];

        for (int iState = 0; iState <  numStates; iState++) {

            final SleepHmmProtos.StateModel model = states.get(iState);

            if (! (model.hasLight() && model.hasDisturbances() && model.hasMotionCount())  ) {
                return Optional.absent();
            }

            //compose measurement model
            final PdfComposite pdf = new PdfComposite();

            pdf.addPdf(new GammaPdf(model.getLight().getMean(),model.getLight().getStddev(), 0));
            pdf.addPdf(new PoissonPdf(model.getMotionCount().getMean(), 1));
            pdf.addPdf(new DiscreteAlphabetPdf(model.getDisturbances().getProbabilitiesList(), 2));


            //assign states of onbed, sleeping
            if (model.hasBedMode() && model.getBedMode() == SleepHmmProtos.BedMode.ON_BED) {
                onBedStates.add(iState);
            }


            if (model.hasSleepMode() && model.getSleepMode() == SleepHmmProtos.SleepMode.SLEEP) {
                sleepStates.add(iState);
            }

            //assign allowable ending states
            //BIG ASSUMPTION HERE!!!!
            //allow to end on any state that is not sleeping
            //because if you're querying this code here... you are sure as fuck not sleeping
            // (LATER THIS MAY BE DIFFERENT)


            if (model.hasSleepMode() && model.getSleepMode() != SleepHmmProtos.SleepMode.SLEEP) {
                allowableEndingStates.add(iState);
            }

            //alternative -- not on bed at all instead of not sleeping
            //if (model.hasBedMode() && model.getBedMode() == SleepHmmProtos.BedMode.OFF_BED) {
            //    allowableEndingStates.add(iState);
            //}



            if (model.hasSleepDepth()) {
                switch (model.getSleepDepth()) {

                    case NOT_APPLICABLE:
                        sleepDepthsByState.add(SLEEP_DEPTH_NONE);
                        break;
                    case LIGHT:
                        sleepDepthsByState.add(SLEEP_DEPTH_LIGHT);
                        break;
                    case REGULAR:
                        sleepDepthsByState.add(SLEEP_DEPTH_REGULAR);
                        break;
                    case DISTURBED:
                        sleepDepthsByState.add(SLEEP_DEPTH_DISTURBED);
                        break;
                    default:
                        sleepDepthsByState.add(SLEEP_DEPTH_NONE);
                        break;
                }
            }
            else {
                sleepDepthsByState.add(SLEEP_DEPTH_NONE);
            }

            obsModel[iState] = pdf;
        }


        //return the HMM
        final HiddenMarkovModel hmm = new HiddenMarkovModel(numStates, stateTransitionMatrix, initialStateProbabilities, obsModel);

        LOGGER.debug("deserialized sleep HMM source={}, id={}, numStates={}",source,id,numStates);

        return Optional.of( new  SleepHmmWithInterpretation(hmm, sleepStates, onBedStates,allowableEndingStates,sleepDepthsByState));
    }

/* MAIN METHOD TO BE USED FOR DATA PROCESSING IS HERE */
    /* Use this method to get all the sleep / bed events from ALL the sensor data and ALL the pill data */
    public Optional<SleepHmmResult> getSleepEventsUsingHMM(final AllSensorSampleList sensors,
                                                           final List<TrackerMotion> pillData,
                                                           final long sleepPeriodStartTime,
                                                           final long sleepPeriodEndTime,
                                                           final long currentTimeInMillis) {




        //get sensor data as fixed time-step array of values
        //sensor data will get put into NUM_MINUTES_IN_WINDOW duration bins, somehow (either by adding, averaging, maxing, or whatever)
        final Optional<BinnedData> binnedDataOptional = getBinnedSensorData(sensors, pillData, NUM_MINUTES_IN_WINDOW,sleepPeriodStartTime,sleepPeriodEndTime,currentTimeInMillis);

        if (!binnedDataOptional.isPresent()) {
            return Optional.absent();
        }

        final BinnedData binnedData = binnedDataOptional.get();

        //only allow ending up in an off-bed state or wake state

        final Integer [] allowableEndings = allowableEndingStates.toArray(new Integer[allowableEndingStates.size()]);

        final int[] path = hmmWithStates.getViterbiPath(binnedData.data,allowableEndings);

        LOGGER.debug("decoded path = {} ",getPathAsString(path));

        //TODO use gaps to find disturbances / when people woke up in the night
        //TODO add in sleep depth via HMM states
        final Optional<SegmentPairWithGaps> sleep = mindTheGapsAndReturnTheLongestSegment(getSetBoundaries(path, sleepStates), ACCEPTABLE_GAP_IN_INDEX_COUNTS);
        final Optional<SegmentPairWithGaps> bed = mindTheGapsAndReturnTheLongestSegment(getSetBoundaries(path, onBedStates), ACCEPTABLE_GAP_IN_INDEX_COUNTS);

        final long t0 = binnedData.t0;
        final int timezoneOffset = binnedData.timezoneOffset;

        if (!sleep.isPresent() || !bed.isPresent()) {
            return Optional.absent();
        }

        final Optional<Event> inBed = Optional.of(getEventFromIndex(Event.Type.IN_BED,bed.get().bounds.i1,t0,timezoneOffset, English.IN_BED_MESSAGE));
        final Optional<Event> fallAsleep = Optional.of(getEventFromIndex(Event.Type.SLEEP,sleep.get().bounds.i1,t0,timezoneOffset,English.FALL_ASLEEP_MESSAGE));
        final Optional<Event> wakeUp = Optional.of(getEventFromIndex(Event.Type.WAKE_UP,sleep.get().bounds.i2,t0,timezoneOffset,English.WAKE_UP_MESSAGE));
        final Optional<Event> outOfBed = Optional.of(getEventFromIndex(Event.Type.OUT_OF_BED,bed.get().bounds.i2,t0,timezoneOffset,English.OUT_OF_BED_MESSAGE));

        final List<Event> disturbances = new ArrayList<Event>();

        return Optional.of(new SleepHmmResult(inBed,fallAsleep,wakeUp,outOfBed,disturbances,getIntArrayAsImmutableList(path)));


    }

    protected  Event getEventFromIndex(Event.Type eventType, final int index, final long t0, final int timezoneOffset,final String description) {
        Long eventTime =  getTimeFromBin(index,NUM_MINUTES_IN_WINDOW,t0);

        //  final long startTimestamp, final long endTimestamp, final int offsetMillis,
        //  final Optional<String> messageOptional,
        //  final Optional<SleepSegment.SoundInfo> soundInfoOptional,
        //  final Optional<Integer> sleepDepth){

       return Event.createFromType(eventType,
                eventTime,
                eventTime + NUMBER_OF_MILLIS_IN_A_MINUTE,
                timezoneOffset,
                Optional.of(description),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent());


    }


    //tells me when my events were, smoothing over gaps
    //if there are multiple candidates for segments still, pick the largest
    protected Optional<SegmentPairWithGaps> mindTheGapsAndReturnTheLongestSegment(final List<SegmentPair> pairs, int acceptableGap) {

        if (pairs.isEmpty()) {
            return Optional.absent();
        }

        final List<SegmentPairWithGaps> candidates = new ArrayList<>();

        SegmentPair pair = pairs.get(0);

        SegmentPairWithGaps candidate = new SegmentPairWithGaps(pair,new ArrayList<SegmentPair>());

        int prevI1 = pair.i1;

        for (int i = 1; i < pairs.size(); i++) {
            pair = pairs.get(i);
            final int i1 = candidate.bounds.i2;
            final int i2 = pair.i1;
            final int gap = i2 - i1;

            //either we smooth it over (gap is less than threshold)
            //or we start a new candidate segment
            if (gap > acceptableGap) {
                //start a new segment here, but first update and save off the old one
                candidates.add(candidate);

                //new segment
                candidate = new SegmentPairWithGaps(pair,new ArrayList<SegmentPair>());
                prevI1 = pair.i1;

            }
            else {
                candidate.gaps.add(new SegmentPair(i1,i2));
                candidate = new SegmentPairWithGaps(new SegmentPair(prevI1,pair.i2),candidate.gaps);

            }
        }

        candidates.add(candidate);

        //find max duration candidate
        int maxDuration = -1;

        for (SegmentPairWithGaps c : candidates) {
            final int duration = c.bounds.i2 - c.bounds.i1;

            if (duration > maxDuration) {
                candidate = c;
                maxDuration = duration;
            }

        }

        return  Optional.of(candidate);

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
                t1 = i;
            }

            if (!inSet.contains(current) && inSet.contains(prev)) {
                foundBeginning = false;

                pairList.add(new SegmentPair(t1, i));
            }
        }


        if (foundBeginning) {
            pairList.add(new SegmentPair(t1, path.length));
        }


        return pairList;
    }

    protected Optional<BinnedData> getBinnedSensorData(AllSensorSampleList sensors, List<TrackerMotion> pillData, final int numMinutesInWindow,
                                                       final long startTimeMillis, final long endTimeMillis, final long currentTimeInMillis) {
        final List<Sample> light = sensors.get(Sensor.LIGHT);
        final List<Sample> wave = sensors.get(Sensor.WAVE_COUNT);
        final List<Sample> sound = sensors.get(Sensor.SOUND_PEAK_DISTURBANCE);

        if (light == Collections.EMPTY_LIST || light.isEmpty()) {
            return Optional.absent();
        }

        //get start and end of window
        final long t0 = startTimeMillis;
        final int timezoneOffset = light.get(0).offsetMillis;
        long tf = endTimeMillis;

        //somehow, the light data is returned for
        if (currentTimeInMillis < tf && currentTimeInMillis >= t0) {
            tf = currentTimeInMillis;
        }

        final int dataLength = (int) (tf - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesInWindow;

        final double[][] data = new double[NUM_DATA_DIMENSIONS][dataLength];

        //zero out data
        for (int i = 0; i < NUM_DATA_DIMENSIONS; i++) {
            Arrays.fill(data[i], 0.0);
        }

        //start filling in the sensor data.  Pick the max of the 5 minute bins for light
        //LOG OF LIGHT, CONTINUOUS
        final Iterator<Sample> it1 = light.iterator();
        while (it1.hasNext()) {
            final Sample sample = it1.next();
            double value = sample.value;
            if (value < 0) {
                value = 0.0;
            }

            final double value2 = Math.log(value * LIGHT_PREMULTIPLIER + 1.0) / Math.log(2);

            maxInBin(data, sample.dateTime, value2, LIGHT_INDEX, t0, numMinutesInWindow);

        }


        //PILL MOTION
        final Iterator<TrackerMotion> it2 = pillData.iterator();
        while (it2.hasNext()) {
            final TrackerMotion m = it2.next();

            double value = m.value;

            //heartbeat value
            if (value == -1) {
                continue;
            }

            //if there's a disturbance, register it in the disturbance index
            if (value > RAW_PILL_MAGNITUDE_DISTURBANCE_THRESHOLD) {
                maxInBin(data, m.timestamp, 1.0, DISTURBANCE_INDEX, t0, numMinutesInWindow);

            }

            addToBin(data, m.timestamp, 1.0, MOT_COUNT_INDEX, t0, numMinutesInWindow);

        }

        //WAVES
        final Iterator<Sample> it3 = wave.iterator();
        while (it3.hasNext()) {
            final Sample sample = it3.next();
            double value = sample.value;

            //either wave happened or it didn't.. value can be 1.0 or 0.0
            if (value > 0.0) {
                maxInBin(data, sample.dateTime, 1.0, DISTURBANCE_INDEX, t0, numMinutesInWindow);
            }
        }

        //SOUND
        final Iterator<Sample> it4 = sound.iterator();
        while (it4.hasNext()) {
            final Sample sample = it4.next();
            double value = sample.value;

            if (value > SOUND_DISTURBANCE_MAGNITUDE_DB) {
                maxInBin(data, sample.dateTime, 1.0, DISTURBANCE_INDEX, t0, numMinutesInWindow);
            }
        }

        final BinnedData res = new BinnedData();
        res.data = data;
        res.numMinutesInWindow = numMinutesInWindow;
        res.t0 = t0;
        res.timezoneOffset = timezoneOffset;

        final DateTime dateTimeBegin = new DateTime(t0).withZone(DateTimeZone.forOffsetMillis(timezoneOffset));
        final DateTime dateTimeEnd = new DateTime(t0 + numMinutesInWindow * NUMBER_OF_MILLIS_IN_A_MINUTE * dataLength).withZone(DateTimeZone.forOffsetMillis(timezoneOffset));

        LOGGER.debug("t0={},tf={}",dateTimeBegin.toLocalTime().toString(),dateTimeEnd.toLocalTime().toString());
        LOGGER.debug("light={}",getDoubleVectorAsString(data[LIGHT_INDEX]));
        LOGGER.debug("motion={}",getDoubleVectorAsString(data[MOT_COUNT_INDEX]));
        LOGGER.debug("waves={}", getDoubleVectorAsString(data[DISTURBANCE_INDEX]));


        return Optional.of(res);
    }


    protected long getTimeFromBin(int bin, int binWidthMinutes, long t0) {
        long t = bin * binWidthMinutes;
        t *= NUMBER_OF_MILLIS_IN_A_MINUTE;
        t += t0;

        return t;
    }


    protected void maxInBin(double[][] data, long t, double value, final int idx, final long t0, final int numMinutesInWindow) {
        final int tIdx = (int) (t - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesInWindow;

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
        final int tIdx = (int) (t - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            data[idx][tIdx] += value;
        }
    }

    protected String getPathAsString(final int [] path) {
        String pathString = "";
        boolean first = true;
        for (int alpha : path) {

            if (!first) {
                pathString += ",";
            }
            pathString += String.format("%d",alpha);
            first = false;
        }

        return pathString;
    }


    protected String getDoubleVectorAsString(final double [] vec) {
        String vecString = "";
        boolean first = true;
        for (double f : vec) {

            if (!first) {
                vecString += ",";
            }
            vecString += String.format("%.1f",f);
            first = false;
        }

        return vecString;
    }

    protected ImmutableList<Integer> getIntArrayAsImmutableList(final int[] path) {
        List<Integer> newlist = new ArrayList<Integer>();

        for (int x : path) {
            newlist.add(x);
        }

        return ImmutableList.copyOf(newlist);


    }


}

