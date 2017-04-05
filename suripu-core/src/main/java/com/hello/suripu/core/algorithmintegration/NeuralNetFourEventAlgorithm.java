package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.AlgorithmType;
import com.hello.suripu.core.util.TimelineError;
import com.hello.suripu.core.util.TimelineSafeguards;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;


public class NeuralNetFourEventAlgorithm implements TimelineAlgorithm {

    public static final String DEFAULT_SLEEP_NET_ID = "SLEEP2";
    public static final int ZERO_PADDING = 120;
    private final int startMinuteOfArtificalLight;
    private final int stopMinuteOfArtificalLight;
    private static final int SLEEP_SEGMENT_WINDOW = 30;//target time for heuristic search window (+/- 15 minutes)

    private final TimelineSafeguards timelineSafeguards;


    private static final float[][] LOG_REG_COEFS= {
            {-12.70396615f, 16.47156896f,-0.70026347f, 9.81588636f, 10.93182064f, 7.49249313f, 13.26656004f, -14.99817079f, -67.23055287f, 12.24668456f},
            {-4.86146879f,  0.87862504f,  14.02283871f, 4.8567298f, 0.72371229f, -1.22904787f, -8.7893782f, -5.85255041f, -0.98252416f, -8.48987515f},
            {-5.94178465f,  0.88973046f, -4.54957249f, 8.16073007f, 0.8865088f, 0.94846862f, -5.20884595f, 3.71380632f, -2.67867639f, -8.1039263f},
            {-5.83208241f,  0.7131686f,   9.64108381f, 5.55667835f, 10.33827963f, 1.42633132f, -18.84869548f, -11.75191131f, 1.75550476f, -4.66252723f},
            {-1.66586387f, -3.16735819f,  0.85292951f, -0.84439642f, -0.69779908f, 5.44305782f, -0.86904775f, 0.06884078f, 0.81954587f, -3.27163618f},
            {-8.64036305f, -2.45618041f, -1.14602329f, -16.79151638f, -15.93192807f, 4.11165991f, 9.02440491f, 10.52865278f, 1.34638061f, 2.67418785f},
            {-8.10222516f, -4.5871558f,  -0.54902291f, -7.53428814f, -10.77699021f, 2.61912995f, 2.70533692f, 12.2725388f, -4.49195264f, 2.24017869f},
            {-3.85318262f, -5.340134f,   -1.14491125f, -10.42432379f, -13.11311129f, -1.39511142f, 4.80125726f, 4.75610756f, 18.20060452f, -0.1935579f},
            {-4.283177f,   -3.28554649f,  0.36573255f, -3.60031365f, -7.74112037f, -0.51765022f, 1.06486657f, 5.45591162f, -0.97619659f, 4.95113903f}
    };

    //DO NOT CHANGE THE INDICES
    //as long as index is right, the order shouldn't matter
    //but why would you mess with the order anyway?
    public enum SensorIndices {
        LIGHT(0), //including natural light
        DIFFLIGHT(1),
        WAVES(2),
        SOUND_DISTURBANCE(3),
        SOUND_VOLUME(4),
        MY_MOTION_DURATION(5),
        PARTNER_MOTION_DURATION(6),
        MY_MOTION_MAX_AMPLITUDE(7),
        PARTNER(8), //1: partner tracker motion data present; 0: partner tracker motion data absent
        AGE(9), // default= 0 for missing age / user age < 18
        BMI(10),   // default = 0 for missing or extreme bmi (< 5 or > 40)
        MALE(11), //1:male; 0:female / unknown
        FEMALE(12), //1: female; 0: male / unknown
        LIGHTVAR(13), //rolling variance - 10 minute window
        DIFFLIGHTSMOOTHED(14), //rolling mean (30 minute window) of difference of mean light
        LIGHT_NODAYLIGHT(15); //excluding natural light

        private final int index;
        SensorIndices(int index) { this.index = index; }
        public int index() { return index; }
        final static int MAX_NUM_INDICES = 16;
    }

    public enum PartialSensorIndices{
        MY_MOTION_DURATION(0),
        MY_MOTION_MAX_AMPLITUDE(1),
        DIFFLIGHTSMOOTHED(2),
        SOUND_VOLUME(3),
        WAVES(4);

        private final int index;
        PartialSensorIndices(int index) { this.index = index; }
        public int index() { return index; }
        final static int MAX_NUM_INDICES = 5;
    }

    private final NeuralNetEndpoint endpoint;

    private static final Logger LOGGER = LoggerFactory.getLogger(NeuralNetFourEventAlgorithm.class);

