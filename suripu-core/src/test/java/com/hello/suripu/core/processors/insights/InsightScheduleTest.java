package com.hello.suripu.core.processors.insights;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.hello.suripu.core.db.InsightScheduleDAO;
import com.hello.suripu.core.db.InsightSchedulesFromResource;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.InsightSchedule;
import com.hello.suripu.core.models.Insights.InsightScheduleMap;
import com.hello.suripu.core.models.Insights.InsightScheduleMonth;
import com.hello.suripu.core.models.Insights.InsightScheduleYear;
import org.junit.Test;

import java.util.Map;

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
    public void testLoadInsightSchedulesFromResources() {
        final String insightScheduleLocation = "insights";

        final Integer year = 2016;
        final Integer month = 2;

        final InsightScheduleDAO insightScheduleDAO = InsightSchedulesFromResource.create(insightScheduleLocation, year, month);
        InsightSchedule insightSchedule = insightScheduleDAO.getInsightScheduleDefault();

        Map<Integer, Map<Integer, Map<Integer, InsightCard.Category>>> insightScheduleMap = insightSchedule.insightScheduleMap;
        InsightCard.Category cat = insightScheduleMap.get(year).get(month).get(1);
//         insightScheduleYear = insightScheduleMap.get(year);
//        InsightScheduleMonth insightScheduleMonth = insightScheduleYear.monthToDay.get(month);
//        InsightCard.Category cat = insightScheduleMonth.dayToCategoryMap.get(1);
//
        assertThat(cat, is(InsightCard.Category.WAKE_VARIANCE));

//        InsightCard.Category catNull = insightSchedule.yearToMonthMapMap.get(year).get(month).get(50);
        InsightCard.Category catNull = insightScheduleMap.get(year).get(month).get(50);
        assertThat(catNull, is(nullValue()));
    }
}
