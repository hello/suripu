package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by kingshy on 1/5/15.
 */
public class TemperatureHumidity {
    private static final Logger LOGGER = LoggerFactory.getLogger(TemperatureHumidity.class);

    private static final double TEMP_MULTIPLIER = 100.0;

    private static final String HOT_SLEEPER = "hot";
    private static final String COLD_SLEEPER = "cold";
    private static final String DEGREE_C = "\u00b0C";
    private static final String DEGREE_F = "\u00b0F";

    private static final int IDEAL_TEMP_MIN = 60;
    private static final int IDEAL_TEMP_MAX = 70;
    private static final int COLD_TEMP_ADJUST = 3; // adjust for cold sleeper
    private static final int HOT_TEMP_ADJUST = 5; // adjust for hot sleeper

    private static final int TEMP_START_HOUR = 23; // 11pm
    private static final int TEMP_END_HOUR = 6; // 6am

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final String tempPref) {
        final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).withHourOfDay(TEMP_END_HOUR); // today 6am
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.RECENT_DAYS).withHourOfDay(TEMP_START_HOUR); // 11pm three days ago

        final int slotDuration = 30;
        final List<DeviceData> sensorData = deviceDataDAO.getBetweenByLocalHourAggregateBySlotDuration(accountId, deviceId, queryStartTime,
                queryEndTime, TEMP_START_HOUR, TEMP_END_HOUR, slotDuration);

        final Optional<InsightCard> cards = processData(accountId, sensorData, tempPref);
        return cards;
    }

    public static Optional<InsightCard> processData(final Long accountId, final List<DeviceData> data, final String tempPref) {

        if (data.isEmpty()) {
            return Optional.absent();
        }

        // TODO if location is available, compare with users from the same city

        // get min, max and average
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final DeviceData deviceData : data) {
            stats.addValue(deviceData.ambientTemperature);
        }

        final double tmpMinValue = stats.getMin() / TEMP_MULTIPLIER;
        final int minTempC = (int) tmpMinValue;
        final int minTempF = celsiusToFahrenheit(tmpMinValue);

        final double tmpMaxValue = stats.getMax() / TEMP_MULTIPLIER;
        final int maxTempC = (int) tmpMaxValue;
        final int maxTempF = celsiusToFahrenheit(tmpMaxValue);

        LOGGER.debug("Temp for account {}: min {}, max {}", minTempF, maxTempF);

        // adjust ideal range depending on user's response to hot/cold sleeper question
        int idealMinF = IDEAL_TEMP_MIN;
        int idealMaxF = IDEAL_TEMP_MAX;
        String sleeperMsg = "for a good night's sleep";
        if (tempPref.equals(COLD_SLEEPER)) {
            idealMinF -= COLD_TEMP_ADJUST;
            idealMaxF -= COLD_TEMP_ADJUST;
            sleeperMsg = "for a cold sleeper";
        } else if (tempPref.equals(HOT_SLEEPER)) {
            idealMinF += HOT_TEMP_ADJUST;
            idealMaxF += HOT_TEMP_ADJUST;
            sleeperMsg = "for a warm sleeper";
        }

        final int idealMinC = fahrenheitToCelsius((double) idealMinF);
        final int idealMaxC = fahrenheitToCelsius((double) idealMaxF);

        String title;
        String message = String.format("Your bedroom's temperature during your sleep ranges from %d%s (%d%s) to %d%s (%d%s). ",
                minTempF, DEGREE_F, minTempC, DEGREE_C, maxTempF, DEGREE_F, maxTempC, DEGREE_C);

        /* Possible cases
                    min                       max
                    |------ ideal range ------|
            |----|                              |-----|
            too cold                            too hot

                |------|                  |-------|
                a little cold               a little warm

                |-------- way out of range! -------|
         */

        // todo: edits
        if (idealMinF <= minTempF && maxTempF <= idealMaxF) {
            title = "Perfect Temperature";
            message += "This is the **perfect** condition " + sleeperMsg + ".\n\n" +
                    "Sense will continue to monitor your sleeping temperature and alert you of any changes.";

        } else if (maxTempF < idealMinF) {
            title = "It's Freezing in Here";
            message += "It's **too cold** for sleeping.\n\n" +
                    String.format("Try turning up the thermostat to a minimum of %d%s (%d%s). ", idealMinF, DEGREE_F, idealMinC, DEGREE_C) +
                    "Alternatively, you could use a thicker blanket, or put on more layers before you go to bed.";

        } else if (minTempF > idealMaxF) {
            title = "It's Hot in Here";
            message += "It's **too warm** for ideal sleep.\n\n" +
            String.format("Try lowering the thermostat to %d%s (%d%s). ", idealMaxF, DEGREE_F, idealMaxC, DEGREE_C) +
                    "You can also open the windows to let in some cool air.";

        } else if (minTempF < idealMinF && maxTempF <= idealMaxF) {
            title = "It Might be a Little Chilly";
            message += "You might feel **a little cold** in the early morning.\n\n" +
                    "Try programming the thermostat to a warmer temperature for the early morning.";

        } else if (minTempF > idealMinF && maxTempF > idealMaxF) {
            title = "It's a Little Warm";
            message += "Your room is **a little warmer** than the ideal conditions.\n\nTry to cool the bedroom a little before going to bed.";

        } else {
            // both min and max are outside of ideal range
            title = "Wild Temperature Swing!";
            message = "The **temperature swing** in your bedroom is too large. The ideal temperature " + sleeperMsg + " is between" +
                    String.format("%d%s (%d%s) to %d%s (%d%s).", idealMinF, DEGREE_F, idealMinC, DEGREE_C,
                            idealMaxF, DEGREE_F, idealMaxC, DEGREE_C);
        }

        return Optional.of(new InsightCard(accountId, title, message,
                InsightCard.Category.TEMPERATURE, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC)));
    }

    private static int celsiusToFahrenheit(final double value) {
        //Multiply by 9, then divide by 5, then add 32
        return (int) Math.round((value * 9.0) / 5.0) + 32;
    }
    private static int fahrenheitToCelsius(final double value) {
        return (int) ((value - 32.0) * (5.0/9.0));
    }
}
