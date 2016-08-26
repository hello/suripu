package com.hello.suripu.core.insights.models.text;

/**
 * Created by kingshy on 1/12/15.
 */
public class SleepMotionMsgEN {

    public static Text moreMovement(final int numNights, final int diff, final int percentage) {
        return new Text("Mover and Shaker",
                String.format("During the last %d nights, you moved %d%% more than the average person using Sense. About **%d%% of your sleep** consists of agitated sleep.", numNights, Math.abs(diff), percentage));
    }

    public static Text lessMovement(final int numNights, final int diff, final int percentage) {
        return new Text("Still as the Night",
                String.format("During the last %d nights, you moved %d%% less than the average person using Sense. About **%d%% of your sleep** consists of agitated sleep.",
                        numNights, Math.abs(diff), percentage));
    }

    public static Text reallyEqualMovement(final int numNights, final int diff, final int percentage) {
        return new Text("Marxist utopia",
                String.format("During the last %d nights, you moved around the same amount as the average person using Sense. About **%d%% of your sleep** consists of agitated sleep.", numNights, percentage));
    }
}
