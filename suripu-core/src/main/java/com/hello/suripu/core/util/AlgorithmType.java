package com.hello.suripu.core.util;

/**
 * Created by benjo on 7/31/15.
 */
public enum AlgorithmType {
    NONE("none"),
    WUPANG("wupang"),
    VOTING("voting"),
    HMM("hmm"),
    ONLINE_HMM("online_hmm"),
    BAYES_NET("bayes_net");


    private final String name;

    AlgorithmType(final String name) {
        this.name = name;
    }

}
