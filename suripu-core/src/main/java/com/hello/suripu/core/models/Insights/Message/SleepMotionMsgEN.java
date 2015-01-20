package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class SleepMotionMsgEN {

    public static Text moreMovement(final int numNights, final int greater, final float diff, final float perc) {
        return new Text("Hey Big Mover",
                String.format("Out of the last %d nights, you moved more than our average users in %d of the nights, ",
                        numNights, greater) +
                        String.format("with an overall of average **+%0.1f%%**. ", diff) +
                        String.format("About %0.1%% of your sleep are considered agitated sleep.", perc)
        );
    }

    public static Text lessMovement(final int numNights, final int lesser, final float diff, final float perc) {
        return new Text("Still as the Night",
                String.format("Out of the last %d nights, you moved less than our average users in %d of the nights",
                        numNights, lesser) +
                        String.format("with an overall of average **+%0.1f%%**. ", diff) +
                        String.format("About %0.1%% of your sleep are considered agitated sleep.", perc)
        );
    }

    public static Text equalMovement(final int numNights, final float diff, final float perc) {
        String description;
        if (diff > 0) {
            description = "more than";
        } else {
            description = "less than";
        }

        return new Text("Just Right", String.format("In each of the last %d nights, ", numNights) +
                String.format("we notice that you only move %0.1f%% %s our average Sense user. ",
                        diff, description) +
                String.format("About %0.1%% of your sleep are considered agitated sleep.", perc));
    }
}
