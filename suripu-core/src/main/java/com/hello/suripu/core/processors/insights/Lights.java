package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.DeviceDataInsightQueryDAO;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.responses.DeviceDataResponse;
import com.hello.suripu.core.db.responses.Response;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.DeviceId;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.LightMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;

/**
 * Created by kingshy on 1/5/15.
 */
public class Lights {

    private static final int NIGHT_START_HOUR = 18; // 6pm
    private static final int NIGHT_END_HOUR = 1; // 1am

    public static final float LIGHT_LEVEL_WARNING = 2.0f;  // in lux
    public static final float LIGHT_LEVEL_ALERT = 8.0f;  // in lux

    public static Optional<InsightCard> getInsights(final Long accountId, final DeviceId deviceId, final DeviceDataInsightQueryDAO deviceDataDAO,
                                                    final LightData lightData, final SleepStatsDAODynamoDB sleepStatsDAODynamoDB) {

        final Optional<Integer> timeZoneOffsetOptional = sleepStatsDAODynamoDB.getTimeZoneOffset(accountId);
        if (!timeZoneOffsetOptional.isPresent()) {
            return Optional.absent(); //cannot compute insight without timezone info
        }
        final Integer timeZoneOffset = timeZoneOffsetOptional.get();

        // get light data for last three days, filter by time
        final DateTime queryEndTime = DateTime.now(DateTimeZone.forOffsetMillis(timeZoneOffset)).withHourOfDay(NIGHT_START_HOUR);
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.RECENT_DAYS);

        final DateTime queryEndTimeLocal = queryEndTime.plusMillis(timeZoneOffset);
        final DateTime queryStartTimeLocal = queryStartTime.plusMillis(timeZoneOffset);

        // get light data > 0 between the hour of 6pm to 1am
        final List<DeviceData> rows;
        final DeviceDataResponse response = deviceDataDAO.getLightByBetweenHourDateByTS(accountId, deviceId, 0, queryStartTime, queryEndTime, queryStartTimeLocal, queryEndTimeLocal, NIGHT_START_HOUR, NIGHT_END_HOUR);
        if (response.status == Response.Status.SUCCESS) {
            rows = response.data;
        } else {
            rows = Lists.newArrayList();
        }

        final Optional<InsightCard> card = processLightData(accountId, rows, lightData);
        return card;
    }

    public static Optional<InsightCard> processLightData(final Long accountId, final List<DeviceData> data, final LightData lightData) {

        if (data.size() == 0) {
            return Optional.absent();
        }

        // compute median value
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        for (final DeviceData deviceData : data) {
            stats.addValue(deviceData.ambientLight);
        }

        int medianLight = 0;
        if (stats.getSum() > 0) {
            medianLight = (int) stats.getPercentile(50); // median
        }

        final int percentile = lightData.getLightPercentile(medianLight);

        // see: http://en.wikipedia.org/wiki/Lux and http://www.greenbusinesslight.com/page/119/lux-lumens-and-watts
        // todo: refine levels
        Text text;
        if (medianLight <= 1) {
            text = LightMsgEN.getLightDark(medianLight, percentile);
        } else if (medianLight <= 4) {
            text = LightMsgEN.getLightNotDarkEnough(medianLight, percentile);
        } else if (medianLight <= 20) {
            text = LightMsgEN.getLightALittleBright(medianLight, percentile);
        } else if (medianLight <= 40) {
            text = LightMsgEN.getLightQuiteBright(medianLight, percentile);
        } else if (medianLight <= 100) {
            text = LightMsgEN.getLightTooBright(medianLight, percentile);
        } else {
            text = LightMsgEN.getLightWayTooBright(medianLight, percentile);
        }

        return Optional.of(new InsightCard(accountId, text.title, text.message,
                InsightCard.Category.LIGHT, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC)));
    }

}
