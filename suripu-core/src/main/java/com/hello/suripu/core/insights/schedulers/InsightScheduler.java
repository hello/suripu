package com.hello.suripu.core.insights.schedulers;

import com.google.common.base.Optional;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.InsightProfile;
import org.joda.time.DateTime;

import java.util.Map;

public interface InsightScheduler {

    Optional<InsightCard.Category> schedule(final Map<InsightCard.Category, DateTime> recentCategories, final InsightProfile profile);

    void update(final InsightProfile profile, InsightCard.Category category);
}
