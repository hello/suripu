package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class SleepMotionMsgEN {

    public static Text moreMovement(final int numNights, final int greater, final float diff, final float perc) {
        String firstSentence = String.format("Out of the last %d nights, you moved on average **%.1f%% more** than our average users ",
                numNights, Math.abs(diff));

        if (numNights == greater) {
            firstSentence += "during all nights. ";
        } else {
            firstSentence = String.format("%d of the nights. ", greater);
        }

        return new Text("Hey Big Mover",
                firstSentence + String.format("About %.1f%% of your sleep period consist of agitated sleep.", perc)
        );
    }

    public static Text lessMovement(final int numNights, final int lesser, final float diff, final float perc) {
        String firstSentence = String.format("Out of the last %d nights, you moved on average **%.1f%% less** than our average users ",
                numNights, Math.abs(diff));

        if (numNights == lesser) {
             firstSentence += "during all nights. ";
        } else {
            firstSentence = String.format("%d of the nights. ", lesser);
        }

        return new Text("Still as the Night",
                firstSentence + String.format("About %.1f%% of your sleep period consist of agitated sleep.", perc)
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
                String.format("we notice that you only move **%.1f %% %s** our average Sense user. ", diff, description) +
                String.format("About %.1f%% of your sleep period consist of agitated sleep.", perc));
    }
}
