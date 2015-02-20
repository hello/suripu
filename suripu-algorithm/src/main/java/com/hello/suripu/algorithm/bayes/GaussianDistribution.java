package com.hello.suripu.algorithm.bayes;

/**
 * Created by benjo on 2/14/15.
 */
public class GaussianDistribution {
    public enum DistributionModel {
        RANDOM_MEAN,
        RANDOM_VARIANCE,
        RANDOM_MEAN_AND_VARIANCE;
    }

    public GaussianDistribution(final double mean,final double sigma) {
        this.mean = mean;
        this.sigma = sigma;

        this.alpha = 0.0;
        this.beta = 0.0;
        this.kappa = 0.0;
        this.modelType = DistributionModel.RANDOM_MEAN;
    }

    public GaussianDistribution(final double mean, final double sigma, final double alpha, final double beta,final double kappa,final DistributionModel modelType) {
        this.mean = mean;
        this.sigma = sigma;
        this.alpha = alpha;
        this.beta = beta;
        this.kappa = kappa;
        this.modelType = modelType;
    }

    public double sigma;
    public double mean;
    public double alpha;
    public double beta;
    public double kappa;
    public DistributionModel modelType;
}
