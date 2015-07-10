package com.hello.suripu.core.models;

import java.util.List;

/**
 * Created by benjo on 7/8/15.
 */
public class BayesNetHmmMultipleModelsPriors {

    public final List<BayesNetHmmSingleModelPrior> modelPriorList;

    public final String source;


    public BayesNetHmmMultipleModelsPriors(List<BayesNetHmmSingleModelPrior> modelPriorList, String source) {
        this.modelPriorList = modelPriorList;
        this.source = source;
    }
}
