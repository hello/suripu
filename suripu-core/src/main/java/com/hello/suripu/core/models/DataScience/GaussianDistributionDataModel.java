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
        this.kappa = gaussian.kappa;
        this.modelType = gaussian.modelType;
    }

    public GaussianDistribution asGaussian() {
        return new GaussianDistribution(this.mean,this.sigma,this.alpha,this.beta,this.kappa,this.modelType);
    }



    @JsonProperty("mean")
    public final double mean;

    @JsonProperty("sigma")
    public final double sigma;

    @JsonProperty("alpha")
    public final double alpha;

    @JsonProperty("beta")
    public final double beta;

    @JsonProperty("kappa")
    public final double kappa;

    @JsonProperty("modelType")
    public final GaussianDistribution.DistributionModel modelType;

}
