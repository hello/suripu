package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created by benjo on 6/15/15.
 * Entirely mutable, sorry Tim.

 An example -- this class contains the priors ai,bi, and P(sj)

 aj,bj      P(sj)  j = 1,2  (true,false)
 |            /
 |           /
 joint pdf_j (2x1)
 |
 |
 Event


 Joint PDF is beta distribution

 beta(a,b,x) = GAMMA(a+b) / (GAMMA(a)*GAMMA(b)) * x^(a-1) * (1-x)^(b-1)

 E[beta(a,b,x)] = a / (a + b)

 Inference?

 Given that the event happened.... we can infer the following things:

 -------------
 1) You can update the P(sj) for j = 1,2,3 ... N, given a, b

 continuous-discrete Bayes update equation:
 P(s1 | event) = likelihood(event | s1) * p(s1) / p(event)

 and

 likelihood(event | s1) =  E[beta(aj,bj,x)] = aj / (aj + bj)

 and

 marginalize p(sj) to get likelihood of data
 p(event) = likelihood(event | s1) * p(s1) + likelihood(event | s2) * p(s2) + ....


 -----------
 2) You can update aj, bj given P(sj) is observed (granted you could just use the prior of P(sj) too, but we are using labeled data so P(sj) is known)

  for the beta distribution, you can think of the alphas ("a") are the number of successes,
  and the betas are the number of fails, so you have to interpret the event happening as a success or failure.

  Interpretation of success and failure depends on the labels P(sj)

  for example:  j = [1,2] ---> P(s) = [1.0, 0.0]

  this is 100% success for state 1, and 0% success for state 2
  so a1 += 1.0, and b2 += 1.0

  another example: j = [1,2,3] --->  P(s) = [0.6, 0.3, 0.1]

  a1 += 0.6, b1 += 0.4
  a2 += 0.3, b2 += 0.7
  a3 += 0.1, b2 += 0.9






 */
public class BetaDiscreteWithEventOutput implements  ContinuousDiscreteWithEventOutput{

    private final List<Double> discreteProbabilities;
    private final List<BetaDistribution> continuousDistributionProbabilities;

    public BetaDiscreteWithEventOutput(final List<BetaDistribution> continuousDistributionPriors, final List<Double> discreteProbabilitiesPriors) {
        discreteProbabilities = discreteProbabilitiesPriors;
        continuousDistributionProbabilities = continuousDistributionPriors;
    }

    @Override
    public void setDiscretePriors(final List<Double> discreteProbabilities) {
        this.discreteProbabilities.clear();
        this.discreteProbabilities.addAll(discreteProbabilities);
    }

    @Override
    public List<Double> getDiscreteProbabilities() {
        return discreteProbabilities;
    }

    @Override
    public void inferContinuousProbabiltiesAsssumingGivenEventHappened(final List<Double> discreteProbabilities) {
        for (int iState = 0; iState < discreteProbabilities.size(); iState++) {
            final Double probSuccess = discreteProbabilities.get(iState);
            continuousDistributionProbabilities.get(iState).updateWithInference(probSuccess);
        }
    }

    @Override
    public void inferDiscreteProbabilitiesGivenContinuousPriorAndEventHapeend() {

        
    }
}
