package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jingyun on 6/26/2015
 */

public class WakeVarianceMsgEN {
    public static Text getWakeVarianceLow(final int wakeStdDev, final int percentile) {
        return new Text("Hello, very regular",
                String.format("You have very consistent sleep habits. Your wake time variability of %d minutes is low, ", wakeStdDev) +
                        String.format("it is **lower than** %d%% of all Sense users.", 100 - percentile));
    }

    public static Text getWakeVarianceNotLowEnough(final int wakeStdDev, final int percentile) {
        return new Text("Hello, regular",
                String.format("You have consistent sleep habits. Your wake time variability of %d minutes is low, ", wakeStdDev) +
                        String.format("it is **lower than** %d%% of all Sense users.", 100 - percentile));
    }

    public static Text getWakeVarianceHigh(final int wakeStdDev, final int percentile) {
        return new Text("Hello, irregular",
                String.format("Your sleep habits are a bit irregular. Your wake time variability of %d minutes is a bit high, ", wakeStdDev) +
                        String.format("it is **higher than** %d%% of all Sense users.", percentile) +
                        "\n\nTry waking up at roughly the same time every morning.");
    }

    public static Text getWakeVarianceTooHigh(final int wakeStdDev, final int percentile) {
        return new Text("Hello, very irregular",
                String.format("Your sleep habits are irregular. Your wake time variability of %d minutes is high, ", wakeStdDev) +
                        String.format("it is **higher than** %d%% of all Sense users.", percentile) +
                        "\n\nTry waking up at roughly the same time every morning.");
    }
}