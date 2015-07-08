package com.hello.suripu.algorithm.partner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.algorithm.bayes.BetaDiscreteWithEventOutput;
import com.hello.suripu.algorithm.bayes.BetaBinomialBayesModel;
import com.hello.suripu.algorithm.bayes.ModelWithDiscreteProbabiltiesAndEventOccurence;
import com.hello.suripu.algorithm.bayes.MultipleEventModel;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/16/15.
 */
public class PartnerBayesNetWithHmmInterpreter {

    private final MultipleEventModel bayesModel;
    private final PartnerHmm hmm;
    public PartnerBayesNetWithHmmInterpreter() {
        hmm = new PartnerHmm();

        final List<Double> conditionalProbs = hmm.getConditionalProbabiltiesOfMeBeingInBed();

        final List<ModelWithDiscreteProbabiltiesAndEventOccurence> models = Lists.newArrayList();

        for (final Double prob : conditionalProbs) {
            BetaDiscreteWithEventOutput betaBayesElement = new BetaDiscreteWithEventOutput(
                    BetaBinomialBayesModel.createBinaryComplementaryBetaDistributions(prob, 1));

            models.add(betaBayesElement);
        }


        bayesModel = new MultipleEventModel(2);

        bayesModel.putModel("partner_filter", models);
    }


    public List<Double> interpretDurationDiff(ImmutableList<Double> durationDiff) {
        final ImmutableList<Integer> path = hmm.decodeSensorData(durationDiff);

        final Map<String,ImmutableList<Integer>> eventsByModel = Maps.newHashMap();
        eventsByModel.put("partner_filter",path);

        final List<List<Double>> jointProbs = bayesModel.getJointOfForwardsAndBackwards(eventsByModel,path.size());

        //class 0 is my motion.....
        //class 1 means your motion
        final List<Double> probOfMeInBed = Lists.newArrayList();

        for (final List<Double> joint : jointProbs) {
            final double notJointNot = 1.0 - joint.get(1); //   !P(!a1,!a2) not joint of false forwards and false backwards  i.e. prob of forwards OR prob of backwards
            probOfMeInBed.add(notJointNot);
        }

        return probOfMeInBed;
    }


}
