package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.bayes.GaussianDistribution;
import com.hello.suripu.algorithm.bayes.GaussianInference;
import com.hello.suripu.core.models.DataScience.GaussianDistributionDataModel;
import com.hello.suripu.core.models.DataScience.GaussianPriorPosteriorPair;
import com.hello.suripu.core.models.DataScience.SleepEventPredictionDistribution;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.Events.WakeupEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by benjo on 2/18/15.
 */
public class BayesUtils {

    //units are in hours
    static final double K_MIN_SIGMA_OF_PREDICTION = 0.25;
    static final double K_MIN_SIGMA_OF_BIAS = 0.05;

    static final double K_PREDICTION_SIGMA = 1.0;
    static final double K_ACCEPTABLE_BIAS_UNCERTAINTY_FLOOR = 0.1;

    static private double getLocalTimeInFloatingPointHoursFromDateTime(final DateTime dateTime) {

        final LocalDateTime local = dateTime.toLocalDateTime();


        final double time_in_hours_local_time =
                ((double)local.getHourOfDay()) +
                        ((double)local.getMinuteOfHour()) / 60.0; //which hour, floating point.

        return time_in_hours_local_time;
    }


    static private double recomputeMeasurementFromMean(final double meas, final double x) {
        final double dx = subHours(x,meas);
        final double h3 = meas + dx; //x - meas + meas = x
        return h3;
    }

    static private double mod24(double h) {
        h = h % 24.0;
        if (h < 0) {
            h += 24.0;
        }

        return h;
    }

    static private double subHours(double h1, double h2) {

        double h3;
        //11 - 13 = -2

        //23 - 1 == -2
       // 23 - 1 > 12 ---> 23 - 25

        //1 - 23 == 2
        //23 - 1 > 12? 25 - 23

        if (h1 - h2 > 12.0) {
            h2 += 24.0;
        }

        if (h2 - h1 > 12.0) {
            h1 += 24.0;
        }


        h3 = h1 - h2;


        return h3;
    }

    static DateTime addBiasInHoursToDateTime(final DateTime time,final double hbias) {
        //compute new event time in absolute terms
        //B - A = C ---> A + C = B
        final long timeDeltaInMillis = (long)(hbias * 3600 * 1000);
        final long newStartTimestamp = time.getMillis() + timeDeltaInMillis;
        DateTime newTime = new DateTime(newStartTimestamp);

        return newTime;
    }



    /*
    * Bayes' magic
    * @param targetDate
    * @param predictions sleep predictions, list of optional events
    * @param alarmTime optional list of events (confusing!)
    * @param wakeFeedbackTime time user said they woke up
    * @return
    */

    static public BayesInferenceResult inferPredictionBiasAndDistributionTimes(
            final GaussianDistributionDataModel priorEventTimeDist,
            final GaussianDistributionDataModel priorBiasDist,
            final Optional<DateTime> prediction,
            final Optional<DateTime> measurement, final double measurementSigma) {


        BayesInferenceResult result = new BayesInferenceResult();

        //if neither prediction nor measurement is present, get out of here
        if (!prediction.isPresent() && !measurement.isPresent()) {
            return result;
        }


        //by default, we assign the priors to the posteriors, in case we don't actually do inference
        GaussianDistributionDataModel posteriorEventTimeDist = priorEventTimeDist;
        GaussianDistributionDataModel posteriorBiasDist = priorBiasDist;

        if (measurement.isPresent()) {

            //estimate the distribution (as a Student's T distribution) of the event
            final double measurementTimeInHours = getLocalTimeInFloatingPointHoursFromDateTime(measurement.get());
            final GaussianDistribution priorPred = priorEventTimeDist.asGaussian();
            final double meas2 = recomputeMeasurementFromMean(priorPred.mean,measurementTimeInHours);


            GaussianDistribution posterior2 = GaussianInference.GetInferredDistribution(priorPred, meas2, measurementSigma, K_MIN_SIGMA_OF_PREDICTION);
            posterior2.mean = mod24(posterior2.mean);

            posteriorEventTimeDist = new GaussianDistributionDataModel(posterior2);



                    //estimate the bias of the prediction, as random mean gaussian
            if (prediction.isPresent()) {
                final double predictionTimeInHours = getLocalTimeInFloatingPointHoursFromDateTime(prediction.get());
                final double biasMeasurementInHours = subHours(measurementTimeInHours, predictionTimeInHours);

                final GaussianDistribution priorBias = priorBiasDist.asGaussian();


                posteriorBiasDist = new GaussianDistributionDataModel(
                        GaussianInference.GetInferredDistribution(priorBias, biasMeasurementInHours, measurementSigma, K_MIN_SIGMA_OF_BIAS));
            }

        } else {
            //if no actual measurement is available... just update the prediction distribution with the prediction.  bleh.
            double predictionTimeInHours = getLocalTimeInFloatingPointHoursFromDateTime(prediction.get());

            //if the bias estimate is of high enough quality, use it
            if (posteriorBiasDist.sigma < K_ACCEPTABLE_BIAS_UNCERTAINTY_FLOOR) {
                predictionTimeInHours += posteriorBiasDist.mean;
            }

            final GaussianDistribution priorPred = priorEventTimeDist.asGaussian();

            final double meas2 = recomputeMeasurementFromMean(priorPred.mean,predictionTimeInHours);


            GaussianDistribution posterior2 = GaussianInference.GetInferredDistribution(priorPred, meas2, K_PREDICTION_SIGMA, K_MIN_SIGMA_OF_PREDICTION);
            posterior2.mean = mod24(posterior2.mean);

            posteriorEventTimeDist = new GaussianDistributionDataModel(posterior2);


        }



        /* If measurement is here, use as estimate.  Otherwise, use prediction with bias */
        DateTime eventTime = null;

        if (measurement.isPresent()) {
            eventTime = measurement.get();
        } else {
            eventTime = prediction.get();
        }

        //if the bias estimate is of high enough quality, use it
        if (posteriorBiasDist.sigma < K_ACCEPTABLE_BIAS_UNCERTAINTY_FLOOR) {
            eventTime = addBiasInHoursToDateTime(eventTime, posteriorBiasDist.mean);
        }

        final GaussianPriorPosteriorPair eventPair = new GaussianPriorPosteriorPair(priorEventTimeDist,posteriorEventTimeDist);
        final GaussianPriorPosteriorPair biasPair = new GaussianPriorPosteriorPair(priorBiasDist,posteriorBiasDist);

        result.eventTime = Optional.of(eventTime);
        result.distributions = Optional.of(new SleepEventPredictionDistribution(biasPair,eventPair));

        return result;

    }


}
