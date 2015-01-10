package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.DeviceDataDAO;
import com.hello.suripu.core.models.Insights.InsightCard;

/**
 * Created by kingshy on 1/5/15.
 */
public class TemperatureHumidity {
    public static Optional<InsightCard> getInsights(final Long accountId, final DeviceDAO deviceDAO, final DeviceDataDAO deviceDataDAO) {
        return Optional.absent();
    }
}
