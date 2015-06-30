package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jingyun on 6/26/2015
 */

public class WakeVarianceMsgEN {
    public static Text getWakeVarianceLow(final double wakeVariance, final int percentile) {
        return new Text("Some text blah blah?",//What is this text for?
                String.format("Your wake-up time variance of %d is low, ", wakeVariance) +
                        String.format("it is **lower than** %d%% of all Sense users.", 100 - percentile));
    }

    public static Text getWakeVarianceNotLowEnough(final double medianLight, final int percentile) {
        return new Text("##############CHANGE ME##############", //TODO: CHANGE ME
                String.format("##############CHANGE ME##############%d ##############CHANGE ME##############, ", medianLight) + //TODO: CHANGE ME
                        String.format("##############CHANGE ME############## %d%% ##############CHANGE ME##############", 100 - percentile)); //TODO: CHANGE ME
    }

    public static Text getWakeVarianceHigh(final double medianLight, final int percentile) {
        return new Text("##############CHANGE ME##############", //TODO: CHANGE ME
                String.format("##############CHANGE ME############## %d ##############CHANGE ME############## ", medianLight) + //TODO: CHANGE ME
                        String.format("##############CHANGE ME############## %d%% ##############CHANGE ME##############.", percentile) + //TODO: CHANGE ME
                        "\n\n##############CHANGE ME##############"); //TODO: CHANGE ME

    }

    public static Text getWakeVarianceTooHigh(final double medianLight, final int percentile) {
        return new Text("##############CHANGE ME##############", //TODO: CHANGE ME
                String.format("##############CHANGE ME############## %d ##############CHANGE ME############## ", medianLight) + //TODO: CHANGE ME
                        String.format("##############CHANGE ME############## %d%% ##############CHANGE ME##############.", percentile) + //TODO: CHANGE ME
                        "\n\n##############CHANGE ME##############"); //TODO: CHANGE ME
    }
}