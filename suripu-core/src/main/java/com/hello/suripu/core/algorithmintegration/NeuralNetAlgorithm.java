package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.interpretation.EventIndices;
import com.hello.suripu.algorithm.interpretation.SleepProbabilityInterpreterWithSearch;
import com.hello.suripu.algorithm.outlier.OnBedBounding;
import com.hello.suripu.algorithm.sleep.SleepEvents;
import com.hello.suripu.core.flipper.FeatureFlipper;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by benjo on 2/23/16.
 */
public class NeuralNetAlgorithm implements TimelineAlgorithm {

    public static final String DEFAULT_SLEEP_NET_ID = "SLEEP";

    private final int startMinuteOfArtificalLight;
    private final int stopMinuteOfArtificalLight;

    private final TimelineSafeguards timelineSafeguards;

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
        MY_MOTION_MAX_AMPLITUDE(7);

        private final int index;
        SensorIndices(int index) { this.index = index; }
        public int index() { return index; }
        final static int MAX_NUM_INDICES = 8;
    }

    private final NeuralNetEndpoint endpoint;

    private static final Logger LOGGER = LoggerFactory.getLogger(NeuralNetAlgorithm.class);

    public NeuralNetAlgorithm(final NeuralNetEndpoint endpoint, final AlgorithmConfiguration algorithmConfiguration) {
        this.endpoint = endpoint;
        this.startMinuteOfArtificalLight = algorithmConfiguration.getArtificalLightStartMinuteOfDay();
        this.stopMinuteOfArtificalLight = algorithmConfiguration.getArtificalLightStopMinuteOfDay();
        timelineSafeguards = new TimelineSafeguards(Optional.<UUID>absent());
    }

    public NeuralNetAlgorithm(final NeuralNetEndpoint endpoint) {
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

        /*********** LOG  LIGHT AND DIFF LOG LIGHT *************/
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

                x[SensorIndices.LIGHT.index()][idx.get()] = 0.0;

            }
        }

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
        for (final TrackerMotion m : oneDaysSensorData.oneDaysTrackerMotion.originalTrackerMotions) {
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
        for (final TrackerMotion m : oneDaysSensorData.oneDaysPartnerMotion.originalTrackerMotions) {
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

            if (features.contains(FeatureFlipper.OFF_BED_HMM_MOTION_FILTER)){
                //a little bit of input filtering
                final double[] diffLight = x[SensorIndices.DIFFLIGHT.index()];
                final double[] light = x[SensorIndices.LIGHT.index()];
                final double[] onDurationMotion = x[SensorIndices.MY_MOTION_DURATION.index()];

                //may alter light values
                OnBedBounding.brightenRoomIfHmmModelWorkedOkay(light, diffLight, onDurationMotion, 6.0, 1.0);
            }

            final Optional<NeuralNetAlgorithmOutput> outputOptional = endpoint.getNetOutput(DEFAULT_SLEEP_NET_ID,x);

            if (!outputOptional.isPresent()) {
                return Optional.absent();
            }

            final NeuralNetAlgorithmOutput algorithmOutput = outputOptional.get();

            if (algorithmOutput.output.length == 0) {
                LOGGER.info("action=return_no_prediction reason=zero_length_neural_net_output");
                log.addMessage(AlgorithmType.NEURAL_NET, TimelineError.UNEXEPECTED);
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

            final double [] myDuration = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_DURATION.index()],0,iEnd);
            final double [] myPillMagnitude = Arrays.copyOfRange(x[SensorIndices.MY_MOTION_MAX_AMPLITUDE.index()],0,iEnd);
            final double [] sleepProbs = Arrays.copyOfRange(algorithmOutput.output[1],0,iEnd);

            final Optional<EventIndices> indicesOptional = SleepProbabilityInterpreterWithSearch.getEventIndices
                    (sleepProbs,myDuration,myPillMagnitude);

            if (!indicesOptional.isPresent()) {
                LOGGER.warn("action=return_no_prediction reason=no_event_indices_from_sleep_probability_interpreter");
                log.addMessage(AlgorithmType.NEURAL_NET, TimelineError.MISSING_KEY_EVENTS);
                return Optional.absent();
            }

            final List<Event> events = getAllEvents(offsetMap,t0,indicesOptional.get());

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
                    accountId, AlgorithmType.NEURAL_NET,
                    sleepEvents,
                    ImmutableList.copyOf(Collections.EMPTY_LIST),
                    ImmutableList.copyOf(oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT)),
                    ImmutableList.copyOf(oneDaysSensorData.oneDaysTrackerMotion.originalTrackerMotions));

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

    static protected List<Event> getAllEvents(final TreeMap<Long,Integer> offsetMap, final long t0, final EventIndices indices) {

        final long inBedTime = getTime(t0,indices.iInBed);
        final long sleepTime = getTime(t0,indices.iSleep);
        final long wakeTime = getTime(t0,indices.iWake);
        final long outOfBedTime = getTime(t0,indices.iOutOfBed);

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


    @Override
    public TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid) {
        return new NeuralNetAlgorithm(endpoint);
    }


}
