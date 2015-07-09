package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableListIterator;
import com.hello.suripu.algorithm.core.AlgorithmException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by benjo on 6/16/15.
 *
 *  This class maps a state sequence (List 'o integers) into probabilties of class.
 *  P(class | sequence, model), where the model is P (state_i | class)
 *
 *  It will also let you update the model with labeled data.
 *
 *
 * This class is used as follows:
 * 1) create, giving it the number of discrete probabilties you're modeling (i.e. true-false?  then it's 2)
 * 2) putModel - add some (most likely) beta-binomial models.
 *    You can do two things with these:
 *        - label the data (i.e update the model given the label and state sequence, "inferring the model parameters")
 *        - infer the probabilities of the discrete state given the data
 *
 * 3) Then, you will get a bunch of state sequences from a bunch of HMMs, which will be put in a map.
 *   You call this on it:  getProbsFromEventSequence(mapOfStateSequencesWhereIdIsWhichHmmItCameFrom, N)
 *
 *   -= Concrete example =-
 *
 *  I've added this model:
 *  beta-binomial 1 maps to  P(state_1 | sleep) = alpha_1 / (alpha_1 + beta_1)
 *  beta-binomial 2 maps to .... etc. etc.
 *
 *  I've got some sensor data, and I decoded ....
 *
 *  to be completed later....
 *
 */
public class MultipleEventModel {
    private static final Double MINIMUM_PROBABILITY = 5e-2;
    private static final Double MAXIMUM_PROBABILITY = 1.0 - MINIMUM_PROBABILITY;

    private final List<Double> discreteProbabilties;
    private final Map<String,List<BetaDiscreteWithEventOutput>> models;

    public MultipleEventModel(final int numDiscreteProbs) {
        this.models = Maps.newHashMap();
        discreteProbabilties = Lists.newArrayList();

        //default is uniform prior
        for (int iState = 0; iState < numDiscreteProbs; iState++) {
            discreteProbabilties.add(1.0 / (double)numDiscreteProbs);
        }
    }

    void setUniformPrior() {
        for (int iState = 0; iState < discreteProbabilties.size(); iState++) {
            discreteProbabilties.set(iState,1.0 / (double)discreteProbabilties.size());
        }
    }

    public boolean hasModel(final String id) {
        return this.models.keySet().contains(id);
    }

    public void putModel(final String id, List<BetaDiscreteWithEventOutput> models) {
        this.models.put(id,models);
    }

    public Set<String> getModelNames() {
        return this.models.keySet();
    }

    public List<BetaDiscreteWithEventOutput> getModel(final String id) {
        return this.models.get(id);
    }

    void setPriorForAllStatesBasedOnOneState(final int iState, final double prior) {
        final double notPrior = 1.0 - prior;

        if (discreteProbabilties.size() == 1) {
            discreteProbabilties.set(0,prior); //this is stupid, should always be at least size 2
            return;
        }

        final double allExceptSelected = notPrior / (double) (discreteProbabilties.size() - 1);

        for (int i = 0; i < discreteProbabilties.size(); i++) {
            discreteProbabilties.set(i,allExceptSelected);
        }

        discreteProbabilties.set(iState,prior);
    }


    private ImmutableList<Double> getBayesianUpdateAtIndex(final ImmutableList<Double> prior, final int t, final Map<String,ImmutableList<Integer>> eventsByModel) {


        ImmutableList<Double> p1 = prior;

        //process events... go through each matching key from events
        for (final String key : models.keySet()) {
            final List<BetaDiscreteWithEventOutput> condProbModels = models.get(key);

            final List<Integer> events = eventsByModel.get(key);

            if (events == null) {
                //should never happen
                throw new AlgorithmException(String.format("did not find events for model=%s",key));
            }

            //get event
            final Integer event = events.get(t);

            //get conditional probability model of event
            final BetaDiscreteWithEventOutput condProbModel = condProbModels.get(event);

            p1 = condProbModel.inferProbabilitiesGivenModel(p1);
        }

        final List<Double> enforcedPosterior = Lists.newArrayList();

        //enforce max/min probabilities
        for (Double p : p1) {
            if (p > MAXIMUM_PROBABILITY) {
                p = MAXIMUM_PROBABILITY;
            }

            if (p < MINIMUM_PROBABILITY) {
                p = MINIMUM_PROBABILITY;
            }

            enforcedPosterior.add(p);
        }

        p1 = ImmutableList.copyOf(enforcedPosterior);

        return p1;
    }

    /* get list of probabilties as they are sequentially updated with the events  */
    public List<List<Double>> getProbsFromEventSequence(final Map<String,ImmutableList<Integer>> eventsByModel, final int numEvents, boolean forwards)  {
        List<List<Double>> probs = Lists.newArrayList();

        //set prior
        ImmutableList<Double> prior = ImmutableList.copyOf(this.discreteProbabilties);

        if (forwards) {
            for (int t = 0; t < numEvents; t++) {

                final ImmutableList<Double> posterior = getBayesianUpdateAtIndex(prior, t, eventsByModel);

                //save posterior
                probs.add(posterior);

                prior = posterior;

            }
        }
        else {
            for (int t = numEvents - 1; t >= 0; t--) {

                final ImmutableList<Double> posterior = getBayesianUpdateAtIndex(prior, t, eventsByModel);

                //save posterior
                probs.add(posterior);

                prior = posterior;


            }
        }

        return probs;
    }

    /*
     *  get joint probability of forwards and backwards: starting with prior forwards, and starting with prior backwards, and compute the joint of these probabilities
     *  at any given time
     *
     *  i.e.   P(A_forwards) * P(A_backwards) = P(A_forwards, A_backwards)
     *
     *  */
    public List<List<Double>> getJointOfForwardsAndBackwards(final Map<String,ImmutableList<Integer>> eventsByModel, final int numEvents)  {


        //do forwards
        final List<List<Double>> forwardProbs = getProbsFromEventSequence(eventsByModel,numEvents,true);

        //do backwards
        final List<List<Double>> backwardProbs =  getProbsFromEventSequence(eventsByModel,numEvents,false);

        final List<List<Double>> joints = Lists.newArrayList();

        for (int iTimeForward = 0; iTimeForward < forwardProbs.size(); iTimeForward++) {
            final int iTimeBackward = forwardProbs.size() - iTimeForward - 1;

            final List<Double> forwardProb = forwardProbs.get(iTimeForward);
            final List<Double> backwardProb = backwardProbs.get(iTimeBackward);

            final List<Double> joint = Lists.newArrayList();
            for (int iState = 0; iState < forwardProb.size(); iState++) {
                joint.add(forwardProb.get(iState) * backwardProb.get(iState));
            }

            joints.add(joint);

        }

        return joints;
    }

    /* Apply to some subset of events where this label is valid  */
    public void inferModelParametersGivenEventsAndLabel(final ImmutableList<Double> label, final ImmutableList<Integer> events) {
        for (final Integer event : events) {
            if (event < 0 || event >= models.size()) {
                //TODO LOG ERROR
                continue;
            }

            //iterate through each model
            for (final List<BetaDiscreteWithEventOutput> myModels : models.values()) {

                //get the model for this event happening
                final BetaDiscreteWithEventOutput myModel = myModels.get(event);

                //updates state of model
                myModel.inferModelGivenObservedProbabilities(label);
            }
        }
    }
}
