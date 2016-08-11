package com.hello.suripu.core.models.Insights.Message;

/**
 * Created by kingshy on 1/12/15.
 */
public class TemperatureMsgEN {
    public static final String DEGREE = "\u00b0";
    public static final String DEGREE_C = "\u00b0C";
    public static final String DEGREE_F = "\u00b0F";

    public static Text getTempMsgPerfect(final int minTemp, final int maxTemp, final String unit) {
        return new Text("Perfect Temperature",
                String.format("The temperature in your bedroom ranges from %d°%s to %d°%s, which is the **ideal temperature** for good sleep.", minTemp, unit, maxTemp, unit));
    }

    public static Text getTempMsgTooCold(final int minTemp, final int maxTemp, final String unit, final int idealTempMin) {
        return new Text("It's Cold in Here",
                String.format("The temperature in your bedroom ranges from %d°%s to %d°%s, which is **too cold** for good sleep. Try raising the temperature to at least %d°%s. Alternatively, consider using a thicker blanket or wearing warmer clothing while sleeping.", minTemp, unit, maxTemp, unit, idealTempMin, unit));
    }

    public static Text getTempMsgTooHot(final int minTemp, final int maxTemp, final String unit, final int idealTempMax) {
        return new Text("It's Hot in Here",
                String.format("The temperature in your bedroom ranges from %d°%s to %d°%s, which is **too warm** for good sleep. Try lowering the temperature to %d°%s, or turning on a fan. Weather permitting, you may also want to open a window to let in some cool air.", minTemp, unit, maxTemp, unit, idealTempMax, unit));
    }

    public static Text getTempMsgFluctuate(final int minTemp, final int maxTemp, final String unit, final int idealTempMin, final int idealTempMax) {
        return new Text("Hot and Cold",
                String.format("The temperature in your bedroom ranges from %d°%s to %d°%s. That's **too much fluctuation** in temperature. The ideal temperature is consistently between %d°%s and %d°%s.",
                        minTemp, unit, maxTemp, unit, idealTempMin, unit, idealTempMax, unit));
    }
}
