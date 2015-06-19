package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class SleepMotionMsgEN {

    public static Text moreMovement(final int numNights, final float diff, final float perc) {
        return new Text("Mover and Shaker",
                String.format("Out of the last %d nights, you moved on average **%.1f%% more**",
                        numNights, Math.abs(diff)) +
                        " than the average Sense user. " +
                        String.format("About %.1f%% of your sleep consists of agitated sleep.", perc)
        );
    }

    public static Text lessMovement(final int numNights, final float diff, final float perc) {
        return new Text("Still as the Night",
                String.format("Out of the last %d nights, you moved on average **%.1f%% less**",
                        numNights, Math.abs(diff)) +
                        " than the average Sense user. " +
                        String.format("About %.1f%% of your sleep consists of agitated sleep.", perc)
        );
    }

    public static Text equalMovement(final int numNights, final float diff, final float perc) {
        String description;
        if (diff > 0) {
            description = "more";
        } else {
            description = "less";
        }

        return new Text("The Way You Move",
                String.format("In each of the last %d nights, ", numNights) +
                String.format("we noticed that you only moved **%.1f%% %s** than the average Sense user. ", Math.abs(diff), description) +
                String.format("About %.1f%% of your sleep consists of agitated sleep.", perc));
    }
}
