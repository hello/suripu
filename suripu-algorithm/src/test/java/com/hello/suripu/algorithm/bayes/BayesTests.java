package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by benjo on 6/16/15.
 */
public class BayesTests {

    @Test
    public void testBetaDist() {

        final List<BetaDistribution> betaDistributions = BetaDistribution.createBinaryComplementaryBetaDistributions(0.8,10);

        TestCase.assertEquals(betaDistributions.get(0).getExpectation(), 0.8, 1e-6);
        TestCase.assertEquals(betaDistributions.get(1).getExpectation(),0.2,1e-6);


        betaDistributions.get(0).updateWithInference(1.0);
        betaDistributions.get(1).updateWithInference(0.0);

        TestCase.assertEquals(betaDistributions.get(0).getExpectation(),9.0 / 11.0,1e-6);
        TestCase.assertEquals(betaDistributions.get(1).getExpectation(),2.0 / 11.0,1e-6);
    }

    @Test
    public void testBayesRule() {
        final List<BetaDistribution> betaDistributions = BetaDistribution.createBinaryComplementaryBetaDistributions(0.8,10);

        final BetaDiscreteWithEventOutput bayesElement = new BetaDiscreteWithEventOutput(betaDistributions);

        final List<Double> p1 = Lists.newArrayList();
        p1.add(0.6);
        p1.add(0.4);
        final ImmutableList<Double> prior1 = ImmutableList.copyOf(p1);

        final ImmutableList<Double> posterior1 = bayesElement.inferProbabilitiesGivenModel(prior1);


        TestCase.assertEquals(posterior1.get(0),0.85714286,1e-6);
        TestCase.assertEquals(posterior1.get(1),0.14285714,1e-6);

    }

    static List<Double> getFirstElement(List<List<Double>> x) {
        List<Double> ret = Lists.newArrayList();
        for (List<Double> vec :  x) {
            ret.add(vec.get(0));
        }
        return ret;
    }

    @Test
    public void TestSequentialBayes() {
        final List<BetaDistribution> betaDistributions1 = BetaDistribution.createBinaryComplementaryBetaDistributions(0.8,1);
        final List<BetaDistribution> betaDistributions2 = BetaDistribution.createBinaryComplementaryBetaDistributions(0.2,1);
        final List<BetaDistribution> betaDistributions3 = BetaDistribution.createBinaryComplementaryBetaDistributions(0.5,1);

        final BetaDiscreteWithEventOutput bayesElement1 = new BetaDiscreteWithEventOutput(betaDistributions1);
        final BetaDiscreteWithEventOutput bayesElement2 = new BetaDiscreteWithEventOutput(betaDistributions2);
        final BetaDiscreteWithEventOutput bayesElement3 = new BetaDiscreteWithEventOutput(betaDistributions3);

        final List<ModelWithDiscreteProbabiltiesAndEventOccurence> models = Lists.newArrayList();

        models.add(bayesElement1);
        models.add(bayesElement2);
        models.add(bayesElement3);

        final MultipleEventModel multipleEventModel = new MultipleEventModel(models,2);


        multipleEventModel.setPriorForAllStatesBasedOnOneState(0,0.70);

        final Integer [] e1 = {0,0,0,0,0,0,0,0,0,0,0};
        final Integer [] e2 = {1,1,1,1,1,1,1,1,1,1,1};
        final Integer [] e3 = {2,2,2,2,2,2,2,2,2,2,2};
        final Integer [] e4 = {2,2,2,2,2,2,2,2,2,2,2,0,0,0,0,0,0,0,0,0,0,0,2,2,2,2,2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,2,2,2};

        final List<Integer> events1 = Arrays.asList(e1);
        final List<Integer> events2 = Arrays.asList(e2);
        final List<Integer> events3 = Arrays.asList(e3);
        final List<Integer> events4 = Arrays.asList(e4);

        final List<Double> probs1 = getFirstElement(multipleEventModel.getProbsFromEventSequence(ImmutableList.copyOf(events1)));

        final List<Double> probs2 = getFirstElement(multipleEventModel.getProbsFromEventSequence(ImmutableList.copyOf(events2)));

        final List<Double> probs3 = getFirstElement(multipleEventModel.getProbsFromEventSequence(ImmutableList.copyOf(events3)));

        final List<Double> probs4 = getFirstElement(multipleEventModel.getJointOfForwardsAndBackwards(ImmutableList.copyOf(events4)));

        //sanity check only
        TestCase.assertTrue(probs4.get(12) > 0.90);
        TestCase.assertTrue(probs4.get(54) < 0.10);
        TestCase.assertEquals(probs4.get(3),probs4.get(4),0.01);
        TestCase.assertEquals(probs4.get(39),probs4.get(40),0.01);
        TestCase.assertEquals(probs4.get(54),probs4.get(53),0.01);


    }
}
