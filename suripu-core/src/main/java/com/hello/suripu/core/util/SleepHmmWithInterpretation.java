package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.hmm.DiscreteAlphabetPdf;
import com.hello.suripu.algorithm.hmm.GammaPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.algorithm.sleep.SleepEvents;
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

import javax.inject.Named;
import javax.sound.midi.Track;
import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by benjo on 2/25/15.
 */
public class SleepHmmWithInterpretation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SleepHmmWithInterpretation.class);




    final static protected int MIN_DURATION_OF_SLEEP_SEGMENT_IN_MINUTES = 45;
    final static protected int MAX_ALLOWABLE_SLEEP_GAP_IN_MINUTES = 15;

    final static protected int MIN_DURATION_OF_ONBED_SEGMENT_IN_MINUTES = 45;
    final static protected int MAX_ALLOWABLE_ONBED_GAP_IN_MINUTES = 45;

    final static protected int NUMBER_OF_MILLIS_IN_A_MINUTE = 60000;

    final static private double LIGHT_PREMULTIPLIER = 4.0;


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
            final ImmutableList<NamedSleepHmmModel> models = HmmDeserialization.createModelsFromProtobuf(serializedModels,LOGGER);

            return Optional.of(new SleepHmmWithInterpretation(models));

        }
        catch (Exception e) {
            return Optional.absent();
        }

    }

