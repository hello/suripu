package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jarredheinrich on 7/8/16.
 */
public class SleepDeprivationMsgEN {

    public static Text getSleepDeprivationMessage(final int idealDuration, final int avgSleepDebtDuration) {

        return new Text("The importance of sleep",
                String.format("For the last four nights, you didn't get your ideal %d hours of sleep.  ", idealDuration)
                        + String.format("During those nights, you have slept on average %d minutes less than your nightly average.", avgSleepDebtDuration));
    }

}