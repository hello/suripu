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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;


public class NeuralNetFourEventAlgorithm implements TimelineAlgorithm {

    public static final String DEFAULT_SLEEP_NET_ID = "SLEEP";

    private final int startMinuteOfArtificalLight;
    private final int stopMinuteOfArtificalLight;

    private final TimelineSafeguards timelineSafeguards;


    private static final float[][] LOG_REG_COEFS= {
            {-12.70396615f,16.47156896f, -0.70026347f, 9.81588636f, 10.93182064f, 7.49249313f, 13.26656004f, -14.99817079f, -67.23055287f, 12.24668456f},
            {-4.86146879f, 0.87862504f, 14.02283871f, 4.8567298f, 0.72371229f, -1.22904787f, -8.7893782f, -5.85255041f, -0.98252416f, -8.48987515f},
            {-5.94178465f, 0.88973046f, -4.54957249f, 8.16073007f, 0.8865088f, 0.94846862f, -5.20884595f, 3.71380632f, -2.67867639f, -8.1039263f},
            {-5.83208241f, 0.7131686f, 9.64108381f, 5.55667835f, 10.33827963f, 1.42633132f, -18.84869548f, -11.75191131f, 1.75550476f, -4.66252723f},
            {-1.66586387f, -3.16735819f, 0.85292951f, -0.84439642f, -0.69779908f, 5.44305782f, -0.86904775f, 0.06884078f, 0.81954587f, -3.27163618f},
            {-8.64036305f, -2.45618041f, -1.14602329f, -16.79151638f, -15.93192807f, 4.11165991f, 9.02440491f, 10.52865278f, 1.34638061f, 2.67418785f},
            {-8.10222516f, -4.5871558f, -0.54902291f, -7.53428814f, -10.77699021f, 2.61912995f, 2.70533692f, 12.2725388f, -4.49195264f, 2.24017869f},
            {-3.85318262f, -5.340134f, -1.14491125f, -10.42432379f, -13.11311129f, -1.39511142f, 4.80125726f, 4.75610756f, 18.20060452f, -0.1935579f},
            {-4.283177f, -3.28554649f, 0.36573255f, -3.60031365f, -7.74112037f, -0.51765022f, 1.06486657f, 5.45591162f, -0.97619659f, 4.95113903f}
    };

    //DO NOT CHANGE THE INDICES
    //as long as index is right, the order shouldn't matter
    //but why would you mess with the order anyway?
    public enum SensorIndices {
        LIGHT(0),
        DIFFLIGHT(1),
        WAVES(2),
        SOUND_DISTURBANCE(3),
        SOUND_VOLUME(4),
        MY_MOTION_DURATION(5),
        PARTNER_MOTION_DURATION(6),
        MY_MOTION_MAX_AMPLITUDE(7),
        PARTNER(8),
        AGE(9),
        BMI(10),
        MALE(11),
        FEMALE(12),
        DIFFLIGHTSMOOTHED(13),
        LIGHTVAR(14),
        LIGHT_NODAYLIGHT(15);



        private final int index;
        SensorIndices(int index) { this.index = index; }
        public int index() { return index; }
        final static int MAX_NUM_INDICES = 16;
    }

    private final NeuralNetEndpoint endpoint;

    private static final Logger LOGGER = LoggerFactory.getLogger(NeuralNetAlgorithm.class);

    public NeuralNetFourEventAlgorithm(final NeuralNetEndpoint endpoint, final AlgorithmConfiguration algorithmConfiguration) {
        this.endpoint = endpoint;
        this.startMinuteOfArtificalLight = algorithmConfiguration.getArtificalLightStartMinuteOfDay();
        this.stopMinuteOfArtificalLight = algorithmConfiguration.getArtificalLightStopMinuteOfDay();
        timelineSafeguards = new TimelineSafeguards(Optional.<UUID>absent());
    }

