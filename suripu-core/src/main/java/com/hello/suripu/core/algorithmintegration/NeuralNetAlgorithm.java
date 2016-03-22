package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.hmm.BetaPdf;
import com.hello.suripu.algorithm.hmm.GaussianPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelFactory;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelInterface;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfComposite;
import com.hello.suripu.core.configuration.NeuralNetServiceClientConfiguration;
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

import java.util.List;
import java.util.UUID;

/**
 * Created by benjo on 2/23/16.
 */
public class NeuralNetAlgorithm implements TimelineAlgorithm {


    private final NeuralNetServiceClientConfiguration neuralNetServiceConfig;
    private final NeuralNetEndpoint endpoint;

    private static final Logger LOGGER = LoggerFactory.getLogger(NeuralNetAlgorithm.class);

    public NeuralNetAlgorithm(final NeuralNetServiceClientConfiguration neuralNetDAO) {
        this.neuralNetServiceConfig = neuralNetDAO;
        this.endpoint = new NeuralNetHttpEndpoint(neuralNetServiceConfig.getEndpoint());
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

            x[0][getIndex(t0,s.dateTime)] = value;
        }

        //diff light
        for (int t = 1; t < x[0].length; t++) {
            x[1][t] = x[0][t] - x[0][t-1];
        }

        //waves
        for (final Sample s : waves) {
            x[2][getIndex(t0,s.dateTime)] = s.value;
        }

        //sound disturbance counts
        for (final Sample s : soundcount) {
            double value = Math.log(s.value + 1.0) / Math.log(2);

            if (Double.isNaN(value) || value < 0.0) {
                value = 0.0;
            }

            x[3][getIndex(t0,s.dateTime)] = value;
        }

        //sould volume
        for (final Sample s : soundvol) {
            double value = 0.1 * s.value - 4.0;

            if (value < 0.0) {
                value = 0.0;
            }
            x[4][getIndex(t0,s.dateTime)] = value;
        }

        for (final TrackerMotion m : oneDaysSensorData.originalTrackerMotions) {
            x[5][getIndex(t0,m.timestamp)] = m.onDurationInSeconds;
        }

        for (final TrackerMotion m : oneDaysSensorData.originalPartnerTrackerMotions) {
            x[6][getIndex(t0,m.timestamp)] = m.onDurationInSeconds;
        }


        return x;
    }

    @Override
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData oneDaysSensorData,final TimelineLog log,final long accountId,final boolean feedbackChanged) {

        try {
            final double [][] x = getSensorData(oneDaysSensorData);


            final Optional<NeuralNetAlgorithmOutput> outputOptional = endpoint.getNetOutput(neuralNetServiceConfig.getSleepNetId(),x);

            if (!outputOptional.isPresent()) {
                return Optional.absent();
            }

            final NeuralNetAlgorithmOutput algorithmOutput = outputOptional.get();

            final List<Sample> light = oneDaysSensorData.allSensorSampleList.get(Sensor.LIGHT);

            if (light.isEmpty()) {
                return Optional.absent();
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

            final HmmDecodedResult res = hmm.decode(sleepProbsWithDeltaProb,new Integer[]{obsModels.length - 1},1e-320);


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

                    events.add(Event.createFromType(Event.Type.SLEEP,
                            eventTime,
                            eventTime+DateTimeConstants.MILLIS_PER_MINUTE,
                            light.get(i).offsetMillis,
                            Optional.of(English.FALL_ASLEEP_MESSAGE),
                            Optional.<SleepSegment.SoundInfo>absent(),
                            Optional.<Integer>absent()));

                    foundSleep = true;

                }
                else if (!state.equals(3) && prevState.equals(3)) {
                    LOGGER.info("WAKE at idx={}, p={}",i,sleepMeas[0][i]);

                    events.add(Event.createFromType(Event.Type.WAKE_UP,
                            eventTime,
                            eventTime+DateTimeConstants.MILLIS_PER_MINUTE,
                            light.get(i).offsetMillis,
                            Optional.of(English.WAKE_UP_MESSAGE),
                            Optional.<SleepSegment.SoundInfo>absent(),
                            Optional.<Integer>absent()));
                }


                prevState = state;

            }

            return Optional.of(new TimelineAlgorithmResult(AlgorithmType.NEURAL_NET,events));

        } catch (Exception e) {
            e.printStackTrace();
        }





        return Optional.absent();
    }

    @Override
    public TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid) {
        return new NeuralNetAlgorithm(neuralNetServiceConfig);
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
