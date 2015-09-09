package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class SleepMotionMsgEN {

    public static Text moreMovement(final int numNights, final int diff, final int percentage) {
        return new Text("Mover and Shaker",
                String.format("During the last %d nights, you moved on average %d%% more",
                        numNights, Math.abs(diff)) +
                        " than the average person using Sense. " +
                        String.format("About **%d%% of your sleep** consists of agitated sleep.", percentage)
        );
    }

    public static Text lessMovement(final int numNights, final int diff, final int percentage) {
        return new Text("Still as the Night",
                String.format("During the last %d nights, you moved on average %d%% less",
                        numNights, Math.abs(diff)) +
                        " than the average person using Sense. " +
                        String.format("About **%d%% of your sleep** consists of agitated sleep.", percentage)
        );
    }

    public static Text equalMovement(final int numNights, final int diff, final int percentage) {
        String description;
        if (diff > 0) {
            description = "more";
        } else {
            description = "less";
        }

        return new Text("The Way You Move",
                String.format("During the last %d nights, ", numNights) +
                String.format("we noticed that you moved %d%% %s than the average person using Sense. ", Math.abs(diff), description) +
                String.format("About **%d%% of your sleep** consists of agitated sleep.", percentage));
    }

    public static Text reallyEqualMovement(final int numNights, final int diff, final int percentage) {
        return new Text("Marxist utopia",
                String.format("During the last %d nights, we noticed that you moved the same amount as the average person using Sense. ", numNights) +
                        String.format("About **%d%% of your sleep** consists of agitated sleep.", percentage)
        );
    }
}