    public NeuralNetFourEventAlgorithm(final NeuralNetEndpoint endpoint, final AlgorithmConfiguration algorithmConfiguration) {
        this.endpoint = endpoint;
        this.startMinuteOfArtificalLight = algorithmConfiguration.getArtificalLightStartMinuteOfDay();
        this.stopMinuteOfArtificalLight = algorithmConfiguration.getArtificalLightStopMinuteOfDay();
        timelineSafeguards = new TimelineSafeguards(Optional.<UUID>absent());
    }

    public NeuralNetFourEventAlgorithm(final NeuralNetEndpoint endpoint) {
        this.endpoint = endpoint;
        startMinuteOfArtificalLight = DateTimeConstants.MINUTES_PER_HOUR * 18;
        stopMinuteOfArtificalLight = DateTimeConstants.MINUTES_PER_HOUR * 6;
        timelineSafeguards = new TimelineSafeguards(Optional.<UUID>absent());
    }



    private static Optional<Integer> getIndex(final long t0, final long t, final int maxIdx) {
        int idx =  (int) ((t - t0) / (DateTimeConstants.MILLIS_PER_MINUTE));

        if (idx < - ZERO_PADDING) {
            return Optional.absent();
        }

        if (idx > maxIdx + ZERO_PADDING) {
            return Optional.absent();
        }

        return Optional.of(idx + ZERO_PADDING);
    }

    protected boolean isArtificalLight(final DateTime localUtcTime) {
        final int minuteOfDay = localUtcTime.getMinuteOfDay();

        // |---0xxxxxxxxxxx1------------|
        //
        // OR
        //
        // |xxxx1--------------------0xx|

        //spans midnight?
        if (stopMinuteOfArtificalLight < startMinuteOfArtificalLight) {
            if (minuteOfDay >= startMinuteOfArtificalLight || minuteOfDay < stopMinuteOfArtificalLight) {
                return true;
            }
        }
        else {
            if (minuteOfDay >= startMinuteOfArtificalLight && minuteOfDay < stopMinuteOfArtificalLight) {
                return true;
            }
        }

        return false;
    }

    protected double [][] getSensorData(final OneDaysSensorData oneDaysSensorData) throws  Exception {

        final List<Sample> light = oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT);
        final List<Sample> soundcount = oneDaysSensorData.allSensorSampleList.get(Sensor.SOUND_NUM_DISTURBANCES);
        final List<Sample> soundvol = oneDaysSensorData.allSensorSampleList.get(Sensor.SOUND_PEAK_DISTURBANCE);
        final List<Sample> waves = oneDaysSensorData.allSensorSampleList.get(Sensor.WAVE_COUNT);

        if (light.isEmpty()) {
            throw new Exception("no data!");
        }

        final long durationMillis =  oneDaysSensorData.endTimeLocalUTC.getMillis() - oneDaysSensorData.startTimeLocalUTC.getMillis();
        final int T = (int)durationMillis / DateTimeConstants.MILLIS_PER_MINUTE + 1 ;

        final long t0 = oneDaysSensorData.startTimeLocalUTC.getMillis() - light.get(0).offsetMillis; //LOCAL ---> UTC
        final int N = SensorIndices.MAX_NUM_INDICES;

        final double [][] x = new double[N][T + 2 * ZERO_PADDING];
        final HashMap<Integer, Double> lightIndexMap = new HashMap<>();
        /*********** LOG  LIGHT, DIFF LOG LIGHT, VAR LOG LIGHT, SMOOTHED DIFF LOG LIGHT and LOG LIGHT NO DAYLIGHT *************/

        for (final Sample s : light) {
            double value = Math.log((s.value * 65536)/256  + 1.0) / Math.log(2);

            if (Double.isNaN(value) || value <= 0.0) {
                value = 0.0;
            }

            final Optional<Integer> idx = getIndex(t0,s.dateTime,T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=LIGHT t0={} t={}",t0,s.dateTime);
                continue;
            }

            x[SensorIndices.LIGHT.index()][idx.get()] = value;
            x[SensorIndices.LIGHT_NODAYLIGHT.index()][idx.get()] = value;
            lightIndexMap.put(idx.get(), value);
        }

        //diff light
        for (int t = ZERO_PADDING + 1; t < T + (ZERO_PADDING -1); t++) {
            if (lightIndexMap.containsKey(t) && lightIndexMap.containsKey(t-1)){ //prevents large difference in light when sensor reading is missing.
                x[SensorIndices.DIFFLIGHT.index()][t - 1] = -(x[SensorIndices.LIGHT.index()][t] - x[SensorIndices.LIGHT.index()][t - 1]);
            }
        }

