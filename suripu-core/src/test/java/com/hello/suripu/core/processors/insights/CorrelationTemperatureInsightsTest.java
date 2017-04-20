package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.CorrelationTemperatureMsgEN;
import com.hello.suripu.core.processors.insights.CorrelationTemperature;
import com.hello.suripu.core.processors.insights.WakeStdDevData;
import com.hello.suripu.core.processors.insights.WakeVariance;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 8/23/16.
 */
public class CorrelationTemperatureInsightsTest {

    final Long FAKE_ACCOUNT = 1L;

    @Test
    public void testCard() {
        final Optional<InsightCard> insightCardOptional = CorrelationTemperature.getInsights(FAKE_ACCOUNT, DateTime.now());
        assertThat(insightCardOptional.isPresent(), is(Boolean.TRUE));

        final InsightCard insightCard = insightCardOptional.get();
        assertThat(insightCard.category, is(InsightCard.Category.CORRELATION_TEMP));
        assertThat(insightCard.title, is(CorrelationTemperatureMsgEN.TEMP_CORR_MARKETING_TITLE));
        assertThat(insightCard.message, is(CorrelationTemperatureMsgEN.TEMP_CORR_MARKETING_MSG));
    }
}
