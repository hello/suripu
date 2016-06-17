package com.hello.suripu.core.models.Insights.Message;

import com.hello.suripu.core.preferences.TimeFormat;

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

        final String messageBody = String.format("You typically wake up at around %s. If you want to get %s hours of sleep, you should start preparing for bed at %s, and plan to sleep by %s.", wakeTime, sleepDurationHrString, preSleepTime, sleepTime);

        return new Text("Bed Time", messageBody);
    }

    public static Text getSleepAlarmFallBackMessage(final TimeFormat timeFormat) {
        final String messageBody;

        if (timeFormat.equals(TimeFormat.TIME_TWENTY_FOUR_HOUR)) {
            messageBody = String.format("If you typically wake up at 07:00 and want to get 8.5 hours of sleep, you should start preparing for bed at 22:00, and plan to sleep by 22:30 PM.");
        } else {
            messageBody = String.format("If you typically wake up at 7 AM and want to get 8.5 hours of sleep, you should start preparing for bed at 10 PM, and plan to sleep by 10:30 PM.");
        }

        return new Text("Bed Time", messageBody);
    }
}
