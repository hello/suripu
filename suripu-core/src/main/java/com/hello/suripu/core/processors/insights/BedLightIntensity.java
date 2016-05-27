package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.BedLightIntensityMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jyfan on 8/5/15.
 */
public class BedLightIntensity {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedLightIntensity.class);

    private static final int NIGHT_START_HOUR_LOCAL = 21; //9pm
    private static final int NIGHT_END_HOUR_LOCAL = 3; //3am

    private static final int MORNING_START_HOUR_LOCAL = 5; //5am
    private static final int MORNING_END_HOUR_LOCAL = 11; //11am

    public static Optional<InsightCard> getInsights(final Long accountId, final DeviceId deviceId, final DeviceDataInsightQueryDAO deviceDataDAO, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {

        //get timezone offset
        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("Could not get timeZoneOffset, not generating humidity insight for accountId {}", accountId);
            return Optional.absent();
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        //integrate night light for week
        final List<DeviceData> nightData = getDeviceData(accountId, deviceId, deviceDataDAO, timeZoneOffset, NIGHT_START_HOUR_LOCAL, NIGHT_END_HOUR_LOCAL);
        final Integer nightIntegral = integrateLight(nightData);

        //integrate morning light for week
        final List<DeviceData> morningData =getDeviceData(accountId, deviceId, deviceDataDAO, timeZoneOffset, MORNING_START_HOUR_LOCAL, MORNING_END_HOUR_LOCAL);
        final Integer morningIntegral = integrateLight(morningData);

        //find night/morning
        final Float nightToMorningRatio = getNightToMorningRatio(nightIntegral, morningIntegral);

//        log transform
//        this is used to calculate percentile, but we are not including percentile for now
//        final Integer nightToMorningRatioLogTransform = getNightToMorningRatioLogTransform(nightToMorningRatio);

        //score card (get percentile, grab corresponding text, return insight card)
        final Optional<InsightCard> insightCard = scoreInsightCard(accountId, nightToMorningRatio);
        return insightCard;
    }

    @VisibleForTesting
    public static Optional<InsightCard> scoreInsightCard(final Long accountId, final Float nightRatio) {

        final Text text;
        if (nightRatio <= 1) {
            final Float morningRatio = (float) 1.0 / nightRatio;
            text = BedLightIntensityMsgEN.getGoodHabits(morningRatio.intValue());
        } else if (nightRatio <= 2) {
            text = BedLightIntensityMsgEN.getMoreThanOne(nightRatio.intValue());
        } else if (nightRatio <= 3) {
            text = BedLightIntensityMsgEN.getMoreThanTwo(nightRatio.intValue());
        } else {
            text = BedLightIntensityMsgEN.getMoreThanThree(nightRatio.intValue());
        }

        return Optional.of(InsightCard.createInsightCards(accountId, text.title, text.message,
                InsightCard.Category.BED_LIGHT_INTENSITY_RATIO, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    /*
    public static final Integer getNightToMorningRatioLogTransform(final Float nightRatio) {
        final Double logTransform = Math.log((nightRatio));
        return (int) Math.round(logTransform);
    }
    */

    @VisibleForTesting
    public static final Float getNightToMorningRatio(final Integer nightSum, final Integer morningSum) {
        final Float nightRatio = (float) nightSum / (float) morningSum;
        return nightRatio;
    }

    public static final Integer integrateLight(final List<DeviceData> deviceDatum) {

        final DescriptiveStatistics lights = new DescriptiveStatistics();
        for (DeviceData deviceData : deviceDatum) {
            lights.addValue(deviceData.ambientLight);
        }

        final Integer totalLight = (int) lights.getSum();
        return totalLight;
    }

    private static final List<DeviceData> getDeviceData(final Long accountId, final DeviceId deviceId, final DeviceDataInsightQueryDAO deviceDataDAO, final Integer timeZoneOffset, final Integer startHour, final Integer endHour) {

        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(0);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.PAST_WEEK);

        final DateTime queryEndTimeLocal = queryEndTime.plusMillis(timeZoneOffset);
        final DateTime queryStartTimeLocal = queryStartTime.plusMillis(timeZoneOffset);

//        TODO: add safeguard for patchy missing data
        final Response<ImmutableList<DeviceData>> response = deviceDataDAO.getBetweenHourDateByTSSameDay(
                accountId, deviceId, queryStartTime, queryEndTime, queryStartTimeLocal, queryEndTimeLocal, startHour, endHour);
        if (response.status == Response.Status.SUCCESS) {
            return response.data;
        }
        return Lists.newArrayList();
    }

}