    public NeuralNetFourEventAlgorithm(final NeuralNetEndpoint endpoint) {
        this.endpoint = endpoint;
        startMinuteOfArtificalLight = DateTimeConstants.MINUTES_PER_HOUR * 21 + 30; //21:30
        stopMinuteOfArtificalLight = 5*60;
        timelineSafeguards = new TimelineSafeguards(Optional.<UUID>absent());
    }



    private static Optional<Integer> getIndex(final long t0, final long t, final int maxIdx) {
        int idx =  (int) ((t - t0) / (DateTimeConstants.MILLIS_PER_MINUTE));

        if (idx < 0) {
            return Optional.absent();
        }

        if (idx >= maxIdx) {
            return Optional.absent();
        }

        return Optional.of(idx);
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

        final long t0 = oneDaysSensorData.startTimeLocalUTC.getMillis() - light.get(0).offsetMillis; //LOCAL ---> UTC
        final int T = (int)durationMillis / DateTimeConstants.MILLIS_PER_MINUTE + 1;
        final int N = SensorIndices.MAX_NUM_INDICES;

        final double [][] x = new double[N][T];

        /*********** LOG  LIGHT, DIFF LOG LIGHT, VAR LOG LIGHT, SMOOTHED DIFF LOG LIGHT and LOG LIGHT NO DAYLIGHT *************/
        for (final Sample s : light) {
            double value = Math.log(s.value + 1.0) / Math.log(2);

            if (Double.isNaN(value) || value < 0.0) {
                value = 0.0;
            }

            final Optional<Integer> idx = getIndex(t0,s.dateTime,T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=LIGHT t0={} t={}",t0,s.dateTime);
                continue;
            }

            x[SensorIndices.LIGHT.index()][idx.get()] = value;
            x[SensorIndices.LIGHT_NODAYLIGHT.index()][idx.get()] = value;
        }

        //diff light
        for (int t = 1; t < x[0].length; t++) {
            x[SensorIndices.DIFFLIGHT.index()][t] = x[SensorIndices.LIGHT.index()][t] - x[SensorIndices.LIGHT.index()][t-1];
        }

        //zero out light during natural light times
        for (final Sample s : light) {
            //get local time, enforce artificial light constraint
            final DateTime time = new DateTime(s.dateTime + s.offsetMillis, DateTimeZone.UTC);

            //if between 5am and 10pm, light is "natural", so we don't care about it, and want to zero it out
            if (!isArtificalLight(time)) {

                final Optional<Integer> idx = getIndex(t0,s.dateTime,T);

                if (!idx.isPresent()) {
                    continue;
                }

                x[SensorIndices.LIGHT_NODAYLIGHT.index()][idx.get()] = 0.0;

            }
        }
        //varLight and smoothedLightDiff
        final double [] varLight  = new double[T];
        final double [] meanLight  = new double[T];
        final double [] diffMeanLight = new double[T];
        final double [] smoothedLightDiff = new double[T];
        final int rollingWindowLight = 10;
        final int smoothedWindowLight = 30;
        for (int idx = 0; idx < x[0].length; idx++){
            final int idxCeiling = Math.min(idx + rollingWindowLight, x[0].length - 1);
            final int idxFloorDiff = Math.max(idx - 1, 0);
            varLight[idx] = getVariance(Arrays.copyOfRange(x[SensorIndices.LIGHT.index()], idx, idxCeiling));
            meanLight[idx] = getMean(Arrays.copyOfRange(x[SensorIndices.LIGHT.index()], idx, idxCeiling));
            diffMeanLight[idx] = meanLight[idx] - meanLight[idxFloorDiff];
        }
        for (int idx = 0; idx < x[0].length; idx++){
            final int idxCeiling = Math.min(idx + smoothedWindowLight, x[0].length - 1);
            smoothedLightDiff[idx] = getMean(Arrays.copyOfRange(diffMeanLight, idx, idxCeiling));
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

            x[SensorIndices.WAVES.index()][idx.get()]=s.value;
        }

        /* SOUND DISTURBANCES */
        for (final Sample s : soundcount) {
            double value = Math.log(s.value + 1.0) / Math.log(2);

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
            double value = 0.1 * s.value - 4.0;

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
        for (final TrackerMotion m : oneDaysSensorData.originalTrackerMotions) {
            if (m.value == -1) {
                continue;
            }

            final Optional<Integer> idx = getIndex(t0, m.timestamp, T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=MY_MOTION t0={} t={}",t0,m.timestamp);
                continue;
            }

            x[SensorIndices.MY_MOTION_DURATION.index()][idx.get()] += m.onDurationInSeconds;


            //normalize to between 0 - 20 or so by dividing by 1000.0
            final double value = ((double)m.value) / 1000.0;
            final double existingValue = x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()][idx.get()];

            //put in max... why? pill timestamps, when truncated, can show up in the same minute.
            x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()][idx.get()] = value > existingValue ? value : existingValue;

        }

