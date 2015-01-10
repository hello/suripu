package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by kingshy on 1/5/15.
 */
public class TemperatureHumidity {
    private static final int IDEAL_TEMP_MIN = 60;
    private static final int IDEAL_TEMP_MAX = 70;
    private static final int BAD_TEMP_MIN = 75;
    private static final int BAD_TEMP_MAX = 54;

    private static final int TEMP_START_HOUR = 23; // 11pm
    private static final int TEMP_END_HOUR = 6; // 6am

    public static Optional<InsightCard> getInsights(final Long accountId, final Long deviceId, final DeviceDataDAO deviceDataDAO) {
        final DateTime queryEndTime = DateTime.now(DateTimeZone.UTC).withHourOfDay(TEMP_END_HOUR); // today 6pm
        final DateTime queryStartTime = queryEndTime.minusDays(3).withHourOfDay(TEMP_START_HOUR);

        return Optional.absent();

    }
}
