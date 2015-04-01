package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.api.datascience.SleepHmmProtos;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.SleepStats;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.translations.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by benjo on 2/25/15.
 */
public class SleepHmmWithInterpretation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepHmmWithInterpretation.class);




    //final static protected int MIN_DURATION_OF_SLEEP_SEGMENT_IN_MINUTES = 45;
    final static protected int MAX_ALLOWABLE_SLEEP_GAP_IN_MINUTES = 15;

    final static protected int MIN_DURATION_OF_ONBED_SEGMENT_IN_MINUTES = 45;
    final static protected int MAX_ALLOWABLE_ONBED_GAP_IN_MINUTES = 45;

    final static protected int NUMBER_OF_MILLIS_IN_A_MINUTE = 60000;

    static final double MIN_ENERGY = 1000.0;
    static final int SMOOTHED_PERIOD_MINUTES = 10;

    final ImmutableList<NamedSleepHmmModel> models;

    ////////////////////////////
    //Externally available results class


    public static class SleepHmmResult {

        public final SleepStats stats;
        public final ImmutableList<Event> sleepEvents;
        public final ImmutableList<Integer> path;



        public SleepHmmResult(SleepStats stats,
                              ImmutableList<Integer> path,
                              ImmutableList<Event> sleepEvents) {

            this.stats = stats;
            this.path = path;
            this.sleepEvents = sleepEvents;
        }
    }

    //////////////////////////////////////////
    //result classes -- internal use


    public class SegmentPair {
        public SegmentPair(final Integer i1, final Integer i2) {
            this.i1 = i1;
            this.i2 = i2;
        }

        public final Integer i1;
        public final Integer i2;

        public boolean isInBounds(final Integer idx) {
            return  (idx <= i2 && idx >= i1);
        }
    }

    public class SegmentPairWithGaps {
        public SegmentPairWithGaps(SegmentPair bounds, List<SegmentPair> gaps) {
            this.bounds = bounds;
            this.gaps = gaps;
        }

        public boolean isInsideOf(final SegmentPairWithGaps p) {
            if (bounds.i1 >= p.bounds.i1 && bounds.i2 <= p.bounds.i2) {
                return true;
            }
            else {
                return false;
            }
        }
        public final SegmentPair bounds;
        public final List<SegmentPair> gaps;
    }

    ///////////////////////////////

    //protected ctor -- only create from static create methods
    protected SleepHmmWithInterpretation(final ImmutableList<NamedSleepHmmModel> models ) {
        this.models = models;

    }

    protected class TimeIndexInfo {
        final int numMinutesInMeasPeriod;
        final long t0;
        final int timezoneOffset;
        final int durationInIndices;

        public TimeIndexInfo(int numMinutesInMeasPeriod, long t0, int timezoneOffset, int durationInIndices) {
            this.numMinutesInMeasPeriod = numMinutesInMeasPeriod;
            this.t0 = t0;
            this.timezoneOffset = timezoneOffset;
            this.durationInIndices = durationInIndices;
        }
    }

    protected class SleepDepthSummary {
        final public int numberMinutesInLightSleep;
        final public int numberMinutesInDeepSleep;

        public SleepDepthSummary(int numberMinutesInLightSleep, int numberMinutesInDeepSleep) {
            this.numberMinutesInLightSleep = numberMinutesInLightSleep;
            this.numberMinutesInDeepSleep = numberMinutesInDeepSleep;
        }
    }


    /*
CREATE CREATE CREATE
CREATE CREATE CREATE
CREATE CREATE CREATE

    Return Sleep HMM model from the SleepHMM protbuf

    */
    static public Optional<SleepHmmWithInterpretation> createModelFromProtobuf(final SleepHmmProtos.SleepHmmModelSet serializedModels) {

        try {
            final ImmutableList<NamedSleepHmmModel> models = HmmDeserialization.createModelsFromProtobuf(serializedModels);

            return Optional.of(new SleepHmmWithInterpretation(models));

        }
        catch (Exception e) {
            return Optional.absent();
        }

    }

    public ImmutableList<SegmentPair> testDecodeWithData(final double [][] data) {
        for (final NamedSleepHmmModel model : models) {

            final Integer [] allowableEndings = model.allowableEndingStates.toArray(new Integer[model.allowableEndingStates.size()]);

            if (!model.modelName.equals("default")) {
                continue;
            }

            final HmmDecodedResult result = model.hmm.decode(data,allowableEndings);

            ImmutableList<SegmentPairWithGaps> sleep = filterSleepSegmentPairsByHeuristic(
                    mindTheGapsAndJoinPairs(getSetBoundaries(result.bestPath, model.sleepStates),MAX_ALLOWABLE_SLEEP_GAP_IN_MINUTES / model.numMinutesInMeasPeriod));

            ImmutableList<SegmentPair> sleepSplitOnGaps = pairsWithGapsToPairs(sleep,false);

            return sleepSplitOnGaps;

        }

        return ImmutableList.copyOf(Collections.EMPTY_LIST);
    }

