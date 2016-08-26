package com.hello.suripu.core.insights.models.text;

/**
 * Created by jyfan on 5/18/16.
 */
public class SleepAlarmMsgEN {

    public static Text getSleepAlarmMessage(final String wakeTime, final int sleepDurationMinutes, final String preSleepTime, final String sleepTime) {
        final float sleepDurationHr = (float) sleepDurationMinutes/60;

        final String sleepDurationHrString;
        final String[] mySplit = String.format("%.1f", sleepDurationHr).split("\\.");
        if (mySplit.length > 1 && mySplit[1].equals("0")) {
            sleepDurationHrString = mySplit[0];
        } else {
            sleepDurationHrString = String.format("%.1f", sleepDurationHr);
        }

        final String messageBody = String.format("You typically wake up around %s. If you want to get %s hours of sleep, you should start preparing for bed at %s, and plan to sleep by %s.", wakeTime, sleepDurationHrString, preSleepTime, sleepTime);

        return new Text("Bed Time", messageBody);
    }

}
