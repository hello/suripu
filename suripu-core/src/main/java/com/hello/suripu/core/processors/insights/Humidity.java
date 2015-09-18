package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.models.AggregateSleepStats;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.HumidityMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.util.DateTimeUtil;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by jyfan on 9/16/15.
 */
public class Humidity {
    private static final Logger LOGGER = LoggerFactory.getLogger(Humidity.class);

    //Make sure these are consistent with TemperatureHumidity.java
    public static final int ALERT_HUMIDITY_LOW = 20;
    public static final int ALERT_HUMIDITY_HIGH = 70;

    public static final int IDEAL_HUMIDITY_MIN = 30;
    public static final int IDEAL_HUMIDITY_MAX = 60;

    private static final Integer PRE_BED_BEGIN_HOUR_LOCAL = 21; // 9pm
    private static final Integer PRE_BED_END_HOUR_LOCAL = 1; // 2am
    //Do not set PRE_BED_END_HOUR_LOCAL before midnight or sql query will need to change

    public static Optional<InsightCard> getInsights(final Long accountId,
                                                    final Long deviceId,
                                                    final DeviceDataDAO deviceDataDAO,
                                                    final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {


        final Optional<Integer> timeZoneOffsetOptional = getTimeZoneOffsetOptional(sleepStatsDAODynamoDB, accountId, DateTime.now());
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("Could not get timeZoneOffset, not generating humidity insight for accountId {}", accountId);
            return Optional.absent();
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        final List<DeviceData> deviceDatas = getDeviceData(accountId, deviceId, deviceDataDAO, timeZoneOffset);
        if (deviceDatas.isEmpty()) {
            return Optional.absent();
        }

        final Integer humMedian = getMedianHumidity(deviceDatas);

        final Optional<InsightCard> card = processData(accountId, humMedian);
        return card;
    }

    @VisibleForTesting
    public static Optional<InsightCard> processData(final Long accountId, final Integer medianHumidity) {

        final Text text;
        if (medianHumidity < ALERT_HUMIDITY_LOW) {
            text = HumidityMsgEN.getVeryLowHumidity();
        }
        else if (medianHumidity < IDEAL_HUMIDITY_MIN) {
            text = HumidityMsgEN.getLowHumidity();
        }
        else if (medianHumidity < IDEAL_HUMIDITY_MAX) {
            text = HumidityMsgEN.getIdealHumidity();
        }
        else if (medianHumidity < ALERT_HUMIDITY_HIGH) {
            text = HumidityMsgEN.getHighHumidity();
        }
        else {
            text = HumidityMsgEN.getVeryHighHumidity();
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.HUMIDITY, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC)));
    }

    @VisibleForTesting
    public static Integer getMedianHumidity(final List<DeviceData> data) {

        final DescriptiveStatistics humStats = new DescriptiveStatistics();
        for (final DeviceData deviceData : data) {
            humStats.addValue(deviceData.ambientHumidity);
        }

        final Integer medianHumidity = (int) humStats.getPercentile(50);
        return medianHumidity;
    }

    private static final List<DeviceData> getDeviceData(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final Integer timeZoneOffset) {

        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(PRE_BED_BEGIN_HOUR_LOCAL);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.PAST_WEEK);

        //Grab all pre-bed data for past week
        return deviceDataDAO.getBetweenHourDateByTS(accountId, deviceId, queryStartTime, queryEndTime, PRE_BED_BEGIN_HOUR_LOCAL, PRE_BED_END_HOUR_LOCAL);
    }

    private static final Optional<Integer> getTimeZoneOffsetOptional(final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final Long accountId, final DateTime queryEndDate) {
        final String sleepStatsQueryEndDate = DateTimeUtil.dateToYmdString(queryEndDate);
        final String sleepStatsQueryStartDate = DateTimeUtil.dateToYmdString(queryEndDate.minusDays(1));

        final List<AggregateSleepStats> sleepStats = sleepStatsDAODynamoDB.getBatchStats(accountId, sleepStatsQueryStartDate, sleepStatsQueryEndDate);

        if (!sleepStats.isEmpty()) {
            return Optional.of(sleepStats.get(0).offsetMillis);
        }

        LOGGER.debug("SleepStats empty, fail to retrieve timeZoneOffset for accountId {} from {} to {}", accountId, sleepStatsQueryStartDate, sleepStatsQueryEndDate);
        return Optional.absent();
    }

}
