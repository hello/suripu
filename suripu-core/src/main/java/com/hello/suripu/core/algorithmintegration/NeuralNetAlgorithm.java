package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.hmm.BetaPdf;
import com.hello.suripu.algorithm.hmm.GaussianPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelFactory;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelInterface;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.algorithm.interpretation.SleepProbabilityInterpreter;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.SleepSegment;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;
import com.hello.suripu.core.translations.English;
import com.hello.suripu.core.util.AlgorithmType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by benjo on 2/23/16.
 */
public class NeuralNetAlgorithm implements TimelineAlgorithm {

    public static final String DEFAULT_SLEEP_NET_ID = "SLEEP";

    final static double POISSON_MEAN_FOR_MOTION = 3.0;
    final static double POISSON_MEAN_FOR_NO_MOTION = 0.1;
    final static double MIN_HMM_PDF_EVAL = 1e-320;

    final static int MAX_ON_BED_SEARCH_WINDOW = 30; //minutes
    final static int MAX_OFF_BED_SEARCH_WINDOW = 30; //minutes

    //DO NOT CHANGE THE ORDER OF THESE
    public enum SensorIndices {
        LIGHT(0),
        DIFFLIGHT(1),
        WAVES(2),
        SOUND_DISTURBANCE(3),
        SOUND_VOLUME(4),
        MY_MOTION_DURATION(5),
        PARTNER_MOTION_DURATION(6),
        MY_MOTION_MAX_NORM(7);

        private final int index;
        SensorIndices(int index) { this.index = index; }
        public int index() { return index; }
    }

    private final NeuralNetEndpoint endpoint;

    private static final Logger LOGGER = LoggerFactory.getLogger(NeuralNetAlgorithm.class);

    public NeuralNetAlgorithm(final NeuralNetEndpoint endpoint) {
        this.endpoint = endpoint;
    }



    private static int getIndex(final long t0, final long t) {
        return (int) ((t - t0) / (DateTimeConstants.MILLIS_PER_MINUTE));
    }


    protected static double [][] getSensorData(final OneDaysSensorData oneDaysSensorData) throws  Exception {

        final List<Sample> light = oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT);
        final List<Sample> soundcount = oneDaysSensorData.allSensorSampleList.get(Sensor.SOUND_NUM_DISTURBANCES);
        final List<Sample> soundvol = oneDaysSensorData.allSensorSampleList.get(Sensor.SOUND_PEAK_DISTURBANCE);
        final List<Sample> waves = oneDaysSensorData.allSensorSampleList.get(Sensor.WAVE_COUNT);

        if (light.isEmpty()) {
            throw new Exception("no data!");
        }
        final long t0 = light.get(0).dateTime;

        final long tf = light.get(light.size()-1).dateTime;

        final int T = (int) ((tf - t0) / (long) DateTimeConstants.MILLIS_PER_MINUTE) + 1;
        final int N = 7;

        final double [][] x = new double[N][T];

        for (final Sample s : light) {
            double value = Math.log(s.value + 1.0) / Math.log(2);

            //get local time, enforce artificial light constraint (light before 8pm and after 5am is considered "natural" and thus should be ignored)
            final DateTime time = new DateTime(s.dateTime + s.offsetMillis, DateTimeZone.UTC);
            if (time.getHourOfDay() >= 5 && time.getHourOfDay() < 20) {
                continue;
            }

            if (Double.isNaN(value) || value < 0.0) {
                value = 0.0;
            }

            x[SensorIndices.LIGHT.index()][getIndex(t0,s.dateTime)] = value;
        }

        //diff light
        for (int t = 1; t < x[0].length; t++) {
            x[SensorIndices.DIFFLIGHT.index()][t] = x[SensorIndices.LIGHT.index()][t] - x[SensorIndices.LIGHT.index()][t-1];
        }

        //waves
        for (final Sample s : waves) {
            x[SensorIndices.WAVES.index()][getIndex(t0,s.dateTime)] = s.value;
        }

        //sound disturbance counts
        for (final Sample s : soundcount) {
            double value = Math.log(s.value + 1.0) / Math.log(2);

            if (Double.isNaN(value) || value < 0.0) {
                value = 0.0;
            }

            x[SensorIndices.SOUND_DISTURBANCE.index()][getIndex(t0,s.dateTime)] = value;
        }

        //sould volume
        for (final Sample s : soundvol) {
            double value = 0.1 * s.value - 4.0;

            if (value < 0.0) {
                value = 0.0;
            }
            x[SensorIndices.SOUND_VOLUME.index()][getIndex(t0,s.dateTime)] = value;
        }

        for (final TrackerMotion m : oneDaysSensorData.originalTrackerMotions) {
            x[SensorIndices.MY_MOTION_DURATION.index()][getIndex(t0,m.timestamp)] = m.onDurationInSeconds;
        }

        for (final TrackerMotion m : oneDaysSensorData.originalPartnerTrackerMotions) {
            x[SensorIndices.PARTNER_MOTION_DURATION.index()][getIndex(t0,m.timestamp)] = m.onDurationInSeconds;
        }


        return x;
    }

    static protected long getTime(final long t0, final int index) {
        return t0 + DateTimeConstants.MILLIS_PER_MINUTE*index;
    }

    static protected Integer getOffset(final long time, final TreeMap<Long,Integer> offsetMap) {
        final Long higher = offsetMap.higherKey(time);
        final Long lower = offsetMap.lowerKey(time);

        if (lower == null && higher == null) {
            throw new AlgorithmException(String.format("unable to map offset from t=%s in map of size %d",new DateTime(time).toString(),offsetMap.size()));
        }

        if (lower == null) {
            return offsetMap.get(higher);
        }
        else {
            return offsetMap.get(lower);
        }

    }


    @Override
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData oneDaysSensorData,final TimelineLog log,final long accountId,final boolean feedbackChanged) {

        try {
            final double [][] x = getSensorData(oneDaysSensorData);


            final Optional<NeuralNetAlgorithmOutput> outputOptional = endpoint.getNetOutput(DEFAULT_SLEEP_NET_ID,x);

            if (!outputOptional.isPresent()) {
                return Optional.absent();
            }

            final NeuralNetAlgorithmOutput algorithmOutput = outputOptional.get();

            final List<Sample> light = oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT);

            if (light.isEmpty()) {
                return Optional.absent();
            }

            final TreeMap<Long,Integer> offsetMap = Maps.newTreeMap();

            for (final Sample s : light) {
                offsetMap.put(s.dateTime,s.offsetMillis);
            }

            final long t0 = light.get(0).dateTime; //utc local


            final Optional<SleepProbabilityInterpreter.EventIndices> indicesOptional = SleepProbabilityInterpreter.getEventIndices(algorithmOutput.output[1],x[SensorIndices.MY_MOTION_DURATION.index()]);

            if (!indicesOptional.isPresent()) {
                LOGGER.warn("status=no_event_indices");
                return Optional.absent();
            }

            return Optional.of(new TimelineAlgorithmResult(AlgorithmType.NEURAL_NET,getAllEvents(offsetMap,t0,indicesOptional.get())));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.absent();
    }

    static protected List<Event> getAllEvents(final TreeMap<Long,Integer> offsetMap, final long t0, final SleepProbabilityInterpreter.EventIndices indices) {

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
