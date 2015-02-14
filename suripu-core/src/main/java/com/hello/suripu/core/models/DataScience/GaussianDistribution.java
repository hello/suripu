package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by benjo on 2/13/15.
 *
 *  A prior or posterior Gaussian distribution
 *
 *  characterized by 5 parameters: mu, sigma^2, alpha, beta, and an enum which says which
 */
public class GaussianDistribution {

    public GaussianDistribution(final double mean, final double variance, final double alpha, final double beta,final RandomVariableSelection rand_type) {
        this.mean = mean;
        this.variance = variance;
        this.alpha = alpha;
        this.beta = beta;
        this.rand_type = rand_type;
    }

    public enum RandomVariableSelection {
        RANDOM_MEAN,
        RANDOM_VARIANCE,
        RANDOM_MEAN_AND_VARIANCE
    }


    @JsonProperty("mean")
    public final double mean;

    @JsonProperty("variance")
    public final double variance;

    @JsonProperty("alpha")
    public final double alpha;

    @JsonProperty("beta")
    public final double beta;

    @JsonProperty("rand_type")
    public final RandomVariableSelection rand_type;

}
