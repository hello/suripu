package com.hello.suripu.algorithm.hmm;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 3/16/15.
 */
public class MultipleHiddenMarkovModels  {
    private final Map<String,HiddenMarkovModel> models;

    public MultipleHiddenMarkovModels() {
        models =  new HashMap<>();
    }

    public void addModel(final String modelName,final HiddenMarkovModel hmm) {
        models.put(modelName,hmm);
    }

    boolean isValid() {
        return !models.isEmpty();
    }

    static public class BestModel {
        public final String model;
        public final HmmDecodedResult result;


        public BestModel(final String model, final HmmDecodedResult result) {
            this.model = model;
            this.result = result;
        }
    }

    public Optional<BestModel> getBestPathOfModels(double[][] observations, Integer[] possibleEndStates) {
        final ArrayList<HmmDecodedResult> results = new ArrayList<>();

        final Map<String,HmmDecodedResult> resultMap = new HashMap<>();

        //get results and put them into the results map
        for (final String key : models.keySet()) {
            resultMap.put(key, models.get(key).decode(observations, possibleEndStates));
        }


        //compare results via BIC.  Find the minimum BIC value.  That means lowest path cost + model complexity
        double minbic = Float.MAX_VALUE;
        HmmDecodedResult bestResult = null;
        String bestkey = null;
        for (final String key : resultMap.keySet()) {
            final HmmDecodedResult r = resultMap.get(key);



            if (r.bic < minbic) {
                minbic = r.bic;
                bestResult = r;
                bestkey  = key;
            }
        }

        if (bestResult == null) {
            return Optional.absent();
        }

        BestModel theBest = new BestModel(bestkey,bestResult);

        //could return null if no hmms were added
        return Optional.of(theBest);

    }
}
