package com.hello.suripu.core.processors.insights;

import com.hello.suripu.core.models.Insights.Message.PartnerMotionMsgEN;
import com.hello.suripu.core.models.Insights.Message.Text;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 11/4/15.
 */
public class PartnerMotionInsightsTest {

    @Test
    public void test_getInsightText_badPartner() {
        final Float myMotionTtl = 100.0f;
        final Float partnerMotionTtl = 120.f;

        final Text result = PartnerMotionInsight.getInsightText(myMotionTtl, partnerMotionTtl);

        final String resultTitle = result.title;
        final String expectedResultTitle = PartnerMotionMsgEN.getBadPartner(20).title;
        assertThat(resultTitle, is(expectedResultTitle));

        final String resultText = result.message;
        final String expectedResultText = PartnerMotionMsgEN.getBadPartner(20).message;
        assertThat(resultText, is(expectedResultText));
//        System.out.print(resultText);
    }

    @Test
    public void test_getInsightText_egalitarian() {
        final Float myMotionTtl = 100.0f;
        final Float partnerMotionTtl = 100.f;

        final Text result = PartnerMotionInsight.getInsightText(myMotionTtl, partnerMotionTtl);

        final String resultTitle = result.title;
        final String expectedResultTitle = PartnerMotionMsgEN.getEgalitarian().title;
        assertThat(resultTitle, is(expectedResultTitle));

        final String resultText = result.message;
        final String expectedResultText = PartnerMotionMsgEN.getEgalitarian().message;
        assertThat(resultText, is(expectedResultText));
//        System.out.print(resultText);
    }

    @Test
    public void test_getInsightText_badMe() {
        final Float myMotionTtl = 120.0f;
        final Float partnerMotionTtl = 100.f;

        final Text result = PartnerMotionInsight.getInsightText(myMotionTtl, partnerMotionTtl);

        final String resultTitle = result.title;
        final String expectedResultTitle = PartnerMotionMsgEN.getBadMe(20).title;
        assertThat(resultTitle, is(expectedResultTitle));

        final String resultText = result.message;
        final String expectedResultText = PartnerMotionMsgEN.getBadMe(20).message;
        assertThat(resultText, is(expectedResultText));
//        System.out.print(resultText);
    }
}
