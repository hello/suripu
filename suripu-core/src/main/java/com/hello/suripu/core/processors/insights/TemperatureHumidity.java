package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.TemperatureMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
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

    private static final int IDEAL_TEMP_MIN = 60;
    private static final int IDEAL_TEMP_MAX = 70;
    private static final int COLD_TEMP_ADJUST = 3; // adjust for cold sleeper
    private static final int HOT_TEMP_ADJUST = 5; // adjust for hot sleeper

    private static final int TEMP_START_HOUR = 23; // 11pm
    private static final int TEMP_END_HOUR = 6; // 6am

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final AccountInfo.SleepTempType tempPref) {
        final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).withHourOfDay(TEMP_END_HOUR); // today 6am
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.RECENT_DAYS).withHourOfDay(TEMP_START_HOUR); // 11pm three days ago

        final int slotDuration = 30;
        final List<DeviceData> sensorData = deviceDataDAO.getBetweenByLocalHourAggregateBySlotDuration(accountId, deviceId, queryStartTime,
                queryEndTime, TEMP_START_HOUR, TEMP_END_HOUR, slotDuration);

        final Optional<InsightCard> card = processData(accountId, sensorData, tempPref);
        return card;
    }

    public static Optional<InsightCard> processData(final Long accountId, final List<DeviceData> data, final AccountInfo.SleepTempType tempPref) {

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

        final int idealMinC = fahrenheitToCelsius((double) idealMinF);
        final int idealMaxC = fahrenheitToCelsius((double) idealMaxF);


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
        Text text;
        final String commonMsg = TemperatureMsgEN.getCommonMsg(minTempF, minTempC, maxTempF, maxTempC);

        if (idealMinF <= minTempF && maxTempF <= idealMaxF) {
            text = TemperatureMsgEN.getTempMsgPerfect(commonMsg, sleeperMsg);

        } else if (maxTempF < idealMinF) {
            text = TemperatureMsgEN.getTempMsgTooCold(commonMsg, idealMinF, idealMinC);

        } else if (minTempF > idealMaxF) {
            text = TemperatureMsgEN.getTempMsgTooHot(commonMsg, idealMaxF, idealMaxC);

        } else if (minTempF < idealMinF && maxTempF <= idealMaxF) {
            text = TemperatureMsgEN.getTempMsgCool(commonMsg);

        } else if (minTempF > idealMinF && maxTempF > idealMaxF) {
            text = TemperatureMsgEN.getTempMsgWarm(commonMsg);

        } else {
            // both min and max are outside of ideal range
            text = TemperatureMsgEN.getTempMsgBad(commonMsg, sleeperMsg, idealMinF, idealMinC, idealMaxF, idealMaxC);
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
