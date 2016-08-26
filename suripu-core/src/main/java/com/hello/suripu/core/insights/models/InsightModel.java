package com.hello.suripu.core.insights.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.InsightProfile;

public interface InsightModel {

    Optional<InsightCard> generate(InsightProfile insightProfile);
}
