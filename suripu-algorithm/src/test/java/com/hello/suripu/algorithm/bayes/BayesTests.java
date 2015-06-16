package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.junit.Test;

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

        BetaDiscreteWithEventOutput bayesElement = new BetaDiscreteWithEventOutput(betaDistributions);

        final List<Double> p1 = Lists.newArrayList();
        p1.add(0.6);
        p1.add(0.4);
        final ImmutableList<Double> prior1 = ImmutableList.copyOf(p1);

        final ImmutableList<Double> posterior1 = bayesElement.inferProbabilitiesGivenModel(prior1);


        TestCase.assertEquals(posterior1.get(0),0.85714286,1e-6);
        TestCase.assertEquals(posterior1.get(1),0.14285714,1e-6);

    }
}
