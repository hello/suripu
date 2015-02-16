package com.hello.suripu.algorithm.bayes;

/**
 * Created by benjo on 2/14/15.
 */
public class GaussianDistribution {
    public enum DistributionModel {
        RANDOM_MEAN (0),
        RANDOM_VARIANCE (1),
        RANDOM_MEAN_AND_VARIANCE (2);

        DistributionModel(int value) {
            this.value = value;
        }

        public final int value;

        public static DistributionModel from_value(int value) {
            switch (value) {
                case 0:
                   return RANDOM_MEAN;

                case 1:
                    return RANDOM_VARIANCE;

                case 2:
                    return RANDOM_MEAN_AND_VARIANCE;

                default:
                    return RANDOM_MEAN;
            }
        }

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
