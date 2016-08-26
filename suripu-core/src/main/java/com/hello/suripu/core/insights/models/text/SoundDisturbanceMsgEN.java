package com.hello.suripu.core.insights.models.text;

/**
 * Created by jyfan on 9/25/15.
 */
public class SoundDisturbanceMsgEN {

    public static Text getHighSumDisturbance() {
        return new Text("High",
                "You have more sound in your bedroom than the average person who uses Sense."
        );
    }

    public static Text getVeryHighSumDisturbance() {
        return new Text("High Alert",
                "You have significantly more sound in your bedroom than the average person who uses Sense."
        );

    }

}
