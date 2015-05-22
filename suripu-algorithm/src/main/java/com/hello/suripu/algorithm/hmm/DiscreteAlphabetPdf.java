package com.hello.suripu.algorithm.hmm;



import java.util.List;

/**
 * Created by benjo on 2/24/15.
 */
public class DiscreteAlphabetPdf implements HmmPdfInterface {

    public DiscreteAlphabetPdf(final List<Double> alphabetProbabilties,final int obsNum, final double weight) {
        this.obsNum = obsNum;
        this.probs = new double[alphabetProbabilties.size()];
        this.weight = weight;

        for (int i = 0; i < alphabetProbabilties.size(); i++) {
            this.probs[i] = alphabetProbabilties.get(i);

            if (this.probs[i] < MIN_LIKELIHOOD) {
                this.probs[i] = MIN_LIKELIHOOD;
            }
        }
    }

    @Override
    public double[] getLogLikelihood(double[][] measurements) {
        double [] result = new double[measurements[0].length];
        //row major or column major? assume it's like C
        final double [] col =  measurements[this.obsNum];

        for (int i = 0; i < col.length; i++) {
            //god I hope this is its likelihood function
            int idx = (int)col[i];

            result[i] = weight * Math.log(this.probs[idx]);

            if (Double.isNaN(result[i])) {
                result[i] = Double.NEGATIVE_INFINITY;
            }
        }

        return result;
    }

    final int obsNum;
    final double [] probs;
    final double weight;
}
