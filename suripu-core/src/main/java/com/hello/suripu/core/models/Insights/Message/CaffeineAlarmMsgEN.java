package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 5/11/16.
 */
public class CaffeineAlarmMsgEN {
    public static Text getCaffeineAlarmMessage(final String sleepTime, final String coffeeTime) {
        final String messageBody = String.format("Since you usually go to bed at around %s, try not to drink coffee after %s in order to minimize the effect of caffeine on your sleep.", sleepTime, coffeeTime);
        return new Text("Coffee Time", messageBody);
    }

    public static Text getCaffeineAlarmFallBackMessage() {
        final String messageBody = String.format("If you usually go to bed at 11 PM, try not to drink coffee after 5 PM in order to minimize the effect of caffeine on your sleep.");
        return new Text("Coffee Time", messageBody);
    }
}
