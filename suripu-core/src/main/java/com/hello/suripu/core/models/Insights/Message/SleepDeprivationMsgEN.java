package com.hello.suripu.core.models.Insights.Message;

import java.text.DecimalFormat;

/**
 * Created by jarredheinrich on 7/8/16.
 */
public class SleepDeprivationMsgEN {

    public static Text getSleepDeprivationMessage(final int idealDuration, final int avgSleepDebtDuration) {
        final float avgSleepDebtHour = ((float) (avgSleepDebtDuration / 30) ) / 2.0f;
        final DecimalFormat df = new DecimalFormat("0.###");
        String plural = "s";
        if (avgSleepDebtHour == 1.0f) {
            plural = "";
        }

            return new Text("The importance of sleep",
                    String.format("For the last four nights, you got less than your ideal %d hours of sleep. ", idealDuration)
                            + String.format("During those nights, you slept %s hour%s less than your nightly average.", df.format(avgSleepDebtHour), plural));
        }


}