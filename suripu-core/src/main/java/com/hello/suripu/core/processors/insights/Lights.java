package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
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

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO, final LightData lightData) {

        // get light data for last three days, filter by time
        final int slotDurationInMinutes = 1;
        final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).withHourOfDay(NIGHT_START_HOUR); // today 6pm
        final DateTime queryStartTime = queryEndTime.minusDays(3);

        // get light data > 0 between the hour of 6pm to 1am
        final List<DeviceData> rows = deviceDataDAO.getLightByBetweenHourDate(accountId, deviceId, 0, queryStartTime, queryEndTime, NIGHT_START_HOUR, NIGHT_END_HOUR);

        final Optional<InsightCard> card = processLightData(accountId, rows, lightData);
        return card;
    }

    public static Optional<InsightCard> processLightData(final Long accountId, final List<DeviceData> data, final LightData lightData) {

        if (data.size() == 0) {
            return Optional.absent();
        }

        // compute median value TODO: check correct times are used
        final DescriptiveStatistics stats = new DescriptiveStatistics();
        final int offsetMillis = data.get(0).offsetMillis;
        for (final DeviceData deviceData : data) {
            stats.addValue(deviceData.ambientLight);
        }

        int medianLight = 0;
        if (stats.getSum() > 0) {
            medianLight = (int) stats.getPercentile(50); // median
        }

        final int percentile = lightData.getLightPercentile(medianLight);

        String title;
        String message;
        // see: http://en.wikipedia.org/wiki/Lux and http://www.greenbusinesslight.com/page/119/lux-lumens-and-watts
        // todo: refine levels
        if (medianLight <= 1) {
            title = "Hello, Darkness";
            message = String.format("Your bedroom light condition of %d lux is perfect, ", medianLight) +
                    String.format("it is **dimmer than** %d%% of all Sense users.", 100 - percentile);

        } else if (medianLight <= 4) {
            title = "Hello, Dark Room";
            message = String.format("Your bedroom light condition of %d lux is close to ideal, ", medianLight) +
                    String.format("it is **dimmer than** %d%% of all Sense users.", 100 - percentile);

        } else if (medianLight <= 20) {
            title = "Hmm, It's A Little Bright";
            message = String.format("Your bedroom light of %d lux is a little brighter than ideal, ", medianLight) +
                    String.format("it is **brighter than** %d%% of all Sense users. ", percentile) +
                    "Try dimming the light a little before bedtime.";

        } else if (medianLight <= 40) {
            title = "Hello, Light";
            message = String.format("Your bedroom is too bright (%d lux) for ideal sleep conditions, ", medianLight) +
                    String.format("it is **brighter than** %d%% of all Sense users. ", percentile) +
                    "Try changing to a lower wattage light bulb, or turning off the light 15 minutes before bedtime.";

        } else if (medianLight <= 100) {
            title = "Time to Dim It Down";
            message = String.format("Your bedroom light of %d lux is as bright as a warehouse aisle! ", medianLight) +
                    String.format("It is **brighter than** %d%% of all Sense users. ", percentile) +
                    "Changing to a lower wattage light bulb might help improve your sleep";
        } else {
            title = "It's Way Too Bright";
            message = String.format("Your bedroom light of %d lux is as bright as an office room! ", medianLight) +
                    String.format("It's **brighter than** %d%% of all Sense users. ", percentile) +
                    "Changing to a lower wattage light bulb might help improve your sleep";
        }

        return Optional.of(new InsightCard(accountId, title, message,
                InsightCard.Category.LIGHT, InsightCard.TimePeriod.RECENTLY,
                DateTime.now(DateTimeZone.UTC)));
    }

}
