package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.hmm.Transition;

import java.util.Arrays;
import java.util.List;

/**
 * Created by benjo on 8/20/15.
 *
 * the result you get from evaluating one of the MultiObsHmms
 */
public class MultiEvalHmmDecodedResult {

    public final int [] path;
    public final int [] stateDurations;
    public final double pathcost;
    public final String originatingModel;
    public final List<Transition> transitions;
    public final int numStates;

    public MultiEvalHmmDecodedResult(int[] path, double pathcost, String originatingModel) {
        this.path = path;
        this.pathcost = pathcost;
        this.originatingModel = originatingModel;
        this.transitions = Lists.newArrayList();

        int maxPath = 0;
        for (int t = 0; t < path.length; t++) {
            if (path[t] > maxPath) {
                maxPath = path[t];
            }
        }
        numStates = maxPath + 1;
        stateDurations = new int[numStates];
        Arrays.fill(stateDurations,0);
        for (int t = 0; t < path.length - 1; t++) {
            stateDurations[path[t]]++;
            if (path[t] != path[t+1]) {
                transitions.add(new Transition(path[t],path[t+1],t));
                if (path[t + 1] > maxPath) {
                    maxPath = path[t+1];
            }
        }
        }

        stateDurations[path[path.length - 1]]++;
    }
}
