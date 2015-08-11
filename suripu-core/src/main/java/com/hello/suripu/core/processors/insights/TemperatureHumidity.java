package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.TemperatureMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.preferences.TemperatureUnit;
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

    public static final int IDEAL_TEMP_MIN = 59;
    public static final int IDEAL_TEMP_MAX = 73;

    public static final int IDEAL_TEMP_MIN_CELSIUS = 15;
    public static final int IDEAL_TEMP_MAX_CELSIUS = 23;

    public static final int ALERT_TEMP_MIN = 55;
    public static final int ALERT_TEMP_MAX = 79;

    public static final int ALERT_TEMP_MIN_CELSIUS = 13;
    public static final int ALERT_TEMP_MAX_CELSIUS = 26;


    public static final int ALERT_HUMIDITY_LOW = 20;
    public static final int ALERT_HUMIDITY_HIGH = 70;

    public static final int IDEAL_HUMIDITY_MIN = 30;
    public static final int IDEAL_HUMIDITY_MAX = 60;

    private static final int COLD_TEMP_ADJUST = 3; // adjust for cold sleeper
    private static final int HOT_TEMP_ADJUST = 5; // adjust for hot sleeper

    private static final int TEMP_START_HOUR = 23; // 11pm
    private static final int TEMP_END_HOUR = 6; // 6am

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId,
                                                    final DeviceDataDAO deviceDataDAO,
                                                    final AccountInfo.SleepTempType tempPref,
                                                    final TemperatureUnit tempUnit) {
        final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).withHourOfDay(TEMP_END_HOUR); // today 6am
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.RECENT_DAYS).withHourOfDay(TEMP_START_HOUR); // 11pm three days ago

        final int slotDuration = 30;
        final List<DeviceData> sensorData = deviceDataDAO.getBetweenByLocalHourAggregateBySlotDuration(accountId, deviceId, queryStartTime,
                queryEndTime, TEMP_START_HOUR, TEMP_END_HOUR, slotDuration);

        final Optional<InsightCard> card = processData(accountId, sensorData, tempPref, tempUnit);
        return card;
    }

    public static Optional<InsightCard> processData(final Long accountId, final List<DeviceData> data,
                                                    final AccountInfo.SleepTempType tempPref,
                                                    final TemperatureUnit tempUnit) {

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

        LOGGER.debug("Temp for account {}: min {}, max {}", accountId, minTempF, maxTempF);

        // adjust ideal range depending on user's response to hot/cold sleeper question
        int idealMinF = IDEAL_TEMP_MIN;
        int idealMaxF = IDEAL_TEMP_MAX;
        String sleeperMsg = TemperatureMsgEN.TEMP_SLEEPER_MSG_NONE;
        if (tempPref == AccountInfo.SleepTempType.COLD) {
            idealMinF -= COLD_TEMP_ADJUST;
            idealMaxF -= COLD_TEMP_ADJUST;
            sleeperMsg = TemperatureMsgEN.TEMP_SLEEPER_MSG_COLD;
        } else if (tempPref == AccountInfo.SleepTempType.HOT) {
            idealMinF += HOT_TEMP_ADJUST;
            idealMaxF += HOT_TEMP_ADJUST;
            sleeperMsg = TemperatureMsgEN.TEMP_SLEEPER_MSG_HOT;
        }

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
        int minTemp = minTempF;
        int maxTemp = maxTempF;
        int idealMin = idealMinF;
        int idealMax = idealMaxF;
        if (tempUnit == TemperatureUnit.CELSIUS) {
            minTemp = fahrenheitToCelsius((double) minTempF);
            maxTemp = fahrenheitToCelsius((double) maxTempF);
            idealMin = fahrenheitToCelsius((double) idealMinF);
            idealMax = fahrenheitToCelsius((double) idealMaxF);
        }

        Text text;
        final String commonMsg = TemperatureMsgEN.getCommonMsg(minTemp, maxTemp, tempUnit.toString());

        if (idealMinF <= minTempF && maxTempF <= idealMaxF) {
            text = TemperatureMsgEN.getTempMsgPerfect(commonMsg, sleeperMsg);

        } else if (maxTempF < idealMinF) {
            text = TemperatureMsgEN.getTempMsgTooCold(commonMsg, idealMin, tempUnit.toString());

        } else if (minTempF > idealMaxF) {
            text = TemperatureMsgEN.getTempMsgTooHot(commonMsg, idealMax, tempUnit.toString());

        } else if (minTempF < idealMinF && maxTempF <= idealMaxF) {
            text = TemperatureMsgEN.getTempMsgCool(commonMsg);

        } else if (minTempF > idealMinF && maxTempF > idealMaxF) {
            text = TemperatureMsgEN.getTempMsgWarm(commonMsg);

        } else {
            // both min and max are outside of ideal range
            text = TemperatureMsgEN.getTempMsgBad(commonMsg, sleeperMsg, idealMin, idealMax, tempUnit.toString());
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
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
