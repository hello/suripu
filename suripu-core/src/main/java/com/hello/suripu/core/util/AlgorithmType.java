package com.hello.suripu.core.util;

/**
 * Created by benjo on 7/31/15.
 */
public enum AlgorithmType {
    NONE("none"),
    WUPANG("wupang"),
    VOTING("voting"),
    HMM("hmm"),
    LAYERED_HMM("layered_hmm");


    private final String name;

    AlgorithmType(final String name) {
        this.name = name;
    }

}
