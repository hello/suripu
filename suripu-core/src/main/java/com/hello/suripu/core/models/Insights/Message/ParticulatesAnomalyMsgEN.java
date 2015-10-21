package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 10/7/15.
 */
public class ParticulatesAnomalyMsgEN {

    private final static String MICROGRAM_PER_CUBIC_METER = "\u00B5g/m\u00b3";

    public static Text getAirImprovement(final int currentDust, final int historyDust, final int percent) {
        return new Text("Improved", String.format("The air quality in your bedroom **improved** by %d%% yesterday, ", percent) +
        String.format("from %dµg/m³ to %dµg/m³.", historyDust, currentDust));
    }

    public static Text getAirWorse(final int currentDust, final int historyDust, final int percent) {
        return new Text("Worse", String.format("The air quality in your bedroom **worsened** by %d%% yesterday, ", percent) +
                String.format("from %dµg/m³ to %dµg/m³.", historyDust, currentDust));
    }

    public static Text getAirVeryWorse(final int currentDust, final int historyDust, int percent) {
        return new Text("A lot Worse", String.format("The air quality in your bedroom **worsened significantly** by %d%% yesterday, ", percent) +
                String.format("from %dµg/m³ to %dµg/m³.", historyDust, currentDust));
    }
}
