package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class TemperatureMsgEN {

    public static final String DEGREE_C = "\u00b0C";
    public static final String DEGREE_F = "\u00b0F";

    public static final String TEMP_SLEEPER_MSG_NONE = "for a good night's sleep";
    public static final String TEMP_SLEEPER_MSG_COLD = "for a cold sleeper";
    public static final String TEMP_SLEEPER_MSG_HOT = "for a warm sleeper";

    public static final String getCommonMsg(final int minTempF, final int minTempC,
                                            final int maxTempF, final int maxTempC) {
        return String.format("Your bedroom's temperature during your sleep ranges from %d°F (%d°C) to %d°F (%d°C). ",
                minTempF, minTempC, maxTempF, maxTempC);
    }

    public static Text getTempMsgPerfect(final String commonMsg, final String sleeperMsg) {
        return new Text("Perfect Temperature", commonMsg +
                "This is the **perfect** condition " + sleeperMsg + ".\n\n" +
                "Sense will continue to monitor your sleeping temperature and alert you of any changes.");
    }

    public static Text getTempMsgTooCold(final String commonMsg, final int tempF, final int tempC) {
        return new Text("It's Freezing in Here", commonMsg +
                "It's **too cold** for sleeping.\n\n" +
                String.format("Try turning up the thermostat to a minimum of %d°F (%d°C). ", tempF, tempC) +
                "Alternatively, you could use a thicker blanket, or put on more layers before you go to bed.");
    }

    public static Text getTempMsgTooHot(final String commonMsg, final int tempF, final int tempC) {
        return new Text("It's Hot in Here", commonMsg +
                "It's **too warm** for ideal sleep.\n\n" +
                String.format("Try lowering the thermostat to %d°F (%d°C). ", tempF, tempC) +
                "You can also open the windows to let in some cool air.");
    }

    public static Text getTempMsgCool(final String commonMsg) {
        return new Text("It Might be a Little Chilly", commonMsg +
                "You might feel **a little cold** in the early morning.\n\n" +
                "Try programming the thermostat to a warmer temperature for the early morning.");
    }

    public static Text getTempMsgWarm(final String commonMsg) {
        return new Text("It's a Little Warm", commonMsg +
                "Your room is **a little warmer** than the ideal conditions.\n\n" +
                "Try to cool the bedroom a little before going to bed.");
    }

    public static Text getTempMsgBad(final String commonMsg, final String sleeperMsg,
                                     final int minTempF, final int minTempC,
                                     final int maxTempF, final int maxTempC) {
        return new Text("Wild Temperature Swing!", commonMsg +
                "The **temperature swing** in your bedroom is too large. The ideal temperature " +
                sleeperMsg + " is between" +
                String.format("%d°F (%d°C) to %d°F (%d°C).", minTempF, minTempC, maxTempF, maxTempC));
    }
}
