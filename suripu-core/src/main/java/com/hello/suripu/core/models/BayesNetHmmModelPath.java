package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableList;

/**
 * Created by benjo on 7/5/15.
 */
public class BayesNetHmmModelPath {
    public final ImmutableList<Integer> path;

    public final String modelId;

    public final String outputId;


    public BayesNetHmmModelPath(ImmutableList<Integer> path, String modelId, String outputId) {
        this.path = path;
        this.modelId = modelId;
        this.outputId = outputId;
    }

}
