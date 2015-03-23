package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    final ImmutableList<NamedSleepHmmModel> models;

    ////////////////////////////
    //Externally available results class
    public static class SleepStats {
        public final int minutesSpentInBed;
        public final int minutesSpentSleeping;
        public final int numTimesWokenUpDuringSleep;
        public final int numSeparateSleepSegments;


        public SleepStats(int minutesSpentInBed, int minutesSpentSleeping, int numTimesWokenUpDuringSleep, int numSeparateSleepSegments) {
            this.minutesSpentInBed = minutesSpentInBed;
            this.minutesSpentSleeping = minutesSpentSleeping;
            this.numTimesWokenUpDuringSleep = numTimesWokenUpDuringSleep;
            this.numSeparateSleepSegments = numSeparateSleepSegments;
        }
    }

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


            final int numMinutes = (int) ((endTimeMillisUTC - startTimeMillisInUTC) / NUMBER_OF_MILLIS_IN_A_MINUTE);

            double [] pillDataArray = getPillDataArray(cleanedUpPillData,startTimeMillisInUTC,numMinutes);

            sleepSplitOnGaps = getIndiciesInMinutesWithIntervalSearch(sleepSplitOnGaps,pillDataArray,numMinutesInMeasPeriod,false);
            onBedIgnoringGaps = getIndiciesInMinutesWithIntervalSearch(onBedIgnoringGaps,pillDataArray,numMinutesInMeasPeriod,true);
            numMinutesInMeasPeriod = 1;
        }


        return  processEventsIntoResult(numMinutesInMeasPeriod,sleepSplitOnGaps,onBedIgnoringGaps,startTimeMillisInUTC,timezoneOffset,bestResult.bestPath);


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


    protected  Event getEventFromIndex(Event.Type eventType, final int index, final long t0, final int timezoneOffset,final String description,final int numMinutesInWindow) {
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

    protected Optional<Integer> getFirstInInterval(final double [] pillArray,final int idx1, final int idx2) {
        for (int i = idx1; i < idx2; i++) {
            if (pillArray[i] != 0.0) {
                return Optional.of(i);
            }
        }
        return Optional.absent();
    }

    protected Optional<Integer> getFirstQuietPeriodInInterval(final double [] pillArray,final int idx1, final int idx2,final int quietcount) {
        boolean isFirst = true;
        int count = 0;
        for (int i = idx1; i < idx2; i++) {
            if (pillArray[i] != 0.0) {
                count = 0;
                isFirst = false;
            }
            else {
                if (!isFirst) {
                    count++;
                }

                if (count >= quietcount) {
                    return Optional.of(i);
                }
            }
        }
        return Optional.absent();
    }

    protected Optional<Integer> getLastInInterval(final double [] pillArray,final int idx1, final int idx2) {
        for (int i = idx2-1; i >- idx1; i--) {
            if (pillArray[i] != 0.0) {
                return Optional.of(i);
            }
        }
        return Optional.absent();
    }

    protected Optional<Double> getMaximumInInterval(final  double [] pillArray,final int idx1, final int idx2) {
        double max = Double.MIN_VALUE;
        for (int i = idx1; i < idx2; i++) {
            if (pillArray[i] != 0.0) {
                if (pillArray[i] > max) {
                    pillArray[i] = max;
                }
            }
        }

        if (max == Double.MIN_VALUE) {
            return Optional.absent();
        }
        else {
            return Optional.of(max);
        }
    }

    protected ImmutableList<SegmentPair> getIndiciesInMinutesWithIntervalSearch(final ImmutableList<SegmentPair> segs, double [] pillArray, final int numMinutesInMeasPeriod,final boolean isInOutOfBed) {

        List<SegmentPair> newSegments = new ArrayList<>();
        int lastIndex = -1;
        for (final SegmentPair seg : segs) {
            final SegmentPair newseg = performIntervalSearch(pillArray, seg, numMinutesInMeasPeriod, numMinutesInMeasPeriod, isInOutOfBed, lastIndex);

            newSegments.add(newseg);

            lastIndex = newseg.i2;
        }

        return ImmutableList.copyOf(newSegments);

    }

    //find leading and trailing
    protected SegmentPair performIntervalSearch(final double [] pillArray,final SegmentPair seg,final int numMinutesInMeasPeriod,final int searchRadiusMinutes,final boolean isInOutOfBed,int lastIndex) {

        int search1Start = seg.i1 * numMinutesInMeasPeriod - searchRadiusMinutes;

        if (search1Start < 0) {
            search1Start = 0;
        }

        if (lastIndex >= search1Start) {
            search1Start = lastIndex + 1;
        }

        int search1End = search1Start + 2*searchRadiusMinutes;




        Optional<Integer> first1 = Optional.absent();

        if (isInOutOfBed) {
            //in/out of bed? get the very first event
            first1 = getFirstInInterval(pillArray, search1Start, search1End);
        }
        else {
            //not sleep gap, so we're going to sleep
            first1 = getFirstQuietPeriodInInterval(pillArray,search1Start,search1End,5);
        }

        int search2Start = (seg.i2 + 1) * numMinutesInMeasPeriod - searchRadiusMinutes;
        int search2End = search2Start + 2*searchRadiusMinutes;

        if (search2End > pillArray.length) {
            search2End = pillArray.length;
        }

        if (search2Start >= search2End) {
            search2Start = search2End - 1;
        }

        Optional<Integer> end2 = Optional.absent();

        if (isInOutOfBed) {
            end2 = getLastInInterval(pillArray, search2Start, search2End);
        }
        else {
            end2 = getFirstInInterval(pillArray, search2Start, search2End);
        }


        int i1 = seg.i1 * numMinutesInMeasPeriod ;
        int i2 = seg.i2 * numMinutesInMeasPeriod - 1 ;

        if (first1.isPresent()) {
            i1 = first1.get();
        }

        if (end2.isPresent()) {
            i2 = end2.get() - 1;
        }

        //make sure at least 5 minutes apart
        if (i2 - i1 < 5) {
            i2 = i1 + 5;
        }

        if (isInOutOfBed) {
            i2 += 1;
            i1 -=1;
        }


        return new SegmentPair(i1,i2);

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

    Optional<SleepHmmResult> processEventsIntoResult(final int numMinutesInMeasPeriod, final ImmutableList<SegmentPair> sleeps, final ImmutableList<SegmentPair> beds, final long t0, final int timezoneOffset, final ImmutableList<Integer> path) {

        List<Event> events = new ArrayList<>();
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



            events.add(getEventFromIndex(Event.Type.SLEEP, seg.i1, t0, timezoneOffset, sleepMessage,numMinutesInMeasPeriod));
            events.add(getEventFromIndex(Event.Type.WAKE_UP, seg.i2, t0, timezoneOffset, wakeupMessage,numMinutesInMeasPeriod));

            minutesSpentSleeping += (seg.i2 - seg.i1) * numMinutesInMeasPeriod;

            numTimesWokenUpDuringSleep += 1;
            numSeparateSleepSegments += 1;

        }

        for (final SegmentPair seg : beds) {
            events.add(getEventFromIndex(Event.Type.IN_BED, seg.i1, t0, timezoneOffset, English.IN_BED_MESSAGE,numMinutesInMeasPeriod));
            events.add(getEventFromIndex(Event.Type.OUT_OF_BED, seg.i2, t0, timezoneOffset, English.OUT_OF_BED_MESSAGE,numMinutesInMeasPeriod));

            //ignore gaps

            minutesSpentInBed += (seg.i2 - seg.i1) * numMinutesInMeasPeriod;
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

        return Optional.of(new SleepHmmResult(new SleepStats(minutesSpentInBed,minutesSpentSleeping,numTimesWokenUpDuringSleep - 1,numSeparateSleepSegments),path,ImmutableList.copyOf(events)));


    }

    protected ImmutableList<SegmentPairWithGaps> filterSleepSegmentPairsByHeuristic(final ImmutableList<SegmentPairWithGaps> pairs) {
        //heuristic is -- filter out a duration 1 sleep segment, but only if it's before the "main" segment, which is defined as 1 hour of sleep or more.

        final List<SegmentPairWithGaps> filteredResults = new ArrayList<SegmentPairWithGaps>();
        boolean isFoundMainSleepSegment = false;

        for (final SegmentPairWithGaps pair : pairs) {
            final int pairDuration = pair.bounds.i2 - pair.bounds.i1 + 1;

            if (pairDuration > 4) {
                isFoundMainSleepSegment = true;
            }


            if (pairDuration <= 1 && !isFoundMainSleepSegment) {
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



}

