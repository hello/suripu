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


    private static double [][] getSensorData(final OneDaysSensorData oneDaysSensorData) throws  Exception {

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

            final DateTime time = new DateTime(s.dateTime, DateTimeZone.UTC);
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




            final double [] sleep = algorithmOutput.output[1].clone();
            final double [][] sleepMeas = {algorithmOutput.output[1]};

            final double [] dsleep = new double[sleep.length];

            for (int i = 1; i < dsleep.length; i++) {
                dsleep[i] = sleep[i] - sleep[i-1];
            }


            final double [][] sleepProbsWithDeltaProb = {algorithmOutput.output[1],dsleep};



            final HmmPdfInterface[] obsModelsMain = {new BetaPdf(2.0,10.0,0),new BetaPdf(6.0,6.0,0),new BetaPdf(10.0,2.0,0)};
            final HmmPdfInterface[] obsModelsDiff = {new GaussianPdf(-0.02,0.02,1),new GaussianPdf(0.00,0.02,1),new GaussianPdf(0.02,0.02,1)};


            //iterate through all possible combinations
            final HmmPdfInterface s0 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[0]).withPdf(obsModelsDiff[1]).build(); //low
            final HmmPdfInterface s1 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[2]).build();
            final HmmPdfInterface s2 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[1]).build();
            final HmmPdfInterface s3 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[2]).withPdf(obsModelsDiff[1]).build();
            final HmmPdfInterface s4 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[0]).build();
            final HmmPdfInterface s5 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[1]).build();
            final HmmPdfInterface s6 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[0]).withPdf(obsModelsDiff[1]).build();


            final HmmPdfInterface[] obsModels = {s0,s1,s2,s3,s4,s5,s6};

            final double[][] A = new double[obsModels.length][obsModels.length];
            A[0][0] = 0.99; A[0][1] = 0.01;
            A[1][1] = 0.98; A[1][2] = 0.01; A[1][3] = 0.01;
            A[2][2] = 0.98; A[2][3] = 0.01; A[2][4] = 0.01;
            A[3][3] = 0.99; A[3][2] = 0.001;                A[3][4] = 0.01;
            A[4][4] = 0.99; A[4][5] = 0.01;                                A[4][6] = 0.01;
            A[5][5] = 0.99; A[5][4] = 0.01;
            A[6][6] = 1.0;



            final double[] pi = new double [obsModels.length];
            pi[0] = 1.0;

            //segment this shit
            final HiddenMarkovModelInterface hmm = HiddenMarkovModelFactory.create(HiddenMarkovModelFactory.HmmType.LOGMATH, obsModels.length, A, pi, obsModels, 0);

            final HmmDecodedResult res = hmm.decode(sleepProbsWithDeltaProb,new Integer[]{obsModels.length - 1},MIN_HMM_PDF_EVAL);


            if (res.bestPath.size() <= 1) {
                LOGGER.error("path size <= 1");
                return Optional.absent();
            }