        //zero out light during natural light times
        for (final Sample s : light) {
            //get local time, enforce artificial light constraint
            final DateTime time = new DateTime(s.dateTime + s.offsetMillis, DateTimeZone.UTC);

            //if between 5am and 6pm, light is "natural", so we don't care about it, and want to zero it out
            if (!isArtificalLight(time)) {

                final Optional<Integer> idx = getIndex(t0,s.dateTime,T);

                if (!idx.isPresent()) {
                    continue;
                }

                x[SensorIndices.LIGHT_NODAYLIGHT.index()][idx.get()] = 0.0;

            }
        }
        //varLight and smoothedLightDiff
        final double [] varLight  = new double[T + 2*ZERO_PADDING];
        final double [] meanLight  = new double[T+ 2*ZERO_PADDING];
        final double [] diffMeanLight = new double[T + 2*ZERO_PADDING];
        final double [] smoothedLightDiff = new double[T + 2*ZERO_PADDING ];
        final int rollingWindowLight = 10;
        final int smoothedWindowLight = 30;
        final int windowSize = T + 2 * ZERO_PADDING;
        for (int idx = 0; idx < windowSize-1 ; idx++){
            final int idxCeiling = idx + 1;
            final int idxFloor= Math.max(idx - rollingWindowLight, 0);
            final int idxFloorDiff= Math.max(idx - smoothedWindowLight, 0);
            varLight[idx] = getVariance(Arrays.copyOfRange(x[SensorIndices.LIGHT.index()], idxFloor, idxCeiling));
            meanLight[idx] = getMean(Arrays.copyOfRange(x[SensorIndices.LIGHT.index()], idxFloor, idxCeiling));
            diffMeanLight[idx] = meanLight[idx] - meanLight[idxFloor];
            smoothedLightDiff[idx] = getMean(Arrays.copyOfRange(diffMeanLight, idxFloorDiff, idxCeiling));

        }

        x[SensorIndices.LIGHTVAR.index()] = varLight;
        x[SensorIndices.DIFFLIGHTSMOOTHED.index()] = smoothedLightDiff;

        /*  WAVES */
        for (final Sample s : waves) {
            final Optional<Integer> idx = getIndex(t0,s.dateTime,T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=WAVES t0={} t={}",t0,s.dateTime);
                continue;
            }

            x[SensorIndices.WAVES.index()][idx.get()]=s.value / 2;
        }

        /* SOUND DISTURBANCES */
        for (final Sample s : soundcount) {
            double value = s.value /2 ;

            if (Double.isNaN(value) || value < 0.0) {
                value = 0.0;
            }

            final Optional<Integer> idx = getIndex(t0,s.dateTime,T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=SOUND_DISTURBANCE t0={} t={}",t0,s.dateTime);
                continue;
            }

            x[SensorIndices.SOUND_DISTURBANCE.index()][idx.get()] = value;
        }

        /* SOUND VOLUME */
        for (final Sample s : soundvol) {
            double value = s.value / 10 - 4.0; // already in db

            if (value < 0.0) {
                value = 0.0;
            }
            final Optional<Integer> idx = getIndex(t0,s.dateTime,T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=SOUND_VOLUME t0={} t={}",t0,s.dateTime);
                continue;
            }

            x[SensorIndices.SOUND_VOLUME.index()][idx.get()] = value;
        }

        /*  MY MOTION  */
        for (final TrackerMotion m : oneDaysSensorData.oneDaysTrackerMotion.originalTrackerMotions) {
            if (m.value == -1) {
                continue;
            }

            final Optional<Integer> idx = getIndex(t0, m.timestamp, T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=MY_MOTION t0={} t={}",t0,m.timestamp);
                continue;
            }

            x[SensorIndices.MY_MOTION_DURATION.index()][idx.get()] = ((double)m.onDurationInSeconds) / 5;


            //normalize to between 0 - 4 or so by dividing by 5000.0
            final double value = ((double)m.value) / 5000.0;
            final double existingValue = x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()][idx.get()];

