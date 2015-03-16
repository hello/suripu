package com.hello.suripu.algorithm.hmm;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by benjo on 3/16/15.
 */
public class MultipleHiddenMarkovModels  {
    private final List<HiddenMarkovModel> models;

    public MultipleHiddenMarkovModels() {
        models =  new ArrayList<HiddenMarkovModel>();
    }

    public void addModel(final HiddenMarkovModel hmm) {
        models.add(hmm);
    }

    boolean isValid() {
        return !models.isEmpty();
    }

    public Optional<HmmDecodedResult> getBestPathOfModels(double[][] observations, Integer[] possibleEndStates) {
        final ArrayList<HmmDecodedResult> results = new ArrayList<>();

        for (final HiddenMarkovModel hmm : models) {
            results.add(hmm.decode(observations, possibleEndStates));
        }

        //compare results via BIC.  Find the minimum BIC value.  That means lowest path cost + model complexity
        double minbic = Float.MAX_VALUE;
        HmmDecodedResult bestResult = null;
        for (final HmmDecodedResult r : results) {
            if (r.bic < minbic) {
                minbic = r.bic;
                bestResult = r;
            }
        }

        if (bestResult == null) {
            return Optional.absent();
        }


        //could return null if no hmms were added
        return Optional.of(bestResult);

    }
}
