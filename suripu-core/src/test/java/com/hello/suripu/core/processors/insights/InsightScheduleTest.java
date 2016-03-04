package com.hello.suripu.core.processors.insights;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
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

    final ClientConfiguration clientConfiguration = new ClientConfiguration();
    final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
    final AmazonS3Client amazonS3 = new AmazonS3Client(awsCredentialsProvider, clientConfiguration);


    @Test
    public void testLoadInsightSchedule() {
        final String insightScheduleLocation = "insights";

        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.DEFAULT;
        final Integer year = 2016;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(insightScheduleLocation, group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(1);
        assertThat(cat, is(InsightCard.Category.WAKE_VARIANCE));
    }

    @Test
    public void testLoadInsightsSchedule_empty() {
        final String insightScheduleLocation = "hello-prod";

        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.DEFAULT;
        final Integer year = 2016;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(amazonS3, insightScheduleLocation, group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(4);
        assertThat(cat, is(nullValue()));
    }

    @Test
    public void testLoadInsightsSchedule_empty2() {
        final String insightScheduleLocation = "hello-prod";

        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.CBTI_V1;
        final Integer year = -2000;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(amazonS3, insightScheduleLocation, group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(4);
        assertThat(cat, is(nullValue()));
    }

    @Test
    public void testLoadInsightsSchedule_empty3() {
        final String insightScheduleLocation = "hello-prod";

        final InsightSchedule.InsightGroup group = InsightSchedule.InsightGroup.CBTI_V1;
        final Integer year = 2016;
        final Integer month = 2;
        InsightSchedule insightSchedule = InsightSchedule.loadInsightSchedule(amazonS3, insightScheduleLocation, group, year, month);
        InsightCard.Category cat = insightSchedule.dayToCategoryMap.get(50);
        assertThat(cat, is(nullValue()));
    }
}