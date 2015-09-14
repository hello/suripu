package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.Lists;
import com.hello.suripu.algorithm.hmm.Transition;

import java.util.List;

/**
 * Created by benjo on 8/20/15.
 */
public class MultiEvalHmmDecodedResult {

    public final int [] path;
    public final double pathcost;
    public final String originatingModel;
    public final List<Transition> transitions;

    public MultiEvalHmmDecodedResult(int[] path, double pathcost, String originatingModel) {
        this.path = path;
        this.pathcost = pathcost;
        this.originatingModel = originatingModel;
        this.transitions = Lists.newArrayList();

        for (int t = 0; t < path.length - 1; t++) {
            if (path[t] != path[t+1]) {
                transitions.add(new Transition(path[t],path[t+1],t));
            }
        }
    }
}
