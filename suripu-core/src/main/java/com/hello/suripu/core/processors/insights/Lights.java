package com.hello.suripu.core.processors.insights;

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
import com.hello.suripu.core.models.Insights.Message.LightMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * Created by kingshy on 1/5/15.
 */
public class Lights {

    private static final Logger LOGGER = LoggerFactory.getLogger(Lights.class);

    private static final int NIGHT_START_HOUR = 18; // 6pm
    private static final int NIGHT_END_HOUR = 1; // 1am

    private static final int LIGHT_ON_THRESHOLD_LUX = 1;

    //TODO: move the below public variables somewhere else
    public static final float LIGHT_LEVEL_WARNING = 2.0f;  // in lux
    public static final float LIGHT_LEVEL_ALERT = 8.0f;  // in lux

    public static Optional<InsightCard> getInsights(final Long accountId,
                                                    final DeviceAccountPair deviceAccountPair,
                                                    final Optional<Device.Color> colorOptional,
                                                    final Optional<Calibration> calibrationOptional,
                                                    final DeviceDataInsightQueryDAO deviceDataDAO,
                                                    final LightData lightData,
                                                    final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {

        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            LOGGER.debug("action=insight-absent insight=lights reason=timezoneoffset-absent account_id={}", accountId);
            return Optional.absent(); //cannot compute insight without timezone info
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        // get light data for last day, filter by time
        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(NIGHT_START_HOUR);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.ONE_DAY);

        final DateTime queryEndTimeLocal = queryEndTime.plusMillis(timeZoneOffset);
        final DateTime queryStartTimeLocal = queryStartTime.plusMillis(timeZoneOffset);

        final List<DeviceData> rows;
        final DeviceId deviceId = DeviceId.create(deviceAccountPair.externalDeviceId);
        final Response<ImmutableList<DeviceData>> response = deviceDataDAO.getBetweenHourDateByTS(
                accountId, deviceId, queryStartTime, queryEndTime,
                queryStartTimeLocal, queryEndTimeLocal, NIGHT_START_HOUR, NIGHT_END_HOUR);

        if (response.status == Response.Status.SUCCESS) {
            rows = response.data;
        } else {
            rows = Lists.newArrayList();
        }


        final Optional<InsightCard> card = processLightData(accountId, rows, colorOptional, calibrationOptional, lightData);
        return card;
    }

    public static Optional<InsightCard> processLightData(final Long accountId,
                                                         final List<DeviceData> data,
                                                         final Optional<Device.Color> colorOptional,
                                                         final Optional<Calibration> calibrationOptional,
                                                         final LightData lightData) {

        if (data.size() == 0) {
            LOGGER.debug("action=insight-absent insight=lights reason=data-empty account_id={}", accountId);
            return Optional.absent();
        }

        // compute mean value
        final Device.Color color = colorOptional.or(Device.DEFAULT_COLOR);
        int sum = 0;
        int n = 0;
        for (final DeviceData deviceData : data) {
            final CalibratedDeviceData calibratedDeviceData = new CalibratedDeviceData(deviceData, color, calibrationOptional);
            if (calibratedDeviceData.lux() < LIGHT_ON_THRESHOLD_LUX) {
                continue;
            }
            sum += calibratedDeviceData.lux();
            n += 1;
        }

        final int meanLight = (int) ((float) sum / (float) n);
        final int percentile = lightData.getLightPercentile( meanLight);

        // see: http://en.wikipedia.org/wiki/Lux and http://www.greenbusinesslight.com/page/119/lux-lumens-and-watts
        // TODO: refine levels
        Text text;
        if (meanLight <= 2) {
            text = LightMsgEN.getLightDark(meanLight, percentile);
        } else if (meanLight <= 5) {
            text = LightMsgEN.getLightNotDarkEnough(meanLight, percentile);
        } else if (meanLight <= 20) {
            text = LightMsgEN.getLightALittleBright(meanLight, percentile);
        } else if (meanLight <= 40) {
            text = LightMsgEN.getLightQuiteBright(meanLight, percentile);
        } else if (meanLight <= 100) {
            text = LightMsgEN.getLightTooBright(meanLight, percentile);
        } else {
            text = LightMsgEN.getLightWayTooBright(meanLight, percentile);
        }

        return Optional.of(InsightCard.createBasicInsightCard(accountId, text.title, text.message,
                InsightCard.Category.LIGHT, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC), InsightCard.InsightType.DEFAULT));
    }

}
