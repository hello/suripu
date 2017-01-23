package com.hello.suripu.core.processors.insights;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.CalibratedDeviceData;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.TemperatureMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import com.hello.suripu.core.preferences.TemperatureUnit;
import com.hello.suripu.core.util.DataUtils;
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

    public static Optional<InsightCard> getInsights(final Long accountId,
                                                    final DeviceAccountPair deviceAccountPair,
                                                    final Optional<Device.Color> colorOptional,
                                                    final Optional<Calibration> calibrationOptional,
                                                    final DeviceDataInsightQueryDAO deviceDataDAO,
                                                    final TemperatureUnit tempUnit,
                                                    final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {
        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            return Optional.absent(); //cannot compute insight without timezone info
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        // get light data for last three days, filter by time
        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(TEMP_END_HOUR); // today 6am
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.RECENT_DAYS).withHourOfDay(TEMP_START_HOUR); // 11pm three days ago

        final DateTime queryEndTimeLocal = queryEndTime.plusMillis(timeZoneOffset);
        final DateTime queryStartTimeLocal = queryStartTime.plusMillis(timeZoneOffset);

        final int slotDuration = 30;
        final List<DeviceData> sensorData;
        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);
        final Response<ImmutableList<DeviceData>> response = deviceDataDAO.getBetweenByLocalHourAggregateBySlotDuration(
                accountId, deviceId, queryStartTime, queryEndTime,
                queryStartTimeLocal, queryEndTimeLocal, TEMP_START_HOUR, TEMP_END_HOUR, slotDuration);
        if (response.status == Response.Status.SUCCESS) {
            sensorData = response.data;
        } else {
            sensorData = Lists.newArrayList();
        }

        final Optional<InsightCard> card = processData(accountId, sensorData, colorOptional, calibrationOptional, tempUnit);
        return card;
    }

    @VisibleForTesting
    public static Optional<InsightCard> processData(final Long accountId,
                                                    final List<DeviceData> data,
                                                    final Optional<Device.Color> colorOptional,
                                                    final Optional<Calibration> calibrationOptional,
                                                    final TemperatureUnit tempUnit) {

        if (data.isEmpty()) {
            return Optional.absent();
        }

        // TODO: if location is available, compare with users from the same city
        // TODO: adjust ideal range based on question response on preference for cold/hot. Implemented before by KSG, but removed for simplicity for now.

        final Device.Color color = colorOptional.or(Device.DEFAULT_COLOR);

        // get min, max and average
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final DeviceData deviceData : data) {
            final CalibratedDeviceData calibratedDeviceData = new CalibratedDeviceData(deviceData, color, calibrationOptional);
            stats.addValue(calibratedDeviceData.temperature());
            stats.addValue(DataUtils.calibrateTemperature(deviceData.ambientTemperature));
        }

        final double tmpMedValue = stats.getPercentile(50);
        final int medTempC = (int) tmpMedValue;
        final int medTempF = DataUtils.celsiusToFahrenheit(medTempC);

        LOGGER.debug("Temp for account {}: med {} C; {} F", accountId, medTempC, medTempF);

        // Units for passing into TemperatureMsgEN
        int medTemp = medTempC;
        int idealMin = IDEAL_TEMP_MIN_CELSIUS;
        int idealMax = IDEAL_TEMP_MAX_CELSIUS;
        if (tempUnit == TemperatureUnit.FAHRENHEIT) {
            medTemp = medTempF;
            idealMin = IDEAL_TEMP_MIN;
            idealMax = IDEAL_TEMP_MAX;
        }

        //careful: comparisons are done in user's own units, TemperatureMsgEN also gets passed user's own units.
        Text text;
        if (medTemp >= idealMin && medTemp <= idealMax) {
            text = TemperatureMsgEN.getTempMsgPerfect(medTemp, tempUnit.toString());
        } else if (medTemp < idealMin) {
            text = TemperatureMsgEN.getTempMsgTooCold(medTemp, tempUnit.toString(), idealMin);
        } else {
            text = TemperatureMsgEN.getTempMsgTooHot(medTemp, tempUnit.toString(), idealMax);
        }

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.TEMPERATURE, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }


}