            //put in max... why? pill timestamps, when truncated, can show up in the same minute.
            x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()][idx.get()] = value;

        }

        /*  PARTNER MOTION  */
        for (final TrackerMotion m : oneDaysSensorData.oneDaysPartnerMotion.originalTrackerMotions) {
            if (m.value == -1) {
                continue;
            }

            final Optional<Integer> idx = getIndex(t0, m.timestamp, T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=PARTNER_MOTION t0={} t={}",t0,m.timestamp);
                continue;
            }

            x[SensorIndices.PARTNER_MOTION_DURATION.index()][idx.get()] = ((double)m.onDurationInSeconds / 5);
        }

        //User attributes - age, bmi, has partner, male, female
        Arrays.fill(x[SensorIndices.AGE.index()], ((double) oneDaysSensorData.userBioInfo.age) /90);
        Arrays.fill(x[SensorIndices.BMI.index()], ((double) oneDaysSensorData.userBioInfo.bmi)/ 50 );
        Arrays.fill(x[SensorIndices.PARTNER.index()], (double) oneDaysSensorData.userBioInfo.partner);
        Arrays.fill(x[SensorIndices.MALE.index()], (double) oneDaysSensorData.userBioInfo.male);
        Arrays.fill(x[SensorIndices.FEMALE.index()], (double) oneDaysSensorData.userBioInfo.female);

        //zero pad
        for (int idx = 0; idx < ZERO_PADDING; idx ++){
            for (int col = 0; col < SensorIndices.MAX_NUM_INDICES; col ++){
                x[col][idx] = 0;
            }
        }
        for (int idx = T + ZERO_PADDING - 1; idx < T+ 2*ZERO_PADDING; idx ++){
            for (int col = 0; col < SensorIndices.MAX_NUM_INDICES; col ++){
                x[col][idx] = 0;
            }

        }

        return x;
    }

    static protected long getTime(final long t0, final int index) {
        return t0 + DateTimeConstants.MILLIS_PER_MINUTE*index;
    }

    /* Get the timezone offset based on the sensor data.  This should keep daylight savings from ever being a problem */
    static protected Integer getOffset(final long time, final TreeMap<Long,Integer> offsetMap) {
        final Long higher = offsetMap.higherKey(time);
        final Long lower = offsetMap.lowerKey(time);

        if (lower == null && higher == null) {
            LOGGER.error("action=exception_due_to_empty_offset_map map_size={} t={}",offsetMap.size(),time);
            throw new AlgorithmException(String.format("unable to map offset from t=%s in map of size %d",new DateTime(time).toString(),offsetMap.size()));
        }

        if (lower == null) {
            return offsetMap.get(higher);
        }

        return offsetMap.get(lower);
    }


    @Override
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData oneDaysSensorData,final TimelineLog log,final long accountId,final boolean feedbackChanged,final Set<String> features) {

        try {
            final double [][] x = getSensorData(oneDaysSensorData);

            final Optional<NeuralNetAlgorithmOutput> outputOptional = endpoint.getNetOutput(DEFAULT_SLEEP_NET_ID,x); //???

            if (!outputOptional.isPresent()) {
                return Optional.absent();
            }

            final NeuralNetAlgorithmOutput algorithmOutput = outputOptional.get();

            if (algorithmOutput.output.length != 9) {
                LOGGER.info("action=return_no_prediction reason=incorrect_output_dimensions dims={}",algorithmOutput.output.length);
                log.addMessage(AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.UNEXEPECTED);
                return Optional.absent();
            }


            final List<Sample> light = oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT);



            if (light.isEmpty()) {
                return Optional.absent();
            }

            final TreeMap<Long,Integer> offsetMap = Maps.newTreeMap();

            for (final Sample s : light) {
                offsetMap.put(s.dateTime,s.offsetMillis);
            }

            //all times in UTC
            final long t0 = oneDaysSensorData.startTimeLocalUTC.minusMillis(light.get(0).offsetMillis).getMillis();
            final long duration = oneDaysSensorData.endTimeLocalUTC.getMillis() - oneDaysSensorData.startTimeLocalUTC.getMillis();
            final long tEnd = t0 + duration;

            //get the earlier of the current time or the end of day time
            final long tf = tEnd < oneDaysSensorData.currentTimeUTC.getMillis() ? tEnd : oneDaysSensorData.currentTimeUTC.getMillis();
            final int iEnd = (int)(tf - t0) / DateTimeConstants.MILLIS_PER_MINUTE + ZERO_PADDING;

            final double [][] xPartial= new double[5][iEnd];
            xPartial[PartialSensorIndices.MY_MOTION_DURATION.index()] = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_DURATION.index()],0,iEnd);
            xPartial[PartialSensorIndices.MY_MOTION_MAX_AMPLITUDE.index()] = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()],0,iEnd);
            xPartial[PartialSensorIndices.DIFFLIGHTSMOOTHED.index()] = Arrays.copyOfRange(x[SensorIndices.DIFFLIGHTSMOOTHED.index()], 0, iEnd);
            xPartial[PartialSensorIndices.SOUND_VOLUME.index()] = Arrays.copyOfRange(x[SensorIndices.SOUND_VOLUME.index()], 0, iEnd);
            xPartial[PartialSensorIndices.WAVES.index()] = Arrays.copyOfRange(x[SensorIndices.WAVES.index()], 0, iEnd);


            final Integer [] sleepSegments = getSleepSegments(accountId, algorithmOutput.output, iEnd);

            if (Arrays.asList(sleepSegments).contains(0)) {
                LOGGER.info("action=return_no_prediction reason=missing_key_events account_id={} night={}",accountId, oneDaysSensorData.date);
                log.addMessage(AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.MISSING_KEY_EVENTS);

                return Optional.absent();
            }
            final List<Event> events = getEventTimes(offsetMap,t0, sleepSegments, xPartial);

            final Map<Event.Type,Event> eventMap = Maps.newHashMap();
            for (final Event event : events) {
                eventMap.put(event.getType(),event);
            }

            final Optional<Event> inbedOptional = Optional.fromNullable(eventMap.get(Event.Type.IN_BED));
            final Optional<Event> sleepOptional = Optional.fromNullable(eventMap.get(Event.Type.SLEEP));
            final Optional<Event> wakeOptional = Optional.fromNullable(eventMap.get(Event.Type.WAKE_UP));
            final Optional<Event> outOfBedOptional = Optional.fromNullable(eventMap.get(Event.Type.OUT_OF_BED));

            final SleepEvents<Optional<Event>> sleepEvents = SleepEvents.create(inbedOptional,sleepOptional,wakeOptional,outOfBedOptional);

            //verify that algorithm produced something useable
            final TimelineError error = timelineSafeguards.checkIfValidTimeline(accountId, AlgorithmType.NEURAL_NET_FOUR_EVENT,
                    sleepEvents,
                    ImmutableList.copyOf(Collections.EMPTY_LIST),
                    ImmutableList.copyOf(oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT)),
                    ImmutableList.copyOf(oneDaysSensorData.oneDaysTrackerMotion.originalTrackerMotions));

            //IF NO ERROR, THEN RETURN
            if (error.equals(TimelineError.NO_ERROR)) {

                log.addMessage(AlgorithmType.NEURAL_NET_FOUR_EVENT,events);

                return Optional.of(new TimelineAlgorithmResult(AlgorithmType.NEURAL_NET_FOUR_EVENT,events));

            }

            //THERE WAS AN ERROR
            log.addMessage(AlgorithmType.NEURAL_NET_FOUR_EVENT,error);


        } catch (Exception e) {
            log.addMessage(AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.UNEXEPECTED);
            LOGGER.error("action=caught_exception exception=Exception error=invalid_index_calculation");
            LOGGER.debug(e.getMessage());
        }

        return Optional.absent();
    }

    static protected List<Event> getEventTimes(final TreeMap<Long,Integer> offsetMap, final long t0,  final Integer[] sleepSegments, final double[][] xPartial) {


        final int timeSteps = xPartial[0].length;
        final int inBedPadding = 3; //minimal difference between in bed time and sleep time
        final int outOfBedPadding = 1; // minimal difference between awake and out of bed time

        final int[] inBedWindow = getSleepSegmentWindow(sleepSegments[0], SLEEP_SEGMENT_WINDOW,  0, timeSteps - 3);
        final int inBedIndex = getInBedIndex(getCopyOfRange(xPartial, inBedWindow[0], inBedWindow[1]), inBedWindow[0]);

        final int[] asleepWindow = getSleepSegmentWindow(sleepSegments[1], SLEEP_SEGMENT_WINDOW,  inBedIndex + inBedPadding, timeSteps - 2 );
        final int asleepIndex = getAsleepIndex(getCopyOfRange(xPartial, asleepWindow[0], asleepWindow[1]), asleepWindow[0]);

        final int[] awakeWindow = getSleepSegmentWindow(sleepSegments[2], SLEEP_SEGMENT_WINDOW,  0, timeSteps - 1 );
        final int awakeIndex= getAwakeIndex(getCopyOfRange(xPartial, awakeWindow[0], awakeWindow[1]), awakeWindow[0]);

        final int[] outOfBedWindow = getSleepSegmentWindow(sleepSegments[3], SLEEP_SEGMENT_WINDOW,  Math.min(awakeIndex + outOfBedPadding, timeSteps), timeSteps);
        final int outOfBedIndex= getOutOfBedIndex(getCopyOfRange(xPartial, outOfBedWindow[0], outOfBedWindow[1]), outOfBedWindow[0]);



        final long inBedTime = getTime(t0,inBedIndex - ZERO_PADDING);
        final long sleepTime = getTime(t0,asleepIndex - ZERO_PADDING);
        final long wakeTime = getTime(t0,awakeIndex - ZERO_PADDING);
        final long outOfBedTime = getTime(t0,outOfBedIndex - ZERO_PADDING);

        final List<Event> events = Lists.newArrayList();
        //create all events

        events.add(Event.createFromType(Event.Type.IN_BED,
                inBedTime,
                inBedTime+DateTimeConstants.MILLIS_PER_MINUTE,
                getOffset(inBedTime,offsetMap),
                Optional.of(English.IN_BED_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        events.add(Event.createFromType(Event.Type.SLEEP,
                sleepTime,
                sleepTime+DateTimeConstants.MILLIS_PER_MINUTE,
                getOffset(sleepTime,offsetMap),
                Optional.of(English.FALL_ASLEEP_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        events.add(Event.createFromType(Event.Type.WAKE_UP,
                wakeTime,
                wakeTime+DateTimeConstants.MILLIS_PER_MINUTE,
                getOffset(wakeTime,offsetMap),
                Optional.of(English.WAKE_UP_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        events.add(Event.createFromType(Event.Type.OUT_OF_BED,
                outOfBedTime,
                outOfBedTime+DateTimeConstants.MILLIS_PER_MINUTE,
                getOffset(outOfBedTime,offsetMap),
                Optional.of(English.OUT_OF_BED_MESSAGE),
                Optional.<SleepSegment.SoundInfo>absent(),
                Optional.<Integer>absent()));

        return events;
    }

    //Searches for first motion event. If there is a 10 minute gap in motion events, finds next "first motion event".
    //If no first motion event is found, defaults to center of search window
    static int getInBedIndex(final double [][] xWindow, final int indexOffset){
        final int windowSize = xWindow[0].length;
        final double onDurationThreshold = 1.0;
        final int noMotionThreshold = 5;
        final List<Integer> motionEventIndices = new ArrayList<Integer>();
        int i = 0;
        for (final double od : xWindow[0]){
            if (od >= onDurationThreshold){
                motionEventIndices.add(i);
            }
            i ++;
        }
        for (final int motionEventIndex : motionEventIndices){
            int noMotionCount  = 0;
            if (motionEventIndex + noMotionThreshold > windowSize){
                return motionEventIndex + indexOffset;
            }
            for (int j = 0; j < windowSize - motionEventIndex; j++){
                if(xWindow[0][j+motionEventIndex] == 0){
                    noMotionCount ++;
                } else {
                    noMotionCount = 0;
                }
                if (noMotionCount >= noMotionThreshold){
                    break;
                }
            }
            if (noMotionCount >= noMotionThreshold){
                continue;
            } else {
                return motionEventIndex + indexOffset;
            }
        }
        return windowSize/ 2 + indexOffset;
    }

    //Finds the latest occurrence of lights out, loud noise, large motion event as a starting point to look for last motion event.
    //Then searches for last motion followed by no motion interval.
    static int getAsleepIndex(final double [][] xWindow, final int indexOffset){
        final int windowSize = xWindow[0].length;
        final double lightsOutThreshold = -0.25;
        final int lightsOffset = -15;
        final int defaultOffset = 0;
        final double noiseThreshold = 4.0;
        final double motionAmpThreshold = 0.625;

        final int lightsOutIndex = getLastEvent(xWindow[PartialSensorIndices.DIFFLIGHTSMOOTHED.index()], lightsOutThreshold,false,lightsOffset);
        final int lastLoudNoiseIndex = getLastEvent(xWindow[PartialSensorIndices.SOUND_VOLUME.index()], noiseThreshold, true, defaultOffset);
        final int lastLargeMotionEventIndex = getLastEvent(xWindow[PartialSensorIndices.MY_MOTION_MAX_AMPLITUDE.index()], motionAmpThreshold, true, defaultOffset );
        int startLastMotionSearchIndex = Math.max(Math.max(lightsOutIndex, lastLoudNoiseIndex), lastLargeMotionEventIndex);
        final int lastMotionIndex = getLastMotionIndex(Arrays.copyOfRange(xWindow[PartialSensorIndices.MY_MOTION_DURATION.index()],startLastMotionSearchIndex,windowSize), startLastMotionSearchIndex);
        return lastMotionIndex + startLastMotionSearchIndex + indexOffset;
    }

    //Finds the first occurrence of the following events: loud noise, lights on, wave, large motion, long motion, sustained motion.
    // If no event found, defaults to center of search window
    static int getAwakeIndex(final double [][] xWindow, final int indexOffset){
        final int windowSize = xWindow[0].length;
        final int  wakeDefault = windowSize / 2;
        final double noiseThreshold = 1;
        final double lightsOnThreshold = 1.15;
        final double waveThreshold =  0.5;
        final double motionAmpThreshold = 0.8;
        final double onDurThreshold = 1.4;
        final int loudNoiseIndex = getFirstEvent(xWindow[PartialSensorIndices.SOUND_VOLUME.index()], noiseThreshold);
        final int lightsOnIndex = getFirstEvent(xWindow[PartialSensorIndices.DIFFLIGHTSMOOTHED.index()], lightsOnThreshold);
        final int waveIndex = getFirstEvent(xWindow[PartialSensorIndices.WAVES.index()], waveThreshold);
        final int motionAmpEventIndex = getFirstEvent(xWindow[PartialSensorIndices.MY_MOTION_MAX_AMPLITUDE.index()], motionAmpThreshold);
        final int onDurEventIndex = getFirstEvent(xWindow[PartialSensorIndices.MY_MOTION_DURATION.index()], onDurThreshold);
        final int sustainedMotionIndex = getSustainedMotionIndex(xWindow[PartialSensorIndices.MY_MOTION_DURATION.index()]);
        //get first event as wake event
        int wakeIndex = Math.min(lightsOnIndex, Math.min(loudNoiseIndex,Math.min(waveIndex,Math.min(motionAmpEventIndex, Math.min(onDurEventIndex, sustainedMotionIndex)))));
        if (wakeIndex >= windowSize){
            wakeIndex = wakeDefault;
        }
        return wakeIndex + indexOffset;
    }

    //Searches motion event followed by 15 minutes of no motion - or the end of the search window.
    // Finds long or last motion gap
    static int getOutOfBedIndex(final double [][] xWindow, final int indexOffset){

        final int windowSize = xWindow[0].length;
        final int noMotionThreshold = 15; //minutes
        final int outOfBedDefault = 15;
        int noMotionCount = 0;
        int outOfBedIndex = 0;

        for ( int i = 0; i < windowSize; i++){
            if (xWindow[0][i] == 0){
                noMotionCount ++;
            } else{
                noMotionCount = 0;
                outOfBedIndex = i;
            }
            if (noMotionCount == noMotionThreshold){
                return outOfBedIndex + indexOffset;
            }
        }

        if (outOfBedIndex == 0 && windowSize >=30) {
            outOfBedIndex = outOfBedDefault;
        }

        return outOfBedIndex + indexOffset;
    }

    //gets last event of feature above/below threshold
    static int getLastEvent(final double[] xSensorWindow, final double threshold, final boolean isGreater, final int offset){
        final int windowSize = xSensorWindow.length;
        int modifier = 1;
        int eventIndex = 0;
        if (!isGreater){
            modifier = -1;
        }
        for (int i = 0; i < windowSize; i++){
            if (xSensorWindow[i] * modifier >= threshold * modifier){
                eventIndex = i;
            }
        }
        return Math.min(Math.max(eventIndex + offset, 0), windowSize);
    }

    //looks for motion event followed by 8 minutes of no motion starting at startLastMotionIndex
    //does not reset no motion count with motion events, but subtracts from the count
    //defaults to startLastMotionIndex if no motion gap found
    static int getLastMotionIndex(final double [] xODWindow, final int startIndex){
        final int windowSize = xODWindow.length;
        final int noMotionCountThreshold = 8; //8 minutes
        int noMotionCount = 0;
        for (int i = 0; i < windowSize; i++){
            if (xODWindow[i] == 0){
                noMotionCount ++;
            } else {
                noMotionCount = Math.max(0, noMotionCount - 1);
            }
            if (noMotionCount == noMotionCountThreshold){
                return i - (noMotionCount - 1);
            }
        }
        return 0;
    }

    //
    static int getFirstEvent(final double [] xSensorWindow, final double threshold){
        final int windowSize = xSensorWindow.length;
        for (int i = 0; i < windowSize; i ++){
            if (xSensorWindow[i] >= threshold){
                return i;
            }
        }
        return windowSize;

    }

    static int getSustainedMotionIndex(final double [] xOnDurWindow){
        final int windowSize = xOnDurWindow.length;
        final int motionCountThreshold = 4;
        int motionCount = 0;
        int sustainedMotionIndex = 0;
        for(int i = 0; i < windowSize; i ++){
            if (motionCount == 0){
                sustainedMotionIndex = i;
            }
            if (xOnDurWindow[i] > 0 ){
                motionCount++;
            } else {
                motionCount = Math.max(motionCount - 1, 0);
            }
            if (motionCount == motionCountThreshold){
                return sustainedMotionIndex;
            }

        }
        return windowSize;
    }

    static int[] getSleepSegmentWindow(final int sleepSegment, final int sleepSegmentWindowSize, final int associatedEventIndex, final int timeSteps){

        final int sleepSegmentFloor;
        if (associatedEventIndex > 0){
            sleepSegmentFloor =associatedEventIndex; //last event index + event padding
        } else {
            sleepSegmentFloor = Math.max( (int) (sleepSegment - .5 * sleepSegmentWindowSize), 0); //floor cant be negative
        }
        final int sleepSegmentCeiling = (int) Math.min(Math.max(sleepSegmentFloor + sleepSegmentWindowSize, sleepSegment + .5 * sleepSegmentWindowSize), timeSteps); // maximizes window - sleep_segment_floor + window size, sleep segment prediction + 1/2 window size, or end of time window
        return new int[] {sleepSegmentFloor, sleepSegmentCeiling};
    }

    static Integer[] getSleepSegments(final long accountId, final double[][] nnOutput, final int iEnd){

        final int[] sleepStates = getSleepStates(nnOutput);
        final int uncertaintyCorrection = 10;
        final int flexCorrection = 5;
        final int timeSteps = sleepStates.length;
        Integer [] sleepSegments = {0,0,0,0};

        int idx = 0;
        for(final int sleepState : sleepStates){
            if (sleepState > 1 && sleepSegments[0] == 0){
                sleepSegments[0] = Math.max(idx - flexCorrection, ZERO_PADDING);
            }
            if (sleepState >= 3 && sleepSegments[1] == 0){
                sleepSegments[1] = Math.max(idx - uncertaintyCorrection, sleepSegments[0]);
            }
            if (sleepState >=5 && sleepSegments[2] == 0){
                sleepSegments[2] = Math.min(idx + uncertaintyCorrection, timeSteps - ZERO_PADDING);
            }
            if (sleepState == 8 && sleepSegments[3] == 0){
                sleepSegments[3] = Math.max(Math.min(idx - flexCorrection,timeSteps - ZERO_PADDING), sleepSegments[2]);
                break;
            }
            idx ++;
        }


        for (int i = 0; i < 4; i++){
            if (sleepSegments[i] > iEnd){
                LOGGER.info("action=bounding-sleep-segment reason=predicted-event-in-future algorithm=neural_net_four_event account_id={} segment={} segment_idx={}", accountId, i, sleepSegments[i]);
                sleepSegments[i] = iEnd - ((int) .5 * SLEEP_SEGMENT_WINDOW);
            }
        }


        return sleepSegments;
    }

    static int[] getSleepStates(final double[][] nnOutput){
        final int timeSteps = nnOutput[0].length;
        final int outputDim= nnOutput.length;
        final int[] sleepStates = new int[timeSteps];
        final double[] stateProb = new double[outputDim];
        for (int idx = ZERO_PADDING; idx < timeSteps -  ZERO_PADDING; idx ++){
            int idMax = 0;
            double maxProb = 0;
            for (int i = 0; i < outputDim ; i ++){
                final double exp = Math.exp((double)(LOG_REG_COEFS[i][0] +  LOG_REG_COEFS[i][1]*nnOutput[0][idx] +
                        LOG_REG_COEFS[i][2]*nnOutput[1][idx] + LOG_REG_COEFS[i][3]*nnOutput[2][idx] + LOG_REG_COEFS[i][4]*nnOutput[3][idx] +
                        LOG_REG_COEFS[i][5]*nnOutput[4][idx] + LOG_REG_COEFS[i][6]*nnOutput[5][idx] + LOG_REG_COEFS[i][7]*nnOutput[6][idx] +
                        LOG_REG_COEFS[i][8]*nnOutput[7][idx] + LOG_REG_COEFS[i][9]*nnOutput[8][idx]));
                stateProb[i] = exp / (1 + exp);
                if ( i == 0){
                    maxProb = stateProb[i];
                } else if (stateProb[i] > maxProb){
                    maxProb = stateProb[i];
                    idMax = i;
                }
            }
            sleepStates[idx] = idMax;
        }
        return sleepStates;
    }

    static double getVariance(final double [] data)
    {
        final int nSamples = data.length;
        final double mean = getMean(data);
        double temp = 0;
        for(final  double a :data)
            temp += (a-mean)*(a-mean);
        return temp/nSamples;
    }

    static double getMean(final double [] data){
        final int nSamples = data.length;
        double sum = 0.0;
        for(final double a : data)
            sum += a;
        return sum/nSamples;
    }

    static double [][] getCopyOfRange(final double [][] data, final int idxStart, final int  idxEnd){
        final double [][] subArray = new double[data.length][idxEnd - idxStart];
        for (int i = 0; i < data.length; i ++) {
            subArray[i] = Arrays.copyOfRange(data[i], idxStart, idxEnd);
        }
        return subArray;
    }

    @Override
    public TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid) {
        return new NeuralNetFourEventAlgorithm(endpoint);
    }


}
