package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 5/18/16.
 */
public class SleepAlarmMsgEN {

    public static Text getSleepAlarmMessage(final String wakeTime, final int sleepDurationMinutes, final String preSleepTime, final String sleepTime) {
        final float sleepDurationHr = (float) sleepDurationMinutes/60;

        final String messageBody = String.format("You typically wake up at %s. If you want to get %.1f hours of sleep, you should start preparing for bed at %s, and plan to sleep by %s", wakeTime, sleepDurationHr, preSleepTime, sleepTime);
        return new Text("Bed Time", messageBody);
    }
}
