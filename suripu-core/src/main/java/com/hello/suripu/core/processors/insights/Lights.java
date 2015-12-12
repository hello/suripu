package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.DeviceData;
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

    private static final int LIGHT_ON_LEVEL_RAW = 2621; //raw ambient_light corresponding to 1.0 lux

    //TODO: move the below somewhere else because its not used in this insight
    public static final float LIGHT_LEVEL_WARNING = 2.0f;  // in lux
    public static final float LIGHT_LEVEL_ALERT = 8.0f;  // in lux

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final LightData lightData) {

        // get light data for last three days, filter by time
        final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).withHourOfDay(NIGHT_START_HOUR); // today 6pm
        final DateTime queryStartTime = queryEndTime.minusDays(InsightCard.RECENT_DAYS);

        // get light data > 1 lux between the hour of 6pm to 1am
        final List<DeviceData> rows = deviceDataDAO.getLightByBetweenHourDate(accountId, deviceId, LIGHT_ON_LEVEL_RAW, queryStartTime, queryEndTime, NIGHT_START_HOUR, NIGHT_END_HOUR);

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
