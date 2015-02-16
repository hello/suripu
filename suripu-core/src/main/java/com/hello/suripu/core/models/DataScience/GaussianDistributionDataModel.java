package com.hello.suripu.core.models.DataScience;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.algorithm.bayes.GaussianDistribution;
/**
 * Created by benjo on 2/13/15.
 *
 *  A prior or posterior Gaussian distribution
 *
 *  characterized by 5 parameters: mu, sigma, alpha, beta, and an enum which says which
 */
public class GaussianDistributionDataModel {

    public GaussianDistributionDataModel(final GaussianDistribution gaussian) {
        this.mean = gaussian.mean;
        this.sigma = gaussian.sigma;
        this.alpha = gaussian.alpha;
        this.beta = gaussian.beta;
        this.model_type = gaussian.model_type.value;
    }

    public GaussianDistribution asGaussian() {
        GaussianDistribution gaussian = new GaussianDistribution(this.mean,this.sigma,this.alpha,this.beta,GaussianDistribution.DistributionModel.from_value(this.model_type));
        return gaussian;
    }



    @JsonProperty("mean")
    public final double mean;

    @JsonProperty("sigma")
    public final double sigma;

    @JsonProperty("alpha")
    public final double alpha;

    @JsonProperty("beta")
    public final double beta;

    @JsonProperty("model_type")
    public final int model_type;

}