        /*  PARTNER MOTION  */
        for (final TrackerMotion m : oneDaysSensorData.originalPartnerTrackerMotions) {
            if (m.value == -1) {
                continue;
            }

            final Optional<Integer> idx = getIndex(t0, m.timestamp, T);

            if (!idx.isPresent()) {
                LOGGER.warn("action=skipping_sensor_value sensor=PARTNER_MOTION t0={} t={}",t0,m.timestamp);
                continue;
            }

            x[SensorIndices.PARTNER_MOTION_DURATION.index()][idx.get()] += m.onDurationInSeconds;
        }

        //User attributes - age, bmi, has partner, male, female
        Arrays.fill(x[SensorIndices.AGE.index()], (double) oneDaysSensorData.age);
        Arrays.fill(x[SensorIndices.BMI.index()], (double) oneDaysSensorData.bmi);
        Arrays.fill(x[SensorIndices.PARTNER.index()], (double) oneDaysSensorData.partner);
        Arrays.fill(x[SensorIndices.MALE.index()], (double) oneDaysSensorData.male);
        Arrays.fill(x[SensorIndices.FEMALE.index()], (double) oneDaysSensorData.female);

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

            if (algorithmOutput.output.length == 0) {
                LOGGER.info("action=return_no_prediction reason=zero_length_neural_net_output");
                log.addMessage(AlgorithmType.NEURAL_NET_FOUR_EVENT, TimelineError.UNEXEPECTED);
                return Optional.absent();
            }
            if (algorithmOutput.output[0].length != 9){
                LOGGER.info("action=return_no_prediction reason=incorrect_output_dimensions");
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
            final int iEnd = (int)(tf - t0) / DateTimeConstants.MILLIS_PER_MINUTE + 1;

            final double [][] xPartial= new double[5][iEnd];
            xPartial[0] = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_DURATION.index()],0,iEnd);
            xPartial[1] = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()],0,iEnd);
            xPartial[2] = Arrays.copyOfRange(x[SensorIndices.DIFFLIGHTSMOOTHED.index()], 0, iEnd);
            xPartial[3] = Arrays.copyOfRange(x[SensorIndices.SOUND_VOLUME.index()], 0, iEnd);
            xPartial[4] = Arrays.copyOfRange(x[SensorIndices.WAVES.index()], 0, iEnd);


            final int [] sleepSegments = getSleepSegments(algorithmOutput.output);

            final List<Event> events = getEventTimes(offsetMap,t0, sleepSegments,xPartial);

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
            final TimelineError error = timelineSafeguards.checkIfValidTimeline(
                    sleepEvents,
                    ImmutableList.copyOf(Collections.EMPTY_LIST),
                    ImmutableList.copyOf(oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT)));

            //IF NO ERROR, THEN RETURN
            if (error.equals(TimelineError.NO_ERROR)) {

                log.addMessage(AlgorithmType.NEURAL_NET,events);

                return Optional.of(new TimelineAlgorithmResult(AlgorithmType.NEURAL_NET,events));

            }

            //THERE WAS AN ERROR
            log.addMessage(AlgorithmType.NEURAL_NET,error);


        } catch (Exception e) {
            log.addMessage(AlgorithmType.NEURAL_NET, TimelineError.UNEXEPECTED);
            LOGGER.error("action=caught_exception exception=Exception error=invalid_index_calculation");
            LOGGER.debug(e.getMessage());
        }

        return Optional.absent();
    }

    static protected List<Event> getEventTimes(final TreeMap<Long,Integer> offsetMap, final long t0, final int[] sleepSegments, final double[][] xPartial) {
        final int timeSteps = xPartial[0].length;
        final int sleepSegmentWindowSize = 30;//minutes
        final int inBedPadding = 3;
        final int outOfBedPadding = 1;

        final int[] inBedWindow = getSleepSegmentWindow(sleepSegments[0], sleepSegmentWindowSize,  0, timeSteps);
        final int inBedIndex = getInBedIndex(Arrays.copyOfRange(xPartial, inBedWindow[0], inBedWindow[1]), inBedWindow[0]);

        final int[] asleepWindow = getSleepSegmentWindow(sleepSegments[1], sleepSegmentWindowSize,  inBedIndex + inBedPadding, timeSteps);
        final int asleepIndex = getAsleepIndex(Arrays.copyOfRange(xPartial, asleepWindow[0], asleepWindow[1]), asleepWindow[0]);

        final int[] awakeWindow = getSleepSegmentWindow(sleepSegments[2], sleepSegmentWindowSize,  0, timeSteps);
        final int awakeIndex= getAwakeIndex(Arrays.copyOfRange(xPartial, awakeWindow[0], awakeWindow[1]), awakeWindow[0]);

        final int[] outOfBedWindow = getSleepSegmentWindow(sleepSegments[3], sleepSegmentWindowSize,  awakeIndex + outOfBedPadding, timeSteps);
        final int outOfBedIndex= getOutOfBedIndex(Arrays.copyOfRange(xPartial, outOfBedWindow[0], outOfBedWindow[1]), outOfBedWindow[0]);



        final long inBedTime = getTime(t0,inBedIndex);
        final long sleepTime = getTime(t0,asleepIndex);
        final long wakeTime = getTime(t0,awakeIndex);
        final long outOfBedTime = getTime(t0,outOfBedIndex);

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

    static int getInBedIndex(final double [][] xWindow, final int indexOffset){
        final int windowSize = xWindow[0].length;
        final double onDurationThreshold = 1.0;
        final int noMotionThreshold = 10;
        final List<Integer> motionEventIndices = new ArrayList<Integer>();
        int i = 0;

        int lastMotionIndex = 0;
        for (final double od : xWindow[0]){
            if (od >= onDurationThreshold){
                motionEventIndices.add(i);
                lastMotionIndex = i;
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
            }
            if (noMotionCount == noMotionThreshold){
                continue;
            } else {
                return motionEventIndex + indexOffset;
            }
        }
        return xWindow[0].length / 2 + indexOffset;
    }

    static int getAsleepIndex(final double [][] xWindow, final int indexOffset){

        final int lightsOutIndex = getLightsOut(xWindow);
        final int lastLoudNoiseIndex = getLastLoudNoiseIndex(xWindow);
        final int lastLargeMotionEventIndex = getLastLargeMotionIndex(xWindow);
        int startLastMotionSearchIndex = Math.max(Math.max(lightsOutIndex, lastLoudNoiseIndex), lastLargeMotionEventIndex);

        final int lastMotionIndex = getLastMotionIndex(xWindow, startLastMotionSearchIndex);
        return lastMotionIndex + indexOffset;
    }

    static int getAwakeIndex(final double [][] xWindow, final int indexOffset){
        final int  wakeDefault = 15;
        final int loudNoiseIndex = getFirstNoiseIndex(xWindow);
        final int lightsOnIndex = getLightsOnIndex(xWindow);
        final int waveIndex = getWaveIndex(xWindow);
        final int largeMotionEventIndex = getFirstLargeMotionIndex(xWindow);
        final int longMotionEventIndex = getFirstLongMotionIndex(xWindow);
        final int sustainedMotionIndex = getSustainedMotionIndex(xWindow);
        //get first event as wake event
        int wakeIndex = Math.min(lightsOnIndex, Math.min(loudNoiseIndex,Math.min(waveIndex,Math.min(largeMotionEventIndex, Math.min(longMotionEventIndex, sustainedMotionIndex)))));
        if (wakeIndex == xWindow[0].length - 1){
            wakeIndex = wakeDefault;
        }
        return wakeIndex + indexOffset;
    }

    static int getOutOfBedIndex(final double [][] xWindow, final int indexOffset){
        final int noMotionThreshold = 15; //minutes
        final int outOfBedDefault = 15;
        int noMotionCount = 0;
        int outOfBedIndex = 0;

        for ( int i = 0; i < xWindow[0].length; i++){
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

        if (outOfBedIndex == 0) {
            outOfBedIndex = outOfBedDefault;
        }

        return outOfBedIndex + indexOffset;
    }

    static int getLightsOut(final double [][] xWindow){
        final int lightsOutThreshold = -1;
        final int lightsOffset = 13;
        int lightsOutIndex = 0;
        for (int i = 0;i <= xWindow[0].length;i++){
            if (xWindow[2][i]<= lightsOutThreshold){
                lightsOutIndex = i;
            }
        }
        return Math.max(lightsOutIndex - lightsOffset, 0);
    }

    static int getLastLoudNoiseIndex(final double [][] xWindow){
        final int noiseThreshold = 4;
        int lastLoudNoiseIndex = 0;
        for (int i = 0;i < xWindow[0].length;i++){
            if (xWindow[2][i] >= noiseThreshold){
                lastLoudNoiseIndex = i;
            }
        }
        return lastLoudNoiseIndex;
    }

    static int getLastLargeMotionIndex(final double [][] xWindow){
        final double motionThreshold  = 0.625;
        int lastMotionIndex = 0;

        for (int i = 0; i < xWindow[0].length; i++){

            if (xWindow[1][i] > motionThreshold){
                lastMotionIndex = i;
            }
        }
        return lastMotionIndex;
    }

    static int getLastMotionIndex(final double [][] xWindow, final int startIndex){
        final int noMotionCountThreshold = 8; //8 minutes
        int noMotionCount = 0;
        for (int i = 0; i < xWindow[0].length; i++){
            if (xWindow[0][i] == 0){
                noMotionCount ++;
            } else {
                noMotionCount = Math.max(0, noMotionCount - 1);
            }
            if (noMotionCount == noMotionCountThreshold){
                return i - noMotionCount;
            }
        }
        return startIndex;
    }

    static int getFirstLongMotionIndex(final double [][] xWindow){
        final double onDurThreshold = 1.4;
        for (int i = 0; i < xWindow[0].length; i++){
            if (xWindow[0][i] > onDurThreshold){
                return i;
            }
        }
        return xWindow[0].length;
    }

    static int getFirstLargeMotionIndex(final double [][] xWindow){
        final double motionThreshold = 0.8;
        for (int i = 0; i < xWindow[0].length; i++){
            if (xWindow[1][i] > motionThreshold){
                return i;
            }
        }
        return xWindow[0].length;
    }

    static int getFirstNoiseIndex(final double [][] xWindow){
        final int noiseThreshold = 1;
        for (int i = 0; i < xWindow[0].length; i ++)
            if (xWindow[3][i] > noiseThreshold){
                return i;
            }
        return xWindow[0].length;
    }


    static int getLightsOnIndex(final double [][] xWindow){
        final double lightsDiffThreshold = 1.15;
        for (int i = 0; i < xWindow[0].length; i ++)
            if (xWindow[2][i] > lightsDiffThreshold){
                return i;
            }
        return xWindow[0].length;
    }

    static int getWaveIndex(final double [][] xWindow){
        for (int i = 0; i < xWindow[0].length; i ++)
            if (xWindow[4][i] > 0){
                return i;
            }
        return xWindow[0].length;
    }

    static int getSustainedMotionIndex(final double [][] xWindow){
        final int motionCountThreshold = 4;
        int motionCount = 0;
        int sustainedMotionIndex = 0;
        for(int i = 0; i < xWindow[0].length; i ++){
           if (motionCount == 0){
               sustainedMotionIndex = i;
           }
           if (xWindow[0][i] > 0 ){
               motionCount++;
           } else {
               motionCount = Math.max(motionCount - 1, 0);
           }
           if (motionCount == motionCountThreshold){
               return sustainedMotionIndex;
           }

        }
        return xWindow[0].length;
    }

    static int[] getSleepSegmentWindow(final int sleepSegment, final int sleepSegmentWindowSize, final int associatedEventIndex, final int timeSteps){

        final int sleepSegmentCeiling = Math.min( (int) (sleepSegment + .5 * sleepSegmentWindowSize), timeSteps);
        final int sleepSegmentFloor;
        if (associatedEventIndex > 0){
            sleepSegmentFloor =associatedEventIndex;
        } else {
            sleepSegmentFloor = Math.min( (int) (sleepSegment - .5 * sleepSegmentCeiling), 0);
        }

        return new int[] {sleepSegmentFloor, sleepSegmentCeiling};
    }

    static int[] getSleepSegments(final double[][] nnOutput){

        final int[] sleepStates = getSleepStates(nnOutput);
        final int uncertaintyCorrection = 10;
        final int flexCorrection = 5;
        final int timeSteps = sleepStates.length;
        int[] sleepSegments = {0,0,0,0};

        int idx = 0;
        for(final int sleepState : sleepStates){
            if (sleepState > 1 && sleepSegments[0] == 0){
                sleepSegments[0] = Math.max(idx - flexCorrection, 0);
            }
            if (sleepState > 3 && sleepSegments[1] == 0){
                sleepSegments[1] = Math.max(idx - uncertaintyCorrection, sleepSegments[0]);
            }
            if (sleepState > 4 && sleepSegments[2] == 0){
                sleepSegments[2] = Math.min(idx + uncertaintyCorrection, timeSteps);
            }
            if (sleepState == 8 && sleepSegments[3] == 0){
                sleepSegments[3] = Math.max(idx - flexCorrection,sleepSegments[2]);
            }
            idx ++;
        }
        if (sleepSegments[1] < sleepSegments[0]){
            sleepSegments[1] = sleepSegments[0] + 1;
        }
        if (sleepSegments[3]<sleepSegments[2]){
            sleepSegments[3] = sleepSegments[2] + 1;
        }

        return sleepSegments;
    }

    static int[] getSleepStates(final double[][] nnOutput){
        final int timeSteps = nnOutput[0].length;
        final int outputDim= nnOutput.length;
        final int[] sleepStates = new int[timeSteps];
        final double[] stateProb = new double[outputDim];
        for (int idx = 0; idx < timeSteps; idx ++){
             int idMax = 0;
             double maxProb = 0;
             for (int i = 0; i < outputDim; i ++){
                 final double exp = Math.exp((double)(LOG_REG_COEFS[i][0] +  LOG_REG_COEFS[i][1]*nnOutput[idx][0] +
                                 LOG_REG_COEFS[i][2]*nnOutput[idx][1] + LOG_REG_COEFS[i][3]*nnOutput[idx][2] + LOG_REG_COEFS[i][4]*nnOutput[idx][3] +
                                 LOG_REG_COEFS[i][5]*nnOutput[idx][4] + LOG_REG_COEFS[i][6]*nnOutput[idx][5] + LOG_REG_COEFS[i][7]*nnOutput[idx][6] +
                                 LOG_REG_COEFS[i][8]*nnOutput[idx][7] + LOG_REG_COEFS[i][9]*nnOutput[idx][8]));
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
        for(double a :data)
            temp += (a-mean)*(a-mean);
        return temp/nSamples;
    }

    static double getMean(final double [] data){
        final int nSamples = data.length;
        double sum = 0.0;
        for(double a : data)
            sum += a;
        return sum/nSamples;
    }

    @Override
    public TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid) {
        return new NeuralNetAlgorithm(endpoint);
    }


}
