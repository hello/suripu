package com.hello.suripu.core.models;

import com.google.common.base.Optional;

/**
 * Created by benjo on 8/19/15.
 */
public class OnlineHmmData {

    public static final String OUTPUT_MODEL_BED = "BED";
    public static final String OUTPUT_MODEL_SLEEP = "SLEEP";

    final public OnlineHmmPriors modelPriors;

    final public OnlineHmmScratchPad scratchPad;


    public OnlineHmmData(final OnlineHmmPriors modelPriors, final OnlineHmmScratchPad scratchPad) {
        this.modelPriors = modelPriors;
        this.scratchPad = scratchPad;
    }

    public OnlineHmmData(final OnlineHmmPriors modelPriors) {
        this.modelPriors = modelPriors;
        this.scratchPad = OnlineHmmScratchPad.createEmpty();
    }

    public OnlineHmmData(final OnlineHmmScratchPad scratchPad) {
        this.modelPriors = OnlineHmmPriors.createEmpty();
        this.scratchPad = scratchPad;
    }

    public static OnlineHmmData createEmpty() {
        return new OnlineHmmData(OnlineHmmPriors.createEmpty(),OnlineHmmScratchPad.createEmpty());
    }


}
