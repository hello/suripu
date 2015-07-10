package com.hello.suripu.algorithm.hmm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by benjo on 2/24/15.
 */
public class DiscreteAlphabetPdf implements HmmPdfInterface {

    @JsonCreator
    public DiscreteAlphabetPdf(final List<Double> alphabetProbabilties,final int obsNum) {
        this.obsNum = obsNum;
        this.probs = new double[alphabetProbabilties.size()];

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

            result[i] = Math.log(this.probs[idx]);
        }

        return result;
    }

    @JsonProperty
    final int obsNum;
    final double [] probs;

    @Override
    public int getNumFreeParams() {
        return probs.length;
    }
}