/* MAIN METHOD TO BE USED FOR DATA PROCESSING IS HERE */
    /* Use this method to get all the sleep / bed events from ALL the sensor data and ALL the pill data */
    public Optional<SleepHmmResult> getSleepEventsUsingHMM(final AllSensorSampleList sensors,
                                                           final List<TrackerMotion> pillData,
                                                           final long sleepPeriodStartTime,
                                                           final long sleepPeriodEndTime,
                                                           final long currentTimeInMillis) {



        double lowestModelScore = Float.MAX_VALUE;
        HmmDecodedResult bestResult = null;
        NamedSleepHmmModel bestModel = null;
        long t0 = 0;
        int timezoneOffset = 0;



        /* go through each model, evaluate, find the best  */
        for (final NamedSleepHmmModel model : models) {
            LOGGER.debug("Trying out model \"{}\"",model.modelName);
            final Optional<BinnedData> binnedDataOptional = getBinnedSensorData(sensors, pillData, model,sleepPeriodStartTime,sleepPeriodEndTime,currentTimeInMillis);


            if (!binnedDataOptional.isPresent()) {
                return Optional.absent();
            }

            final BinnedData binnedData = binnedDataOptional.get();

            //they're all the same... just take the last one
            t0 = binnedData.t0;
            timezoneOffset = binnedData.timezoneOffset;


            //only allow ending up in an off-bed state or wake state

            final Integer [] allowableEndings = model.allowableEndingStates.toArray(new Integer[model.allowableEndingStates.size()]);

            //decode via viterbi
            final HmmDecodedResult result = model.hmm.decode(binnedData.data, allowableEndings);

            LOGGER.debug("path={}", getPathAsString(result.bestPath));
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

        ImmutableList<SegmentPairWithGaps> sleep = filterSegmentPairsByDuration(
                mindTheGapsAndJoinPairs(getSetBoundaries(bestResult.bestPath, bestModel.sleepStates),MAX_ALLOWABLE_SLEEP_GAP_IN_MINUTES / bestModel.numMinutesInMeasPeriod),
                MIN_DURATION_OF_SLEEP_SEGMENT_IN_MINUTES / bestModel.numMinutesInMeasPeriod);

        ImmutableList<SegmentPairWithGaps> onBed = filterSegmentPairsByDuration(
                mindTheGapsAndJoinPairs(getSetBoundaries(bestResult.bestPath, bestModel.onBedStates),MAX_ALLOWABLE_ONBED_GAP_IN_MINUTES/ bestModel.numMinutesInMeasPeriod),
                MIN_DURATION_OF_ONBED_SEGMENT_IN_MINUTES / bestModel.numMinutesInMeasPeriod);


        ImmutableList<SegmentPair> sleep2 = pairsWithGapsToPairs(sleep,false);
        ImmutableList<SegmentPair> onBed2 = pairsWithGapsToPairs(onBed,true);

        if (bestModel.isUsingIntervalSearch) {
            //get whatever is earlier
            long endTime = currentTimeInMillis < sleepPeriodEndTime ? currentTimeInMillis : sleepPeriodEndTime;
            endTime -= timezoneOffset; //convert to UTC

            final int numMinutes = (int) ((endTime - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE);

            double [] pillDataArray = getPillDataArray(pillData,t0,numMinutes);

            sleep2 = getIndiciesInMinutesWithIntervalSearch(sleep2,pillDataArray,numMinutesInMeasPeriod,false);
            onBed2 = getIndiciesInMinutesWithIntervalSearch(onBed2,pillDataArray,numMinutesInMeasPeriod,true);
            numMinutesInMeasPeriod = 1;
        }


        return  processEventsIntoResult(numMinutesInMeasPeriod,sleep2,onBed2,t0,timezoneOffset,bestResult.bestPath);


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
        Long eventTime =  getTimeFromBin(index,numMinutesInWindow,t0);

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

        for (final SegmentPair seg : segs) {
            newSegments.add(performIntervalSearch(pillArray, seg, numMinutesInMeasPeriod, numMinutesInMeasPeriod, isInOutOfBed));
        }

        return ImmutableList.copyOf(newSegments);

    }

    //find leading and trailing
    protected SegmentPair performIntervalSearch(final double [] pillArray,final SegmentPair seg,final int numMinutesInMeasPeriod,final int searchRadiusMinutes,final boolean isInOutOfBed) {

        int search1Start = seg.i1 * numMinutesInMeasPeriod - searchRadiusMinutes;

        if (search1Start < 0) {
            search1Start = 0;
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

        Optional<Integer> end2 = getLastInInterval(pillArray, search2Start, search2End);


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
    //if there is only a termination in the set, then we have the segment t0 - t2
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

    protected  List<Sample> getTimeOfDayAsMeasurement(final List<Sample> light, final double startNaturalLightForbiddedenHour, final double stopNaturalLightForbiddenHour) {
        List<Sample> minuteData = new ArrayList<>();

        for (final Sample s : light) {
            //local UTC
            final DateTime t = new DateTime(s.dateTime + s.offsetMillis, DateTimeZone.UTC);
            final float hour = ((float)t.hourOfDay().get()) + t.minuteOfHour().get() / 60.0f;

            float value = 0.0f;
            if (hour > startNaturalLightForbiddedenHour || hour < stopNaturalLightForbiddenHour) {
                value = 1.0f;
            }


            minuteData.add(new Sample(s.dateTime,value,s.offsetMillis));
        }


        return minuteData;
    }


    /*
    *  CONVERT PILL DATA AND SENSOR DATA INTO 2-D ARRAY OF N MINUTE BINS
    *  where N is specified by the model
    *
    *
    *
    * */
    protected Optional<BinnedData> getBinnedSensorData(AllSensorSampleList sensors, List<TrackerMotion> pillData, final NamedSleepHmmModel model,
                                                       final long startTimeMillis, final long endTimeMillis, final long currentTimeInMillis) {
        final List<Sample> light = sensors.get(Sensor.LIGHT);
        final List<Sample> wave = sensors.get(Sensor.WAVE_COUNT);
        final List<Sample> sound = sensors.get(Sensor.SOUND_PEAK_DISTURBANCE);
        final List<Sample> soundCounts = sensors.get(Sensor.SOUND_NUM_DISTURBANCES);

        if (light == Collections.EMPTY_LIST || light.isEmpty()) {
            return Optional.absent();
        }

        /* ideally, this is from sundown to sunrise.... but we just say it's a time you're unlikely to be sleeping in natural daylight  */
        final List<Sample> naturalLightForbidden = getTimeOfDayAsMeasurement(light,model.naturalLightFilterStartHour,model.naturalLightFilterStopHour);

        //get start and end of window
        final int timezoneOffset = light.get(0).offsetMillis;

        //convert all to UTC
        final long t0 = startTimeMillis - timezoneOffset;
        long tf = endTimeMillis - timezoneOffset;

        //somehow, the light data is returned for
        if (currentTimeInMillis < tf && currentTimeInMillis >= t0) {
            tf = currentTimeInMillis;
        }

        final int numMinutesInWindow = model.numMinutesInMeasPeriod;

        //safeguard
        if (numMinutesInWindow <= 0) {
            return Optional.absent();
        }

        final int dataLength = (int) (tf - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesInWindow;

        final double[][] data = new double[HmmDataConstants.NUM_DATA_DIMENSIONS][dataLength];

        //zero out data
        for (int i = 0; i < HmmDataConstants.NUM_DATA_DIMENSIONS; i++) {
            Arrays.fill(data[i], 0.0);
        }

        //start filling in the sensor data.

        /////////////////////////////////////////////
        //LOG OF LIGHT, CONTINUOUS
        final Iterator<Sample> it1 = light.iterator();
        while (it1.hasNext()) {
            final Sample sample = it1.next();
            double value = sample.value;
            if (value < 0) {
                value = 0.0;
            }
            //add so we can average later
            addToBin(data, sample.dateTime, value, HmmDataConstants.LIGHT_INDEX, t0, numMinutesInWindow);
        }

        //computing average light in bin, so divide by bin size
        for (int i = 0; i < data[HmmDataConstants.LIGHT_INDEX].length; i++) {
            data[HmmDataConstants.LIGHT_INDEX][i] /= numMinutesInWindow; //average

            //transform via log2(4.0 * x + 1.0)
            data[HmmDataConstants.LIGHT_INDEX][i] =  Math.log(data[HmmDataConstants.LIGHT_INDEX][i] * LIGHT_PREMULTIPLIER + 1.0) / Math.log(2);
        }

        ///////////////////////////
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
            if (value > model.pillMagnitudeDisturbanceThresholdLsb) {
                maxInBin(data, m.timestamp, 1.0, HmmDataConstants.DISTURBANCE_INDEX, t0, numMinutesInWindow);
            }

            addToBin(data, m.timestamp, 1.0, HmmDataConstants.MOT_COUNT_INDEX, t0, numMinutesInWindow);

        }

        ///////////////////////
        //WAVES
        final Iterator<Sample> it3 = wave.iterator();
        while (it3.hasNext()) {
            final Sample sample = it3.next();
            double value = sample.value;

            //either wave happened or it didn't.. value can be 1.0 or 0.0
            if (value > 0.0) {
                maxInBin(data, sample.dateTime, 1.0, HmmDataConstants.DISTURBANCE_INDEX, t0, numMinutesInWindow);
            }
        }

        ///////////////////////////////////
        //SOUND MAGNITUDE ---> DISTURBANCE
        final Iterator<Sample> it4 = sound.iterator();
        while (it4.hasNext()) {
            final Sample sample = it4.next();
            double value = sample.value;

            if (value > model.soundDisturbanceThresholdDB) {
                maxInBin(data, sample.dateTime, 1.0, HmmDataConstants.DISTURBANCE_INDEX, t0, numMinutesInWindow);
            }
        }

        //SOUND COUNTS
        final Iterator<Sample> it5 = soundCounts.iterator();
        while (it5.hasNext()) {
            final Sample sample = it5.next();
            final double value = Math.log(sample.value + 1.0f) / Math.log(2);

            //accumulate
            addToBin(data, sample.dateTime, value, HmmDataConstants.LOG_SOUND_COUNT_INDEX, t0, numMinutesInWindow);

        }

        //transform via log2 (1.0 + x)
        for (int i = 0; i < data[HmmDataConstants.LOG_SOUND_COUNT_INDEX].length; i++) {
            data[HmmDataConstants.LOG_SOUND_COUNT_INDEX][i] =  Math.log(data[HmmDataConstants.LOG_SOUND_COUNT_INDEX][i] + 1.0) / Math.log(2);
        }


        //FORBIDDEN NATURAL LIGHT
        final Iterator<Sample> it6 = naturalLightForbidden.iterator();
        while (it6.hasNext()) {
            final Sample sample = it6.next();

            if (sample.value > 0.0) {
                maxInBin(data,sample.dateTime,1.0,HmmDataConstants.NATURAL_LIGHT_FILTER_INDEX,t0,numMinutesInWindow);
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
        LOGGER.debug("light={}",getDoubleVectorAsString(data[HmmDataConstants.LIGHT_INDEX]));
        LOGGER.debug("motion={}",getDoubleVectorAsString(data[HmmDataConstants.MOT_COUNT_INDEX]));
        LOGGER.debug("waves={}", getDoubleVectorAsString(data[HmmDataConstants.DISTURBANCE_INDEX]));
        LOGGER.debug("logsc={}", getDoubleVectorAsString(data[HmmDataConstants.LOG_SOUND_COUNT_INDEX]));
        LOGGER.debug("natlight={}", getDoubleVectorAsString(data[HmmDataConstants.NATURAL_LIGHT_FILTER_INDEX]));


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

    protected String getPathAsString(final ImmutableList<Integer> path) {
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

}