/*
        float [] f = new float[sleep.length];
        for (int i = 0; i < sleep.length; i++) {
            f[i] = (float)sleep[i];
        }
        LOGGER.info("\n{}\n{}",f,res.bestPath);
*/

            int isleep = 0;
            int iwake = 0;

            final List<Event> events = Lists.newArrayList();
            boolean foundSleep = false;
            Integer prevState = res.bestPath.get(0);
            for (int i = 1; i < res.bestPath.size(); i++) {
                final Integer state = res.bestPath.get(i);

                if (!state.equals(prevState)) {
                    LOGGER.info("FROM {} ---> {} at {}",prevState,state,i);
                }

                final long eventTime = i * DateTimeConstants.MILLIS_PER_MINUTE + t0;
                if (state.equals(3) && !prevState.equals(3) && !foundSleep) {
                    LOGGER.info("SLEEP at idx={}, p={}",i,sleepMeas[0][i]);
                    foundSleep = true;
                    isleep = i;
                }
                else if (!state.equals(3) && prevState.equals(3)) {
                    LOGGER.info("WAKE at idx={}, p={}",i,sleepMeas[0][i]);
                    iwake = i;
                }

                prevState = state;

            }

            events.addAll(getAllEvents(offsetMap,t0,isleep,iwake,x[SensorIndices.MY_MOTION_DURATION.index()]));

            return Optional.of(new TimelineAlgorithmResult(AlgorithmType.NEURAL_NET,events));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.absent();
    }

    static protected List<Event> getAllEvents(final TreeMap<Long,Integer> offsetMap, final long t0, final int iSleep, final int iWake, final double [] myMotionDuration) {
        int iInBed = iSleep - 5;
        int iOutOfBed = iWake + 5;

        final double[][] x = {myMotionDuration};
        final double[][] A = {{0.99, 0.01}, {0.001, 0.999}};
        final double[] pi = {0.5, 0.5};
        final HmmPdfInterface[] obsModels = {new PoissonPdf(POISSON_MEAN_FOR_NO_MOTION, 0), new PoissonPdf(POISSON_MEAN_FOR_MOTION, 0)};

        final HiddenMarkovModelInterface hmm = HiddenMarkovModelFactory.create(HiddenMarkovModelFactory.HmmType.LOGMATH, 2, A, pi, obsModels, 0);


        final HmmDecodedResult result = hmm.decode(x, new Integer[]{0, 1}, MIN_HMM_PDF_EVAL);

        //check to make sure it found a motion cluster SOMEWHERE
        boolean valid = false;
        for (final Iterator<Integer> it = result.bestPath.iterator(); it.hasNext(); ) {
            if (it.next() == 1) {
                valid = true;
                break;
            }
        }

        if (valid) {
            boolean foundCluster = false;

            //go backwards from sleep and find beginning of next motion cluster encountered
            for (int i = iSleep; i >= 0; i--) {
                final Integer state = result.bestPath.get(i);

                if (state.equals(1)) {
                    //if motion cluster start was found too far before sleep, then stop search and use default
                    if (iSleep - i > MAX_ON_BED_SEARCH_WINDOW) {
                        break;
                    }

                    foundCluster = true;
                    continue;
                }

                if (state.equals(0) && foundCluster) {
                    iInBed = i;
                    break;
                }
            }

            foundCluster = false;
            for (int i = iWake; i < myMotionDuration.length; i++) {
                final Integer state = result.bestPath.get(i);

                if (state.equals(1)) {
                    //if motion cluster start was found too far after wake, then stop search and use default
                    if (i - iWake > MAX_OFF_BED_SEARCH_WINDOW) {
                        break;
                    }
                    foundCluster = true;
                }

                if (state.equals(0) && foundCluster) {
                    iOutOfBed = i;
                    break;
                }
            }
            //go forwards from wake to find end of next motion cluster encountered
        }
        else {
            LOGGER.warn("action=return_default_on_bed");
        }

        final long inBedTime = getTime(t0,iInBed);
        final long sleepTime = getTime(t0,iSleep);
        final long wakeTime = getTime(t0,iWake);
        final long outOfBedTime = getTime(t0,iOutOfBed);

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




    protected static class PdfCompositeBuilder {
        final private PdfComposite pdf;

        public static PdfCompositeBuilder newBuilder() {
            return new PdfCompositeBuilder();
        }

        private PdfCompositeBuilder() {
            pdf = new PdfComposite();
        }

        private PdfCompositeBuilder(final PdfComposite pdf) {
            this.pdf = pdf;
        }

        public PdfCompositeBuilder withPdf(final HmmPdfInterface pdf) {
            this.pdf.addPdf(pdf);

            return new PdfCompositeBuilder(this.pdf);
        }

        public HmmPdfInterface build() {
            return this.pdf;
        }


    }

}
