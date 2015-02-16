package com.hello.suripu.algorithm.bayes;

/**
 * Created by benjo on 2/14/15.
 */

public class GaussianInference {
    static public final double k_minimum_variance = 1e-9;
    static public final double k_minimum_conj_prior_variance = 1e-6;

    /* return posterior */
    static public GaussianDistribution GetInferredDistribution(final GaussianDistribution prior, final double x, final double conjugate_prior_sigma) {
        GaussianDistribution posterior = prior;

        switch (prior.model_type) {

            case RANDOM_MEAN:
                double variance = prior.sigma*prior.sigma;
                double conjugate_prior_variance = conjugate_prior_sigma * conjugate_prior_sigma;

                if (variance < k_minimum_variance) {
                    variance = k_minimum_variance;
                }

                if (conjugate_prior_variance < k_minimum_conj_prior_variance) {
                    conjugate_prior_variance = k_minimum_conj_prior_variance;
                }

                final double total_variance = conjugate_prior_variance + variance;
                final double w1 = variance / total_variance;
                final double w2 = conjugate_prior_variance / total_variance;
                final double conj_prior_information = 1.0 / conjugate_prior_variance;
                final double prior_information = 1.0 / variance;

                final double new_mean  = w1 * x + w2 * prior.mean;
                final double new_variance = 1.0 / (conj_prior_information + prior_information);

                posterior = new GaussianDistribution(new_mean,Math.sqrt(new_variance),prior.alpha,prior.beta,prior.model_type);

                break;
            case RANDOM_VARIANCE:
                /* not implemented yet! */
                break;
            case RANDOM_MEAN_AND_VARIANCE:
                /* not implemented yet! */
                break;


        }


        return posterior;


    }
}
