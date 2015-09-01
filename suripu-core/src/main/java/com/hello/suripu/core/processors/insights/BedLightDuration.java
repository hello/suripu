package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightDurationMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jyfan on 8/5/15.
 */
public class BedLightDuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedLightDuration.class);

    private static final int NIGHT_START_HOUR = 21; // 9pm
    private static final int NIGHT_END_HOUR = 4; // 4am

    public static final float LIGHT_LEVEL_WARNING = 5.0f;  // in lux
    public static final float LIGHT_LEVEL_ALERT = 35.0f;  // in lux

    public static final int OFF_MINUTES_THRESHOLD = 45; //If lights are off for more than 45 minutes, we discard preceeding data

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO) {

        final List<Integer> lightOnList = new ArrayList<>();

        for (int i = 0; i >= 6; i++) {
            final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).minusDays(i);
            final DateTime queryStartTime = DateTime.now(DateTimeZone.UTC).minusDays(i + 1);
            // get light data > some threshold between the hours of 9pm and 4am
            List<DeviceData> rows = deviceDataDAO.getLightByBetweenHourDate(accountId, deviceId, (int) LIGHT_LEVEL_WARNING, queryStartTime, queryEndTime, NIGHT_START_HOUR, NIGHT_END_HOUR);
            Optional<Integer> lightOnDuration = processLightDataOneDay(rows, OFF_MINUTES_THRESHOLD);
            if (lightOnDuration.isPresent()) {
                lightOnList.add(lightOnDuration.get());
            }
        }

        final Optional<InsightCard> card = processLightData(lightOnList, accountId);
        return card;
    }

    public static Optional<Integer> processLightDataOneDay(final List<DeviceData> data, final int offMinutesThreshold) {
        if (data.size() <= 1) {
            return Optional.absent();
        }

        final DateTime lastLightOnTimeStamp = data.get(data.size() - 1).dateTimeUTC;
        for (int i = data.size() - 2; i >= 0; i--) {
            final DateTime currentLightOnTimeStamp = data.get(i).dateTimeUTC;
            final DateTime previousLightOnTimeStamp = data.get(i + 1).dateTimeUTC;
            //assertTrue(currentLightOnTimeStamp < previousLightOnTimeStamp);
            final Period offTime = new Period(currentLightOnTimeStamp, previousLightOnTimeStamp);
            final int offMinutes = offTime.getMinutes();

            if (offMinutes > offMinutesThreshold) {
                final Period onTimeTruncated = new Period(previousLightOnTimeStamp, lastLightOnTimeStamp);
                final Integer onMinutesTruncated = onTimeTruncated.getMinutes();
                LOGGER.debug("on Minutes truncated is {}", onMinutesTruncated);
                return Optional.of(onMinutesTruncated);
            }
        }

        final DateTime firstLightOnTimeStamp = data.get(0).dateTimeUTC;
        final Period onTime = new Period(firstLightOnTimeStamp, lastLightOnTimeStamp);
        final Integer onMinutes = onTime.getMinutes();
        return Optional.of(onMinutes);
    }

    public static Optional<InsightCard> processLightData(final List<Integer> lightOnList, final Long accountId) {

        if (lightOnList.size() == 0) {
            LOGGER.debug("Light was on for less than 1 second each day for accountId {}", accountId);
            return Optional.absent();
        }

        // compute average value
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final Integer lightOn : lightOnList) {
            stats.addValue(lightOn);
        }

        int avgLightOn = (int) stats.getMean();

        Text text;
        if (avgLightOn <= 60) {
            return Optional.absent();
//            text = BedLightDurationMsgEN.getLittleLight();
        } else if (avgLightOn <= 120) {
            text = BedLightDurationMsgEN.getMediumLight();
        } else {
            text = BedLightDurationMsgEN.getHighLight();
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.BED_LIGHT_DURATION, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC)));
    }
}