/* MAIN METHOD TO BE USED FOR DATA PROCESSING IS HERE */
    /* Use this method to get all the sleep / bed events from ALL the sensor data and ALL the pill data */
    public Optional<SleepHmmResult> getSleepEventsUsingHMM(final AllSensorSampleList sensors,
                                                           final List<TrackerMotion> pillData,
                                                           final long sleepPeriodStartTimeLocal,
                                                           final long sleepPeriodEndTimeLocal,
                                                           final long currentTimeInMillisUTC) {



        if (pillData.isEmpty()) {
            return Optional.absent();
        }

        double lowestModelScore = Float.MAX_VALUE;
        HmmDecodedResult bestResult = null;
        NamedSleepHmmModel bestModel = null;
        final int timezoneOffset = pillData.get(0).offsetMillis;;


        final long startTimeMillisInUTC = sleepPeriodStartTimeLocal - timezoneOffset; //convert to UTC
        final long endOfThePeriodUTC = sleepPeriodEndTimeLocal - timezoneOffset; //convert to UTC
        final long endTimeMillisUTC = currentTimeInMillisUTC < endOfThePeriodUTC ? currentTimeInMillisUTC : endOfThePeriodUTC; //find earlier of end of period or current time
        final int numMinutes = (int) ((endTimeMillisUTC - startTimeMillisInUTC) / NUMBER_OF_MILLIS_IN_A_MINUTE);


        final List<TrackerMotion> cleanedUpPillData = SleepHmmSensorDataBinning.removeDuplicatesAndInvalidValues(pillData);


        LOGGER.debug("removed {} duplicate or invalid pill data points",pillData.size() - cleanedUpPillData.size());

        /* go through each model, evaluate, find the best  */
        for (final NamedSleepHmmModel model : models) {
            LOGGER.debug("Trying out model \"{}\"",model.modelName);
            final Optional<SleepHmmSensorDataBinning.BinnedData> binnedDataOptional = SleepHmmSensorDataBinning.getBinnedSensorData(sensors, cleanedUpPillData, model, startTimeMillisInUTC, endTimeMillisUTC, timezoneOffset);


            if (!binnedDataOptional.isPresent()) {
                return Optional.absent();
            }

            final SleepHmmSensorDataBinning.BinnedData binnedData = binnedDataOptional.get();


            //only allow ending up in an off-bed state or wake state

            final Integer [] allowableEndings = model.allowableEndingStates.toArray(new Integer[model.allowableEndingStates.size()]);

            //decode via viterbi
            final HmmDecodedResult result = model.hmm.decode(binnedData.data, allowableEndings);

            LOGGER.debug("path={}", SleepHmmSensorDataBinning.getPathAsString(result.bestPath));
            LOGGER.debug("model \"{}\" BIC={}",model.modelName,result.bic);
            //keep track of lowest score (lowest == best)
            if (result.bic < lowestModelScore) {
                lowestModelScore = result.bic;
                bestResult = result;
                bestModel = model;
            }
        }

        if (bestModel == null || bestResult == null) {
            return Optional.absent();
        }


        LOGGER.debug("picked model \"{}\" ",bestModel.modelName);
        /*  First pass is mind the gaps
         *  so if there's a disturbance that is less than  ACCEPTABLE_GAP_IN_INDEX_COUNTS it's absorbed into the segment
         *  Then, we filter by segment length.
         *
         *  What could go wrong?
         *
         *  */

        int numMinutesInMeasPeriod = bestModel.numMinutesInMeasPeriod;

        //extract set boundaries as pairs of indices (i1,i2)
        //merge pairs that are close together, keeping track of the gaps
        //then filter out the remaining pairs that are too small in duration (i2 - i1)
        ImmutableList<SegmentPairWithGaps> sleep = filterSleepSegmentPairsByHeuristic(
                mindTheGapsAndJoinPairs(getSetBoundaries(bestResult.bestPath, bestModel.sleepStates),MAX_ALLOWABLE_SLEEP_GAP_IN_MINUTES / bestModel.numMinutesInMeasPeriod));

        ImmutableList<SegmentPairWithGaps> onBed = filterSegmentPairsByDuration(
                mindTheGapsAndJoinPairs(getSetBoundaries(bestResult.bestPath, bestModel.onBedStates),MAX_ALLOWABLE_ONBED_GAP_IN_MINUTES/ bestModel.numMinutesInMeasPeriod),
                MIN_DURATION_OF_ONBED_SEGMENT_IN_MINUTES / bestModel.numMinutesInMeasPeriod);

        //split into pure pairs (i.e. just a list of pairs)
        ImmutableList<SegmentPair> sleepSplitOnGaps = pairsWithGapsToPairs(sleep,false);
        ImmutableList<SegmentPair> onBedIgnoringGaps = pairsWithGapsToPairs(onBed,true);

        if (bestModel.isUsingIntervalSearch) {
            //get whatever is earlier


            double [] pillDataArray = getPillDataArray(cleanedUpPillData,startTimeMillisInUTC,numMinutes);

            final SmoothedBufferFeats smoothedBufferFeats = getSmoothedBuffer(pillDataArray, SMOOTHED_PERIOD_MINUTES, MIN_ENERGY,true);

            //LOGGER.debug("pillenergy={}",SleepHmmSensorDataBinning.getDoubleVectorAsString(pillFeats.filteredEnergy));
            //LOGGER.debug("diffenergy={}",SleepHmmSensorDataBinning.getDoubleVectorAsString(pillFeats.differentialEnergy));

            sleepSplitOnGaps = getIndiciesInMinutesWithIntervalSearchForSleep(sleepSplitOnGaps, smoothedBufferFeats, numMinutesInMeasPeriod);
            onBedIgnoringGaps = getIndiciesInMinutesWithIntervalSearchForInAndOutOfBed(onBedIgnoringGaps, smoothedBufferFeats,numMinutesInMeasPeriod);
            numMinutesInMeasPeriod = 1;
        }


        final TimeIndexInfo timeIndexInfo = new TimeIndexInfo(numMinutesInMeasPeriod,startTimeMillisInUTC,timezoneOffset, numMinutes/numMinutesInMeasPeriod);

        final SleepDepthSummary sleepDepthSummary = getSleepDepthSummary(timeIndexInfo,cleanedUpPillData,sleepSplitOnGaps);


        return  processEventsIntoResult(sleepSplitOnGaps,onBedIgnoringGaps,bestResult.bestPath,timeIndexInfo,sleepDepthSummary);


    }
    
    protected ImmutableList<SegmentPair> pairsWithGapsToPairs(final ImmutableList<SegmentPairWithGaps> segs,boolean ignoreGaps) {
        List<SegmentPair> pairs = new ArrayList<>();

        for (final SegmentPairWithGaps seg : segs) {
            int i1 = seg.bounds.i1;


            if (!ignoreGaps) {
                for (final SegmentPair pair : seg.gaps) {
                    final int i2 = pair.i1;
                    pairs.add(new SegmentPair(i1,i2));

                    i1 = pair.i2;
                }
            }


            final int i2 = seg.bounds.i2;
            pairs.add(new SegmentPair(i1,i2));
        }


        return ImmutableList.copyOf(pairs);
    }


    static protected  Event getEventFromIndex(Event.Type eventType, final int index, final long t0, final int timezoneOffset,final String description,final int numMinutesInWindow) {
        Long eventTime =  SleepHmmSensorDataBinning.getTimeFromBin(index, numMinutesInWindow, t0);

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

    double [] getPillDataArray(final List<TrackerMotion> pillData,final long t0,final int numMinutes) {
        double [] ret = new double[numMinutes];

        Arrays.fill(ret,0.0);

        Iterator<TrackerMotion> it = pillData.iterator();

        while(it.hasNext()) {
            final TrackerMotion m = it.next();


            final int idx = (int) ((m.timestamp - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE);

            if (idx > 0 && idx < numMinutes && m.value != -1) {
                ret[idx] = m.value;
            }
        }


        return  ret;
    }



    protected Optional<Integer> getMaximumInInterval(final  double [] x,final int idx1, final int idx2) {
        double max = Double.MIN_VALUE;
        int imax = 0;
        for (int i = idx1; i < idx2; i++) {
            if (x[i] > max) {
                max = x[i];
                imax = i;
            }
        }

        if (max == Double.MIN_VALUE) {
            return Optional.absent();
        }
        else {
            return Optional.of(imax);
        }
    }

    protected Optional<Integer> getMinimumInInterval(final  double [] x,final int idx1, final int idx2) {
        double min = Double.MAX_VALUE;
        int imin = 0;
        for (int i = idx1; i < idx2; i++) {
            if (x[i] < min) {
                min = x[i];
                imin = i;
            }
        }

        if (min == Double.MAX_VALUE) {
            return Optional.absent();
        }
        else {
            return Optional.of(imin);
        }
    }


    protected  static class SmoothedBufferFeats {

        final double [] filteredEnergy;
        final double [] differentialEnergy;

        public SmoothedBufferFeats(double[] filteredEnergy, double[] differentialEnergy) {
            this.filteredEnergy = filteredEnergy;
            this.differentialEnergy = differentialEnergy;
        }
    }

    protected SmoothedBufferFeats getSmoothedBuffer(final double[] x, final int interval, final double minEnergy,boolean computeLog) {
        double[] filteredEnergy = new double[x.length];
        double[] differentialEnergy = new double[x.length];

        Arrays.fill(filteredEnergy, minEnergy);
        Arrays.fill(differentialEnergy, 0.0);

        //forwards
        for (int j = 0; j < x.length - interval; j++) {
            double accumulator = 0.0;
            for (int i = 0; i < interval; i++) {
                accumulator += x[j + i];
            }

            accumulator /= (double)interval;
            accumulator *= 0.5;

            filteredEnergy[j + interval - 1] += accumulator;
        }

        //backwards
        for (int j = x.length - interval - 1; j >= 0; j--) {
            double accumulator = 0.0;
            for (int i = 0; i < interval; i++) {
                accumulator += x[j + i];
            }

            accumulator /= (double)interval;
            accumulator *= 0.5;

            filteredEnergy[j] += accumulator;
        }

        //compute log
        if (computeLog) {
            for (int j = 0; j < x.length; j++) {
                if (filteredEnergy[j] < 0.0) {
                    filteredEnergy[j] = 0.0;
                }

                filteredEnergy[j] = Math.log(filteredEnergy[j] + 1.0);
            }
        }


        //compute differential of log energy
        for (int j = 0; j < x.length - interval; j++) {
            differentialEnergy[j + interval / 2 + 1] = filteredEnergy[j + interval] - filteredEnergy[j];
        }


        return new SmoothedBufferFeats(filteredEnergy, differentialEnergy);
    }

    protected ImmutableList<SegmentPair> getIndiciesInMinutesWithIntervalSearchForSleep(final ImmutableList<SegmentPair> segs, final SmoothedBufferFeats smoothedBufferFeats,
                                                                                        final int numMinutesInMeasPeriod) {

        List<SegmentPair> newSegments = new ArrayList<>();
        int lastIndex = -1;
        for (final SegmentPair seg : segs) {

            int i1Sleep = seg.i1*numMinutesInMeasPeriod - numMinutesInMeasPeriod;
            int i2Sleep = seg.i1*numMinutesInMeasPeriod + numMinutesInMeasPeriod;

            if (i1Sleep < 0) {
                i1Sleep = 0;
            }

            final Optional<Integer> sleepBound = getMaximumInInterval(smoothedBufferFeats.filteredEnergy, i1Sleep, i2Sleep);

            int i1Wake = seg.i2*numMinutesInMeasPeriod - 1*numMinutesInMeasPeriod;
            int i2Wake = seg.i2*numMinutesInMeasPeriod + 1*numMinutesInMeasPeriod;

            if (i2Wake > smoothedBufferFeats.filteredEnergy.length) {
                i2Wake = smoothedBufferFeats.filteredEnergy.length;
            }

            final Optional<Integer> wakeIndex = getMaximumInInterval(smoothedBufferFeats.filteredEnergy,i1Wake,i2Wake);

            int newI1 = seg.i1*numMinutesInMeasPeriod;
            int newI2 = seg.i2*numMinutesInMeasPeriod;

            if (sleepBound.isPresent()) {

                final Optional<Integer> sleepIndex = getMinimumInInterval(smoothedBufferFeats.differentialEnergy,sleepBound.get(),i2Sleep);
                newI1 = sleepIndex.get();
            }

            if (newI1 < lastIndex) {
                newI1 = lastIndex + 5;
            }


            if (wakeIndex.isPresent()) {
                newI2 = wakeIndex.get();
            }

            if (newI2 < newI1) {
                newI2 = newI1 + 5;
            }


            newSegments.add(new SegmentPair(newI1,newI2));

            lastIndex = newI2;
        }

        return ImmutableList.copyOf(newSegments);

    }

    protected ImmutableList<SegmentPair> getIndiciesInMinutesWithIntervalSearchForInAndOutOfBed(final ImmutableList<SegmentPair> segs, final SmoothedBufferFeats smoothedBufferFeats,
                                                                                        final int numMinutesInMeasPeriod) {

        List<SegmentPair> newSegments = new ArrayList<>();

        for (final SegmentPair seg : segs) {

            int i1InBed = seg.i1*numMinutesInMeasPeriod - numMinutesInMeasPeriod;
            int i2InBed = seg.i1*numMinutesInMeasPeriod + numMinutesInMeasPeriod;

            if (i1InBed < 0) {
                i1InBed = 0;
            }

            final Optional<Integer> inBed = getMaximumInInterval(smoothedBufferFeats.filteredEnergy, i1InBed, i2InBed);

            int i1OutOfBed = seg.i2*numMinutesInMeasPeriod - numMinutesInMeasPeriod;
            int i2OutOfBed = seg.i2*numMinutesInMeasPeriod + numMinutesInMeasPeriod;

            if (i2OutOfBed > smoothedBufferFeats.filteredEnergy.length) {
                i2OutOfBed = smoothedBufferFeats.filteredEnergy.length;
            }

            final Optional<Integer> outOfBed = getMaximumInInterval(smoothedBufferFeats.filteredEnergy,i1OutOfBed,i2OutOfBed);

            int newI1 = seg.i1*numMinutesInMeasPeriod;
            int newI2 = seg.i2*numMinutesInMeasPeriod;


                if (inBed.isPresent()) {
                    newI1 = inBed.get();
                }



                if (outOfBed.isPresent()) {
                newI2 = outOfBed.get() + 1;
            }



            newSegments.add(new SegmentPair(newI1,newI2));

        }

        return ImmutableList.copyOf(newSegments);

    }






    //tells me when my events were, smoothing over gaps
    //if there are multiple candidates for segments still, pick the largest
    protected ImmutableList<SegmentPairWithGaps> mindTheGapsAndJoinPairs(final ImmutableList<SegmentPair> pairs, final int acceptableGap) {

        final List<SegmentPairWithGaps> candidates = new ArrayList<>();

        if (pairs.isEmpty()) {
            return ImmutableList.copyOf(candidates);
        }


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



        return  ImmutableList.copyOf(candidates);
    }

    static public Optional<SleepHmmResult> processEventsIntoResult(final ImmutableList<SegmentPair> sleeps, final ImmutableList<SegmentPair> beds,final ImmutableList<Integer> path,final TimeIndexInfo info,final SleepDepthSummary sleepDepthSummary) {

        LinkedList<Event> events = new LinkedList<>();
        int minutesSpentInBed = 0;
        int minutesSpentSleeping = 0;
        int numTimesWokenUpDuringSleep = 0;
        int numSeparateSleepSegments = 0;

        if (beds.isEmpty() || sleeps.isEmpty() ) {
            return Optional.absent();
        }

        for (int iSegment = 0; iSegment < sleeps.size(); iSegment++) {
            final SegmentPair seg = sleeps.get(iSegment);

            //if not the first sleep, it's a  going to sleep after a disturbance
            String sleepMessage = English.FALL_ASLEEP_DISTURBANCE_MESSAGE;
            if (iSegment == 0) {
                sleepMessage = English.FALL_ASLEEP_MESSAGE;
            }

            //if not the last wakeup, it's a disturbance-based wakeup
            String wakeupMessage = English.WAKE_UP_DISTURBANCE_MESSAGE;
            if (iSegment == sleeps.size() - 1) {
                wakeupMessage = English.WAKE_UP_MESSAGE;
            }



            events.add(getEventFromIndex(Event.Type.SLEEP, seg.i1, info.t0, info.timezoneOffset, sleepMessage,info.numMinutesInMeasPeriod));
            events.add(getEventFromIndex(Event.Type.WAKE_UP, seg.i2, info.t0, info.timezoneOffset, wakeupMessage,info.numMinutesInMeasPeriod));

            minutesSpentSleeping += (seg.i2 - seg.i1) * info.numMinutesInMeasPeriod;

            numTimesWokenUpDuringSleep += 1;
            numSeparateSleepSegments += 1;

        }

        for (final SegmentPair seg : beds) {
            events.add(getEventFromIndex(Event.Type.IN_BED, seg.i1, info.t0, info.timezoneOffset, English.IN_BED_MESSAGE,info.numMinutesInMeasPeriod));
            events.add(getEventFromIndex(Event.Type.OUT_OF_BED, seg.i2, info.t0, info.timezoneOffset, English.OUT_OF_BED_MESSAGE,info.numMinutesInMeasPeriod));

            //ignore gaps

            minutesSpentInBed += (seg.i2 - seg.i1) * info.numMinutesInMeasPeriod;
        }


        //sort list of events into chronological order
        Comparator<Event> chronologicalComparator = new Comparator<Event>() {
            @Override
            public int compare(final Event o1, final Event o2) {
                final long t1 = o1.getStartTimestamp();
                final long t2 = o2.getStartTimestamp();
                int ret = 0;
                if (t1 < t2) {
                    ret = 1;
                }

                if (t2 < t1) {
                    ret = -1;
                }

                return ret;
            }
        };

        Collections.sort(events,chronologicalComparator);



        /* find orphan in/out of bed pairs and remove since there's no sleep in them */

        /*
        ListIterator<Event> it = events.listIterator();
        Event prev = null;
        while(it.hasNext()) {
            final Event current = it.next();


            if (prev != null) {
                if (prev.getType() == Event.Type.IN_BED && current.getType() == Event.Type.OUT_OF_BED) {
                    minutesSpentInBed -= (current.getStartTimestamp() - prev.getStartTimestamp()) / NUMBER_OF_MILLIS_IN_A_MINUTE;
                    it.previous();
                    it.remove();
                    it.next();
                    it.remove();
                }
            }

            prev = current;

        }
        */

        /*
        final SleepStats sleepStats = new SleepStats(sleepDepthSummary.numberMinutesInDeepSleep,
                sleepDepthSummary.numberMinutesInLightSleep,
                minutesSpentSleeping == 0 ? minutesSpentInBed : minutesSpentSleeping,
                numberOfMotionEvents,
                sleepTimestampMillis,
                wakeUpTimestampMillis,
                sleepOnsetTimeMinutes
        );
        */

        final SleepStats sleepStats = SleepStats.create(0,0,0,0);


        return Optional.of(new SleepHmmResult(sleepStats,path,ImmutableList.copyOf(events)));


    }

    protected ImmutableList<SegmentPairWithGaps> filterSleepSegmentPairsByHeuristic(final ImmutableList<SegmentPairWithGaps> pairs) {
        //heuristic is -- filter out a duration 1 sleep segment, but only if it's before the "main" segment, which is defined as 1 hour of sleep or more.

        final List<SegmentPairWithGaps> filteredResults = new ArrayList<SegmentPairWithGaps>();
        boolean isFoundMainSleepSegment = false;

        for (int i = 0; i < pairs.size(); i++) {
            final SegmentPairWithGaps pair = pairs.get(i);
            final boolean isLast = (i == pairs.size() - 1);
            final int pairDuration = pair.bounds.i2 - pair.bounds.i1 + 1;

            if (pairDuration > 4) {
                isFoundMainSleepSegment = true;
            }

            //haven't found main segment, and is orphan
            if (pairDuration <= 1 && !isFoundMainSleepSegment) {
                continue;
            }


            //is last segment, and is orphan.  Get rid of it!
            if (pairDuration <= 1 && isLast) {
                continue;
            }

            filteredResults.add(pair);
        }

        return ImmutableList.copyOf(filteredResults);

    }

    protected ImmutableList<SegmentPairWithGaps> filterSegmentPairsByDuration(final ImmutableList<SegmentPairWithGaps> pairs,final  int durationThreshold) {

        final List<SegmentPairWithGaps> filteredResults = new ArrayList<SegmentPairWithGaps>();

        for (final SegmentPairWithGaps pair : pairs) {
            final int pairDuration = pair.bounds.i2 - pair.bounds.i1;

            if (pairDuration >= durationThreshold) {
                filteredResults.add(pair);
            }
        }

        return ImmutableList.copyOf(filteredResults);

    }

    protected ImmutableList<SegmentPair> filterPairsByDuration(final ImmutableList<SegmentPair> pairs,final  int durationThreshold) {

        final List<SegmentPair> filteredResults = new ArrayList<SegmentPair>();

        for (final SegmentPair pair : pairs) {
            final int pairDuration = pair.i2 - pair.i1;

            if (pairDuration >= durationThreshold) {
                filteredResults.add(pair);
            }
        }

        return ImmutableList.copyOf(filteredResults);

    }

    //Returns the boundary indices (i.e. a segment) that is in the int array
    //if there is only a termination in the set, then we have the segment t0UTC - t2
    //if there is only a beginning in the set, then we have the segment t1 - tfinal
    protected ImmutableList<SegmentPair> getSetBoundaries(final ImmutableList<Integer> path, final Set<Integer> inSet) {
        boolean foundBeginning = false;


        int t1 = 0;
        int t2 = 0;

        List<SegmentPair> pairList = new ArrayList<SegmentPair>();

        for (int i = 1; i < path.size(); i++) {
            int prev = path.get(i - 1);
            int current = path.get(i);


            if (inSet.contains(current) && !inSet.contains(prev)) {
                foundBeginning = true;
                t1 = i;
            }

            if (!inSet.contains(current) && inSet.contains(prev)) {
                foundBeginning = false;

                pairList.add(new SegmentPair(t1, i - 1));
            }
        }


        if (foundBeginning) {
            pairList.add(new SegmentPair(t1, path.size()));
        }


        return ImmutableList.copyOf(pairList);
    }

    protected SleepDepthSummary getSleepDepthSummary(final TimeIndexInfo timeIndexInfo, final List<TrackerMotion> motionData,List<SegmentPair> sleeps) {
        /*final Integer soundSleepDurationInMinutes, final Integer lightSleepDurationInMinutes,
        final Integer sleepDurationInMinutes,
        final Integer numberOfMotionEvents,*/


        double[] counts = new double[timeIndexInfo.durationInIndices];

        Arrays.fill(counts, 0.0);

        for (final TrackerMotion m : motionData) {
            final int idx = (int)(m.timestamp - timeIndexInfo.t0) / timeIndexInfo.numMinutesInMeasPeriod / NUMBER_OF_MILLIS_IN_A_MINUTE;

            if (idx >= counts.length || idx < 0) {
                continue;
            }

            counts[idx] = (double)m.kickOffCounts;
        }

        int interval = 15 / timeIndexInfo.numMinutesInMeasPeriod;
        if (interval <= 0) {
            interval = 1;
        }

        final SmoothedBufferFeats feats = getSmoothedBuffer(counts,interval,0,false);

        int numMinutesInDeepSleep = 0;
        int numMinutesInShallowSleep = 0;
        for (final SegmentPair segment : sleeps) {
            for (int i = segment.i1; i <= segment.i2; i++) {
                if (feats.filteredEnergy[i] > 0.6) {
                    numMinutesInShallowSleep += timeIndexInfo.numMinutesInMeasPeriod;
                }
                else {
                    numMinutesInDeepSleep += timeIndexInfo.numMinutesInMeasPeriod;
                }
            }
        }


        return new SleepDepthSummary(numMinutesInShallowSleep,numMinutesInDeepSleep);
    }

}

