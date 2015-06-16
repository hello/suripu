package com.hello.suripu.algorithm.bayes;

import java.util.List;

/**
 * Created by benjo on 6/15/15.
 */
public interface ContinuousDiscreteWithEventOutput {
    public void setDiscretePriors(final List<Double> discreteProbabilities);
    public List<Double> getDiscreteProbabilities();

    void inferContinuousProbabiltiesAsssumingGivenEventHappened(final List<Double> discreteProbabilities);
    void inferDiscreteProbabilitiesGivenContinuousPriorAndEventHapeend();

    //TODO serialize and deserialize to JSON or protobuf SOMEHOW

}
