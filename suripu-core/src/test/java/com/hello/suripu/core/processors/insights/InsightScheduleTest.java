package com.hello.suripu.core.processors.insights;

import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.InsightSchedule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 2/19/16.
 */
public class InsightScheduleTest {

    @Test
    public void testLoadInsightSchedule() {
        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.CBTI_V1;
        final Integer year = 2016;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(1);
        assertThat(cat, is(InsightCard.Category.WAKE_VARIANCE));
    }

    @Test
    public void testLoadInsightsSchedule_empty() {
        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.DEFAULT;
        final Integer year = 2016;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(4);
        assertThat(cat, is(nullValue()));
    }

    @Test
    public void testLoadInsightsSchedule_empty2() {
        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.CBTI_V1;
        final Integer year = -2000;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(4);
        assertThat(cat, is(nullValue()));
    }

    @Test
    public void testLoadInsightsSchedule_empty3() {
        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.CBTI_V1;
        final Integer year = 2016;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(50);
        assertThat(cat, is(nullValue()));
    }
}
