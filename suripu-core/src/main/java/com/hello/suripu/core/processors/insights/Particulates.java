package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.CalibrationDAO;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.ParticulatesAnomalyMsgEN;
import com.hello.suripu.core.models.Insights.Message.ParticulatesLevelMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.util.DataUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

public class Particulates {
    //https://hello.hackpad.com/Study-of-fine-Particulates-and-contribution-BhCh9eV2qE5
    //https://hello.hackpad.com/Dust-sensor-data-mapping-employing-offset-calibration-n9EJHA22Q20

    //computed from weighted avg of EPA recommended levels for PM2.5 and PM10
    public static final Float PARTICULATE_DENSITY_MAX_IDEAL = 80.0f; // µg/m³
    public static final Float PARTICULATE_DENSITY_MAX_WARNING = 250.0f; // µg/m³

    public static final Float PARTICULATE_SIG_DIFF = 10.0f;

    private static final Integer NUM_DAYS = 7;

    public static Optional<InsightCard> getInsights(final Long accountId, final DeviceId deviceId, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB, final DeviceDataInsightQueryDAO deviceDataDAO, final CalibrationDAO calibrationDAO) {

        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            return Optional.absent(); //cannot compute insight without timezone info
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        final List<Float> dustList = getAvgAirQualityList(accountId, deviceId, NUM_DAYS, timeZoneOffset, deviceDataDAO, calibrationDAO);
        if (dustList.isEmpty()) {
            return Optional.absent();
        }

        final Float currentDust = dustList.get(dustList.size() - 1);
        final Float historyDust = getHistoryDust(dustList, currentDust);

        final Float dustDiff = currentDust - historyDust;

        final Text text;
        if (Math.abs(dustDiff) < PARTICULATE_SIG_DIFF) {
            text = getConstantText(currentDust);
        } else {
            text = getAnomalyText(currentDust, historyDust, dustDiff);
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.AIR_QUALITY, InsightCard.TimePeriod.MONTHLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

    private static List<Float> getAvgAirQualityList(final Long accountId, final DeviceId deviceId, final Integer num_days, final Integer timeZoneOffset, final DeviceDataInsightQueryDAO deviceDataDAO, final CalibrationDAO calibrationDAO) {

        final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay(); //start of day time?
        final DateTime queryStartTime = queryEndTime.minusDays(num_days);

        final DateTime queryEndTimeLocal = queryEndTime.plusMillis(timeZoneOffset);
        final DateTime queryStartTimeLocal = queryStartTime.plusMillis(timeZoneOffset);

        //Grab all night-time data for past week
        final Optional<Calibration> calibrationOptional = calibrationDAO.getStrict(deviceId.toString());
        return getAirQualityList(deviceDataDAO, accountId, deviceId, queryStartTime, queryEndTime, queryStartTimeLocal, queryEndTimeLocal, calibrationOptional);
    }

    @VisibleForTesting
    public static Float getHistoryDust(final List<Float> dustList, final Float currentDust) {
        if (dustList.size() <= 1) {
            return currentDust;
        }

        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final Float dustReading : dustList.subList(0, dustList.size() - 1)) {
            stats.addValue(dustReading);
        }

        return (float) stats.getMean();
    }

    @VisibleForTesting
    public static Text getConstantText(final Float currentDust) {

        final Text text;
        if (currentDust < PARTICULATE_DENSITY_MAX_IDEAL) {
            text = ParticulatesLevelMsgEN.getAirIdeal();
        } else if (currentDust < PARTICULATE_DENSITY_MAX_WARNING) {
            text = ParticulatesLevelMsgEN.getAirHigh();
        } else {
            text = ParticulatesLevelMsgEN.getAirWarningHigh();
        }

        return text;
    }

    @VisibleForTesting
    public static Text getAnomalyText(final Float currentDust, final Float historyDust, final Float dustDiff) {
        //Cases of dustDiff \in [-PARTICULATE_SIG_DIFF, +PARTICULATE_SIG_DIFF] should never be passed into this function

        final Text text;
        if (dustDiff <= -PARTICULATE_SIG_DIFF) {
            final Integer percent =  (int) (-100.0f * dustDiff /  historyDust); //percent is positive
            text = ParticulatesAnomalyMsgEN.getAirImprovement(currentDust.intValue(), historyDust.intValue(), percent);
        } else if (dustDiff <= 2 * PARTICULATE_SIG_DIFF) {
            final Integer percent = (int) (100.0f * dustDiff / historyDust);
            text = ParticulatesAnomalyMsgEN.getAirWorse(currentDust.intValue(), historyDust.intValue(), percent);
        } else {
            final Integer percent = (int) (100.0f * dustDiff / historyDust);
            text = ParticulatesAnomalyMsgEN.getAirVeryWorse(currentDust.intValue(), historyDust.intValue(), percent);
        }
        return text;
    }


    public static ImmutableList<Float> getAirQualityList(final DeviceDataInsightQueryDAO deviceDataDAO,
                                                         final Long accountId,
                                                         final DeviceId deviceId,
                                                         final DateTime startTimestamp,
                                                         final DateTime endTimestamp,
                                                         final DateTime startLocalTimestamp,
                                                         final DateTime endLocalTimestamp,
                                                         final Optional<Calibration> calibrationOptional) {

        final Response<ImmutableList<Integer>> response = deviceDataDAO.getAirQualityRawList(accountId, deviceId, startTimestamp, endTimestamp, startLocalTimestamp, endLocalTimestamp);

        final List<Float> airQualityList = Lists.newArrayList();
        if (response.data.isEmpty() || response.status != Response.Status.SUCCESS) {
            return ImmutableList.copyOf(airQualityList);
        }

        for (Integer airQualityRaw : response.data) {
            airQualityList.add(DataUtils.convertRawDustCountsToDensity(airQualityRaw, calibrationOptional));
        }

        return ImmutableList.copyOf(airQualityList);
    }
}
