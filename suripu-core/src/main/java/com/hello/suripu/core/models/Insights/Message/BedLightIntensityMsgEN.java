package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 8/28/15.
 */
public class BedLightIntensityMsgEN {

    public static Text getGoodHabits(final int morningRatio) {
        return new Text("Early bird gets the worm",
                String.format("good job"));
    }

    public static Text getMoreThanOne(final int nightRatio) {
        return new Text("Who's on first?",
                String.format("don't be bad"));
    }

    public static Text getMoreThanTwo(final int nightRatio) {
        return new Text("What happens once will never...",
                String.format("don't be bad"));
    }
    public static Text getMoreThanThree(final int nightRatio) {
        return new Text("Three time's the charm",
                String.format("don't be bad"));
    }






}
