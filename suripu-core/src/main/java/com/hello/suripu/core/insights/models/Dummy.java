package com.hello.suripu.core.insights.models;

import com.google.common.base.Optional;
import com.hello.suripu.core.insights.InsightCard;
import com.hello.suripu.core.insights.InsightProfile;

public class Dummy implements InsightModel {
    @Override
    public Optional<InsightCard> generate(InsightProfile insightProfile) {
        return Optional.absent();
    }
}
