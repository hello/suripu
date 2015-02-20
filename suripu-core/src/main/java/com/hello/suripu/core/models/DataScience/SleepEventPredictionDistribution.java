package com.hello.suripu.core.models.DataScience;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.algorithm.bayes.GaussianDistribution;
import org.joda.time.DateTime;

/**
 * Created by benjo on 2/13/15.
 */
public class SleepEventPredictionDistribution {

    public SleepEventPredictionDistribution(final GaussianPriorPosteriorPair biasDistributions,
                                            final GaussianPriorPosteriorPair eventTimeDistributions) {


        this.biasDistributions = biasDistributions;
        this.eventTimeDistributions = eventTimeDistributions;
    }

   public static final double BIAS_PREDICTION_MEAN = 0.0; //hours
   public static final double BIAS_PREDICTION_SIGMA = 2.0; //hours

   //gamma distribution
   // mean = alpha/beta, variance = alpha / beta^2
   //so the bigger the beta, the peakier the distribution
   //
   public static final double SLEEP_EVENT_ALPHA = 0.2;
   public static final double SLEEP_EVENT_BETA = 0.2;
   public static final double SLEEP_EVENT_KAPPA = 0.0;

   public static final SleepEventPredictionDistribution getDefault(final double eventTimeInHoursOfTheDay) {

       GaussianPriorPosteriorPair biasPair = new GaussianPriorPosteriorPair(
               new GaussianDistributionDataModel(
                       new GaussianDistribution(BIAS_PREDICTION_MEAN,BIAS_PREDICTION_SIGMA,0.0,0.0,0.0,
                               GaussianDistribution.DistributionModel.RANDOM_MEAN)),
               new GaussianDistributionDataModel(
                       new GaussianDistribution(BIAS_PREDICTION_MEAN,BIAS_PREDICTION_SIGMA,0.0,0.0,0.0,
                            GaussianDistribution.DistributionModel.RANDOM_MEAN))
               );


       GaussianPriorPosteriorPair eventTimePair = new GaussianPriorPosteriorPair(
               new GaussianDistributionDataModel(
                    new GaussianDistribution(eventTimeInHoursOfTheDay,0.0,SLEEP_EVENT_ALPHA,SLEEP_EVENT_BETA,SLEEP_EVENT_KAPPA,
                       GaussianDistribution.DistributionModel.RANDOM_MEAN_AND_VARIANCE)),
               new GaussianDistributionDataModel(
                       new GaussianDistribution(eventTimeInHoursOfTheDay,0.0,SLEEP_EVENT_ALPHA,SLEEP_EVENT_BETA,SLEEP_EVENT_KAPPA,
                               GaussianDistribution.DistributionModel.RANDOM_MEAN_AND_VARIANCE)));



       return new SleepEventPredictionDistribution(biasPair,eventTimePair);
    }

    public final SleepEventPredictionDistribution getCopy() {
        return new SleepEventPredictionDistribution(
                this.biasDistributions.getCopy(),
                this.eventTimeDistributions.getCopy());

    }

    public final SleepEventPredictionDistribution getCopyWithPosteriorAsPrior() {
        return new SleepEventPredictionDistribution(
                this.biasDistributions.getCopyWithPosteriorAsPrior(),
                this.eventTimeDistributions.getCopyWithPosteriorAsPrior());

    }





    @JsonProperty("prediction_bias_distributions")
    public final GaussianPriorPosteriorPair biasDistributions;

    @JsonProperty("event_time_distributions")
    public final GaussianPriorPosteriorPair eventTimeDistributions;

}
