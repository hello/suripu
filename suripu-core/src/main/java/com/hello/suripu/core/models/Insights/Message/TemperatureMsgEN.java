package com.hello.suripu.core.models.Insights.Message;

import com.hello.suripu.core.preferences.TemperatureUnit;

/**
 * Created by kingshy on 1/12/15.
 */
public class TemperatureMsgEN {

    private static final String CELSIUS = TemperatureUnit.CELSIUS.toString();
    public static final String DEGREE = "\u00b0";
    public static final String DEGREE_C = "\u00b0C";
    public static final String DEGREE_F = "\u00b0F";

    public static final String TEMP_SLEEPER_MSG_NONE = "for a good night's sleep";
    public static final String TEMP_SLEEPER_MSG_COLD = "for a cold sleeper";
    public static final String TEMP_SLEEPER_MSG_HOT = "for a warm sleeper";

    public static final String getCommonMsg(final int minTemp, final int maxTemp, final String unit) {
        return String.format("Your bedroom's temperature during your sleep ranges from %d°%s to %d°%s. ", minTemp, unit, maxTemp, unit);
    }

    public static Text getTempMsgPerfect(final String commonMsg, final String sleeperMsg) {
        return new Text("Perfect Temperature", commonMsg +
                "Your bedroom is the **perfect** temperature " + sleeperMsg + ".\n\n" +
                "Sense will continue to monitor your sleeping temperature and alert you of any changes.");
    }

    public static Text getTempMsgTooCold(final String commonMsg, final int temperature, final String unit) {
        return new Text("It's Cold in Here", commonMsg +
                "It's **too cold** in your bedroom for ideal sleep conditions.\n\n" +
                String.format("Try turning up the thermostat to a minimum of %d°%s. ", temperature, unit) +
                "Alternatively, you could use a thicker blanket, or put on more layers before you go to bed.");
    }

    public static Text getTempMsgTooHot(final String commonMsg, final int temperature, final String unit) {
        return new Text("It's Hot in Here", commonMsg +
                "It's **too warm** for ideal sleep.\n\n" +
                String.format("Try lowering the thermostat to %d°%s, or use a fan. ", temperature, unit) +
                "You can also open the windows to let in some cool air.");
    }

    public static Text getTempMsgCool(final String commonMsg) {
        return new Text("It's a Bit Chilly", commonMsg +
                "You might feel **a bit cold** in the early morning.\n\n" +
                "Try programming the thermostat to a warmer temperature for the early morning.");
    }

    public static Text getTempMsgWarm(final String commonMsg) {
        return new Text("It's a Bit Warm", commonMsg +
                "Your bedroom is **a bit warmer** than the recommended ideal conditions.\n\n" +
                "Try to cool the bedroom a little before going to bed.");
    }

    public static Text getTempMsgBad(final String commonMsg, final String sleeperMsg,
                                     final int minTemp, final int maxTemp, final String unit) {
        return new Text("Hot and Cold", commonMsg +
                "The **temperature swing** in your bedroom is too large. The ideal temperature " +
                sleeperMsg + " is between " +
                String.format("%d°%s to %d°%s.", minTemp, unit, maxTemp, unit));
    }
}
