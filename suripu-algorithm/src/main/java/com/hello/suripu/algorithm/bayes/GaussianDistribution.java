package com.hello.suripu.algorithm.bayes;

/**
 * Created by benjo on 2/14/15.
 */
public class GaussianDistribution {
    enum DistributionModel {
        RANDOM_MEAN,
        RANDOM_VARIANCE,
        RANDOM_MEAN_AND_VARIANCE
    }

    public GaussianDistribution(final double mean, final double sigma, final double alpha, final double beta,final DistributionModel model_type) {
        this.mean = mean;
        this.sigma = sigma;
        this.alpha = alpha;
        this.beta = beta;
        this.model_type = model_type;
    }

    public final double sigma;
    public final double mean;
    public final double alpha;
    public final double beta;
    public final DistributionModel model_type;
}
