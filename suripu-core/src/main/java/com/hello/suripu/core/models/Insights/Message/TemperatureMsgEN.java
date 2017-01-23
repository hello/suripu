package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class TemperatureMsgEN {
    public static final String DEGREE = "\u00b0";
    public static final String DEGREE_C = "\u00b0C";
    public static final String DEGREE_F = "\u00b0F";

    public static Text getTempMsgPerfect(final int medTemp, final String unit) {
        return new Text("Perfect Temperature",
                String.format("The median temperature in your bedroom is %d°%s, which is within the **ideal range** for good sleep.", medTemp, unit));
    }

    public static Text getTempMsgTooCold(final int medTemp, final String unit, final int idealTempMin) {
        return new Text("It's Cold in Here",
                String.format("The median temperature in your bedroom is %d°%s, which is **too cold** for good sleep. Try raising the temperature to at least %d°%s. Alternatively, consider using a thicker blanket or wearing warmer clothing while sleeping.", medTemp, unit, idealTempMin, unit));
    }

    public static Text getTempMsgTooHot(final int medTemp, final String unit, final int idealTempMax) {
        return new Text("It's Hot in Here",
                String.format("The median temperature in your bedroom is %d°%s, which is **too warm** for good sleep. Try lowering the temperature to %d°%s, or turning on a fan. Weather permitting, you may also want to open a window to let in some cool air.", medTemp, unit, idealTempMax, unit));
    }
}
