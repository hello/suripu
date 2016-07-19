package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jingyun on 6/26/2015
 */

public class WakeVarianceMsgEN {
    public static Text getWakeVarianceLow(final int wakeStdDev, final int percentile) {
        final float wakeStdDevHr = (float) wakeStdDev/60;
        return new Text("Hello, very regular",
                String.format("The time you wake up each morning is **very consistent**. It varied an average of %.1f hours last week, ", wakeStdDevHr)
                        + String.format("which is more consistent than %d%% of other people using Sense. ", 100 - percentile)
                        + "\n\nWaking up at the same time each morning is great for your internal clock, and helps you sleep better.");
    }

    public static Text getWakeVarianceNotLowEnough(final int wakeStdDev, final int percentile) {
        final float wakeStdDevHr = (float) wakeStdDev/60;
        return new Text("Hello, regular",
                String.format("The time you wake up each morning is **fairly consistent**. It varied an average of %.1f hours last week, ", wakeStdDevHr)
                        + String.format("which is more consistent than %d%% of other people using Sense. ", 100 - percentile)
                        + "\n\nWaking up at the same time each morning is great for your internal clock, and helps you sleep better.");
    }

    public static Text getWakeVarianceHigh(final int wakeStdDev, final int percentile) {
        final float wakeStdDevHr = (float) wakeStdDev/60;
        return new Text("Hello, irregular",
                String.format("The time you wake up each morning is **a little inconsistent**. It varied an average of %.1f hours last week, ", wakeStdDevHr)
                        + String.format("which is less consistent than %d%% of other people using Sense. ", percentile)
                        + "\n\nWaking up at the same time each morning is great for your internal clock, and helps you sleep better.");
    }

    public static Text getWakeVarianceTooHigh(final int wakeStdDev, final int percentile) {
        final float wakeStdDevHr = (float) wakeStdDev/60;
        return new Text("Hello, very irregular",
                String.format("The time you wake up each morning is **pretty inconsistent**. It varied an average of %.1f hours last week, ", wakeStdDevHr)
                        + String.format("which is less consistent than %d%% of other people using Sense. ", percentile)
                        + "\n\nWaking up at the same time each morning is great for your internal clock, and helps you sleep better.");
    }
}