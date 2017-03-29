package com.hello.suripu.core.util;

/**
 * Created by benjo on 7/31/15.
 */
public enum AlgorithmType {
    NONE("none", 0),
    WUPANG("wupang", 1),
    VOTING("voting", 2),
    HMM("hmm", 3),
    ONLINE_HMM("online_hmm", 4),
    BAYES_NET("bayes_net", 5),
    NEURAL_NET("neural_net", 6),
    NEURAL_NET_FOUR_EVENT("neural_net_four_event", 7);


    private final String name;
    private final Integer value;

    AlgorithmType(final String name, final Integer value) {
        this.name = name;
        this.value = value;
    }
    public int getValue() {
        return this.value;
    }

    public static AlgorithmType fromInteger(int value) {
        for (final AlgorithmType algorithmType : AlgorithmType.values()) {
            if (algorithmType.getValue() == value) {
                return algorithmType;
            }
        }
        return NONE;
    }
}
