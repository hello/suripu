package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jingyun on 6/26/2015
 */

public class WakeVarianceMsgEN {
    public static Text getWakeVarianceLow(final int wakeStdDev, final int percentile) {
        return new Text("Hello, very regular",
                String.format("Your wake time is **very consistent**. It varied an average of %d minutes last week, ", wakeStdDev) + String.format("Which is less than %d%% of all Sense users. ", 100 - percentile));
    }

    public static Text getWakeVarianceNotLowEnough(final int wakeStdDev, final int percentile) {
        return new Text("Hello, regular",
                String.format("Your wake time is **fairly consistent**. It varied an average of %d minutes last week, ", wakeStdDev) + String.format("which is less than %d%%% of all Sense users. ", 100 - percentile) +
                        "\n\nTry waking up at roughly the same time every morning");
    }

    public static Text getWakeVarianceHigh(final int wakeStdDev, final int percentile) {
        return new Text("Hello, irregular",
                String.format("Your wake time is **a little inconsistent**. It varied an average of %d minutes last week, ", wakeStdDev) + String.format("which is more than %d%% of all Sense users. ", percentile) +
                        "\n\nTry waking up at roughly the same time each morning.");
    }

    public static Text getWakeVarianceTooHigh(final int wakeStdDev, final int percentile) {
        return new Text("Hello, very irregular",
                String.format("Your wake time are **pretty inconsistent**. It varied an average of %d minutes last week, ", wakeStdDev) + String.format("which is more than %d%% of all Sense users.", percentile) +
                        "\n\nTry waking up at roughly the same time each morning.");
    }
}