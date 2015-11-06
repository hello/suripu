package com.hello.suripu.core.algorithmintegration;

import com.google.common.collect.Multimap;

import java.util.Map;

/**
 * Created by benjo on 10/18/15.
 */
public class EvaluationResult {

    public final Multimap<String,MultiEvalHmmDecodedResult> modelEvaluations;
    public final Map<String,MultiEvalHmmDecodedResult> predictions;

    public EvaluationResult(Multimap<String, MultiEvalHmmDecodedResult> modelEvaluations, Map<String, MultiEvalHmmDecodedResult> predictions) {
        this.modelEvaluations = modelEvaluations;
        this.predictions = predictions;
    }
}