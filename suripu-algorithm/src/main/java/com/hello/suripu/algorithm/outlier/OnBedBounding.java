package com.hello.suripu.algorithm.outlier;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelFactory;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModelInterface;
import com.hello.suripu.algorithm.hmm.HmmDecodedResult;
import com.hello.suripu.algorithm.hmm.HmmPdfInterface;
import com.hello.suripu.algorithm.hmm.PdfCompositeBuilder;
import com.hello.suripu.algorithm.hmm.PoissonPdf;
import com.hello.suripu.algorithm.interpretation.IdxPair;

import java.util.Iterator;

/**
 * Created by benjo on 5/12/16.
 */
public class OnBedBounding {
    final static HmmPdfInterface[] motionModels = {new PoissonPdf(0.01,0),new PoissonPdf(0.1,0),new PoissonPdf(1.0,0),new PoissonPdf(5.0,0)};
    final static double MIN_HMM_PDF_EVAL = 1e-320;

    private static boolean isOnBed(final Integer current) {
        return current.intValue() >= 2 && current.intValue() <= 4;
    }


    public static void brightenRoomIfHmmModelWorkedOkay(final double [] light,final double [] diffLight,final double [] motionDuration, final double maxLightValue,final double diffLightThreshold) {
        //a little bit of input filtering
        final Optional<IdxPair> bedIndicesOptional = OnBedBounding.getIndicesOnBedBounds(motionDuration);

        //IF NO LIGHTS OUT HAPPENED, RELY ON SIMPLE HMM MOTION MODEL TO DETERMINE WHEN THE USER GOT INTO BED/ FELL ASLEEP
        FILTER_PRE_BED:
        if (bedIndicesOptional.isPresent()) {
            final int i1 = bedIndicesOptional.get().i1;
            final int i2 = bedIndicesOptional.get().i2;

            if (i2 - i1 < 300) {
                break FILTER_PRE_BED; //do nothing because on-bed duration is waay to short and probably wrong
            }

            int iBegin = i1 - 120 < 0 ? 0 : i1 - 120;
            for (int t = iBegin; t < i1; t++) {
                //search for lights out
                if (diffLight[t] < -Math.abs(diffLightThreshold)) {
                    break FILTER_PRE_BED; //do nothing if we found a lights out
                }
            }

            //make it bright, this is how we haxor the neural net
            for (int t = 0; t < i1; t++) {
                light[t] = maxLightValue;
            }
        }

    }

    //assumes each bin is a minute
    public static Optional<IdxPair> getIndicesOnBedBounds(final double [] onDurationSeconds) {

        //general idea is to ignore "blips" separated by ~2 - 3 hours
        //and then flip to "on bed" mode when there is sufficient motion

        //progression is "no motion" (long duration) ---> "a lot of motion" (short duration) --> "very little motion (medium duration) ---> "a lot of motion" ---> no motion (long duration)
        //with some backtracking

        final HmmPdfInterface[] obsModels = {motionModels[0], motionModels[2], motionModels[3], motionModels[2], motionModels[1], motionModels[0]};

        final double[][] A = new double[obsModels.length][obsModels.length];

        A[0][0] = 0.98; A[0][1] = 0.01; A[0][2] = 0.01;
        A[1][0] = 0.99; A[1][1] = 0.01;
                                        A[2][2] = 0.59;  A[2][3] = 0.20;  A[2][4] = 0.20; A[2][5] = 0.01;
                                                         A[3][3] = 0.80;  A[3][4] = 0.20;
                                        A[4][2] = 0.05;  A[4][3] = 0.05;  A[4][4] = 0.90;
                                                                                          A[5][5] = 1.0;



        final double[] pi = new double[obsModels.length];
        pi[0] = 9.0;
        pi[1] = 0.1;

        //segment this shit
        final HiddenMarkovModelInterface hmm = HiddenMarkovModelFactory.create(HiddenMarkovModelFactory.HmmType.LOGMATH, obsModels.length, A, pi, obsModels, 0);

        final HmmDecodedResult res = hmm.decode(new double[][]{onDurationSeconds}, new Integer[]{obsModels.length - 1}, MIN_HMM_PDF_EVAL);

        int iOnBed = -1;
        int iOffBed = -1;
        boolean onBed = false;
        for (int i = 0; i < res.bestPath.size(); i++) {
            final Integer current = res.bestPath.get(i);

            if (isOnBed(current) && !onBed) {
                iOnBed = i;
                onBed = true;
            }

            if (!isOnBed(current) && onBed) {
                iOffBed = i-1;
                onBed = false;
            }
        }

        if (iOnBed == -1 || iOffBed == -1 || (iOffBed < iOnBed)) {
            return Optional.absent();
        }

        return Optional.of(new IdxPair(iOnBed,iOffBed));

    }

}
