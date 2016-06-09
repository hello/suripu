package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by jyfan on 5/18/16.
 */
public class SleepAlarmMsgEN {

    public static Text getSleepAlarmMessage(final String wakeTime, final int sleepDurationMinutes, final String preSleepTime, final String sleepTime) {
        final float sleepDurationHr = (float) sleepDurationMinutes/60;

        final String messageBody = String.format("You typically wake up at around %s. If you want to get %.1f hours of sleep, you should start preparing for bed at %s, and plan to sleep by %s.", wakeTime, sleepDurationHr, preSleepTime, sleepTime);
        return new Text("Bed Time", messageBody);
    }

    public static Text getSleepAlarmFallBackMessage() {
        final String messageBody = String.format("If you typically wake up at 7 AM and want to get 8.5 hours of sleep, you should start preparing for bed at 10 PM, and plan to sleep by 10:30 PM.");
        return new Text("Bed Time", messageBody);
    }
}
