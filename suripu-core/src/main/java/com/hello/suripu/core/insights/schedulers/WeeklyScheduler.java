package com.hello.suripu.core.insights.schedulers;

import com.google.common.base.Optional;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.InsightProfile;
import com.hello.suripu.core.insights.InsightsLastSeen;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class WeeklyScheduler implements InsightScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyScheduler.class);
    private static final int LAST_TWO_WEEKS = 13; //last 2 weeks

    @Override
    public Optional<InsightCard.Category> schedule(Map<InsightCard.Category, DateTime> recentCategories, InsightProfile profile) {
        //Generate some Insights weekly
        final Integer dayOfWeek = profile.utcnow().getDayOfWeek();
        LOGGER.debug("The day of week is {}", dayOfWeek);

        switch (dayOfWeek) {
            case 6:
                if (!InsightsLastSeen.checkQualifiedInsight(recentCategories, InsightCard.Category.WAKE_VARIANCE, LAST_TWO_WEEKS)) {
                    return Optional.absent();
                }
                return Optional.of(InsightCard.Category.WAKE_VARIANCE);
        }
        return Optional.absent();
    }

    @Override
    public void update(InsightProfile profile, InsightCard.Category category) {

    }
}
