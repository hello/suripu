package com.hello.suripu.core.models;

import com.google.common.base.Optional;

/**
 * Created by benjo on 8/19/15.
 */
public class OnlineHmmData {

    public static final String OUTPUT_MODEL_BED = "BED";
    public static final String OUTPUT_MODEL_SLEEP = "SLEEP";

    final public Optional<OnlineHmmPriors> modelPriors;

    final public Optional<OnlineHmmScratchPad> scratchPad;


    public OnlineHmmData(final Optional<OnlineHmmPriors> modelPriors, final Optional<OnlineHmmScratchPad> scratchPad) {
        this.modelPriors = modelPriors;
        this.scratchPad = scratchPad;
    }
}
