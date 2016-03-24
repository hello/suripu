package com.hello.suripu.algorithm.interpretation;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.hmm.BetaPdf;
import com.hello.suripu.algorithm.hmm.GaussianPdf;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelFactory;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelInterface;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfCompositeBuilder;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by benjo on 3/24/16.
 */
public class SleepProbabilityInterpreter {

    final static Logger LOGGER = LoggerFactory.getLogger(SleepProbabilityInterpreter.class);

    final static HmmPdfInterface[] obsModelsMain = {new BetaPdf(2.0,10.0,0),new BetaPdf(6.0,6.0,0),new BetaPdf(10.0,2.0,0)};
    final static HmmPdfInterface[] obsModelsDiff = {new GaussianPdf(-0.02,0.02,1),new GaussianPdf(0.00,0.02,1),new GaussianPdf(0.02,0.02,1)};

    final static double MIN_HMM_PDF_EVAL = 1e-320;
    final static int MAX_ON_BED_SEARCH_WINDOW = 30; //minutes
    final static int MAX_OFF_BED_SEARCH_WINDOW = 30; //minutes

    final static int DEFAULT_SPACING_OF_OUT_OF_BED_AFTER_WAKE = 1;
    final static int DEFAULT_SPACING_OF_IN_BED_BEFORE_SLEEP = 5;

    final static double POISSON_MEAN_FOR_MOTION = 3.0;
    final static double POISSON_MEAN_FOR_NO_MOTION = 0.1;

    final static int MIN_SLEEP_DURATION = 60; //insanity check

    public static class EventIndices {
        public final int iInBed;
        public final int iSleep;
        public final int iWake;
        public final int iOutOfBed;

        public EventIndices(int iInBed, int iSleep, int iWake, int iOutOfBed) {
            this.iInBed = iInBed;
            this.iSleep = iSleep;
            this.iWake = iWake;
            this.iOutOfBed = iOutOfBed;
        }
    }

    public static Optional<EventIndices> getEventIndices(final double [] sleepProbabilities, final double [] myMotionDurations) {

        int iSleep = -1;
        int iWake = -1;
        int iInBed = -1;
        int iOutOfBed = -1;

        final double [] sleep = sleepProbabilities.clone();
        final double [][] sleepMeas = {sleep};

        final double [] dsleep = new double[sleep.length];

        for (int i = 1; i < dsleep.length; i++) {
            dsleep[i] = sleep[i] - sleep[i-1];
        }

        final double [][] sleepProbsWithDeltaProb = {sleep,dsleep};

        {
            //iterate through all possible combinations
            final HmmPdfInterface s0 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[0]).withPdf(obsModelsDiff[1]).build();
            final HmmPdfInterface s1 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[2]).build();
            final HmmPdfInterface s2 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[1]).build();
            final HmmPdfInterface s3 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[2]).withPdf(obsModelsDiff[1]).build();
            final HmmPdfInterface s4 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[0]).build();
            final HmmPdfInterface s5 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[1]).withPdf(obsModelsDiff[1]).build();
            final HmmPdfInterface s6 = PdfCompositeBuilder.newBuilder().withPdf(obsModelsMain[0]).withPdf(obsModelsDiff[1]).build();


            final HmmPdfInterface[] obsModels = {s0, s1, s2, s3, s4, s5, s6};

            final double[][] A = new double[obsModels.length][obsModels.length];
            A[0][0] = 0.99; A[0][1] = 0.01;
            A[1][1] = 0.98; A[1][2] = 0.01; A[1][3] = 0.01;
            A[2][2] = 0.98; A[2][3] = 0.01; A[2][4] = 0.01;
            A[3][3] = 0.99; A[3][2] = 0.001;                A[3][4] = 0.01;
            A[4][4] = 0.99; A[4][5] = 0.01;                                A[4][6] = 0.01;
            A[5][5] = 0.99; A[5][4] = 0.01;
            A[6][6] = 1.0;



            final double[] pi = new double[obsModels.length];
            pi[0] = 1.0;

            //segment this shit
            final HiddenMarkovModelInterface hmm = HiddenMarkovModelFactory.create(HiddenMarkovModelFactory.HmmType.LOGMATH, obsModels.length, A, pi, obsModels, 0);

            final HmmDecodedResult res = hmm.decode(sleepProbsWithDeltaProb, new Integer[]{obsModels.length - 1}, MIN_HMM_PDF_EVAL);


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



            boolean foundSleep = false;
            Integer prevState = res.bestPath.get(0);
            for (int i = 1; i < res.bestPath.size(); i++) {
                final Integer state = res.bestPath.get(i);

                if (!state.equals(prevState)) {
                    LOGGER.info("FROM {} ---> {} at {}", prevState, state, i);
                }

                if (state.equals(3) && !prevState.equals(3) && !foundSleep) {
                    foundSleep = true;
                    iSleep = i;
                } else if (!state.equals(3) && prevState.equals(3)) {
                    iWake = i;
                }

                prevState = state;

            }

            if (iSleep == -1 || iWake == -1) {
                return Optional.absent();
            }

            if (iWake - iSleep < MIN_SLEEP_DURATION) {
                return Optional.absent();
            }

            iInBed = iSleep - DEFAULT_SPACING_OF_IN_BED_BEFORE_SLEEP;
            iOutOfBed = iWake + DEFAULT_SPACING_OF_OUT_OF_BED_AFTER_WAKE;
        }


        {
            final double[][] x = {myMotionDurations};
            final double[][] A = {{0.99, 0.01}, {0.001, 0.999}};
            final double[] pi = {0.5, 0.5};
            final HmmPdfInterface[] obsModels = {new PoissonPdf(POISSON_MEAN_FOR_NO_MOTION, 0), new PoissonPdf(POISSON_MEAN_FOR_MOTION, 0)};

            final HiddenMarkovModelInterface hmm = HiddenMarkovModelFactory.create(HiddenMarkovModelFactory.HmmType.LOGMATH, 2, A, pi, obsModels, 0);


            final HmmDecodedResult result = hmm.decode(x, new Integer[]{0, 1}, MIN_HMM_PDF_EVAL);


            boolean foundCluster = false;

            //go backwards from sleep and find beginning of next motion cluster encountered
            for (int i = iSleep; i >= 0; i--) {
                final Integer state = result.bestPath.get(i);

                if (state.equals(1)) {
                    //if motion cluster start was found too far before sleep, then stop search and use default
                    if (iSleep - i > MAX_ON_BED_SEARCH_WINDOW) {
                        LOGGER.warn("action=return_default_in_bed");
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
            for (int i = iWake; i < myMotionDurations.length; i++) {
                final Integer state = result.bestPath.get(i);

                if (state.equals(1)) {
                    //if motion cluster start was found too far after wake, then stop search and use default
                    if (i - iWake > MAX_OFF_BED_SEARCH_WINDOW) {
                        LOGGER.warn("action=return_default_out_of_bed");
                        break;
                    }
                    foundCluster = true;
                }

                if (state.equals(0) && foundCluster) {
                    iOutOfBed = i;
                    break;
                }
            }
        }

        LOGGER.info("IN_BED at idx={}, psleep={}",iInBed,sleep[iInBed]);
        LOGGER.info("SLEEP at idx={}, psleep={}",iSleep,sleep[iSleep]);
        LOGGER.info("WAKE_UP at idx={}, psleep={}",iWake,sleep[iWake]);
        LOGGER.info("OUT_OF_BED at idx={}, psleep={}",iOutOfBed,sleep[iOutOfBed]);

        return Optional.of(new EventIndices(iInBed,iSleep,iWake,iOutOfBed));

    }




}
