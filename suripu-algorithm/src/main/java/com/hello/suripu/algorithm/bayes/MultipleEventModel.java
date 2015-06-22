package com.hello.suripu.algorithm.bayes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableListIterator;

import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 6/16/15.
 */
public class MultipleEventModel {
    final List<Double> discreteProbabilties;
    final Map<String,List<ModelWithDiscreteProbabiltiesAndEventOccurence>> models;

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

    public void addModel(final String id, List<ModelWithDiscreteProbabiltiesAndEventOccurence> models) {
        this.models.put(id,models);
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
            final List<ModelWithDiscreteProbabiltiesAndEventOccurence> condProbModels = models.get(key);

            final List<Integer> events = eventsByModel.get(key);

            if (events == null) {
                //should never happen
                continue;
            }

            //get event
            final Integer event = events.get(t);

            //get conditional probability model of event
            final ModelWithDiscreteProbabiltiesAndEventOccurence condProbModel = condProbModels.get(event);

            p1 = condProbModel.inferProbabilitiesGivenModel(p1);

        }

        return p1;
    }

    /* get list of probabilties as they are sequentially updated with the events  */
    public List<List<Double>> getProbsFromEventSequence(final Map<String,ImmutableList<Integer>> eventsByModel, final int numEvents, boolean forwards) {
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
    public List<List<Double>> getJointOfForwardsAndBackwards(final Map<String,ImmutableList<Integer>> eventsByModel, final int numEvents) {


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
            for (final List<ModelWithDiscreteProbabiltiesAndEventOccurence> myModels : models.values()) {

                //get the model for this event happening
                final ModelWithDiscreteProbabiltiesAndEventOccurence myModel = myModels.get(event);

                //updates state of model
                myModel.inferModelGivenObservedProbabilities(label);
            }
        }
    }
}
