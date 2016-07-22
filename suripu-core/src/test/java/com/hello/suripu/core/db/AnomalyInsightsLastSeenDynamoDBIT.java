package com.hello.suripu.core.db;

import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Insights.AnomalyInsightsLastSeen;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
/**
 * Created by jarredheinrich on 7/21/16.
 */
public class AnomalyInsightsLastSeenDynamoDBIT extends DynamoDBIT<AnomalyInsightsLastSeenDynamoDB> {

    @Override
    protected CreateTableResult createTable() {
        return dao.createTable(2L, 2L);
    }

    @Override
    protected AnomalyInsightsLastSeenDynamoDB createDAO() {
        return new AnomalyInsightsLastSeenDynamoDB(amazonDynamoDBClient, TABLE_NAME);
    }

    @Test
    public void testUpdateAndGetAnomaly() throws Exception {
        final Long accoundId = 1L;
        final DateTime dateTimePast = DateTime.now(DateTimeZone.UTC).minusDays(1);
        final DateTime dateTimeNow = DateTime.now(DateTimeZone.UTC);
        final InsightCard.Category category = InsightCard.Category.SLEEP_HYGIENE;
        AnomalyInsightsLastSeen anomalyInsight= new AnomalyInsightsLastSeen(accoundId, Optional.of(category), Optional.of(dateTimePast));

        Boolean insertResult = dao.upsertAnomalyInsightsLastSeen(anomalyInsight);
        assertThat(insertResult, is(true));

        ImmutableList<AnomalyInsightsLastSeen> retrievedAnomaly = dao.getAnomalyInsightsByAccountId(accoundId);
        assertThat(retrievedAnomaly.get(0).updatedUTC.get(), is(dateTimePast.withTimeAtStartOfDay()));
        assertThat(retrievedAnomaly.get(0).seenCategory.get(), is(category));
        assertThat(retrievedAnomaly.get(0).accountId, is(accoundId));


        anomalyInsight= new AnomalyInsightsLastSeen(accoundId, Optional.of(category), Optional.of(dateTimeNow));
        insertResult = dao.upsertAnomalyInsightsLastSeen(anomalyInsight);
        assertThat(insertResult, is(true));

        retrievedAnomaly = dao.getAnomalyInsightsByAccountId(accoundId);
        assertThat(retrievedAnomaly.get(0).updatedUTC.get(), is(dateTimeNow.withTimeAtStartOfDay()));
        assertThat(retrievedAnomaly.get(0).seenCategory.get(), is(category));
        assertThat(retrievedAnomaly.get(0).accountId, is(accoundId));
    }
    @Test
    public void testGetLatestParameters() throws Exception {
        final Long accoundId = 1L;
        final DateTime dateTime = DateTime.now(DateTimeZone.UTC);
        final InsightCard.Category category1 = InsightCard.Category.SLEEP_HYGIENE;
        final InsightCard.Category category2 = InsightCard.Category.SLEEP_DURATION;
        final InsightCard.Category category3 = InsightCard.Category.AIR_QUALITY;

        ImmutableList<AnomalyInsightsLastSeen> results = dao.getAnomalyInsightsByAccountId(accoundId);
        assertThat(results.size(), is(1));
        assertThat(results.get(0).seenCategory.isPresent(), is(false));
        assertThat(results.get(0).updatedUTC.isPresent(), is(false));

        Boolean insertResult;
        insertResult = dao.upsertAnomalyInsightsLastSeen(new AnomalyInsightsLastSeen (accoundId, Optional.of(category1), Optional.of(dateTime.minusDays(2))));
        assertThat(insertResult, is(true));

        insertResult = dao.upsertAnomalyInsightsLastSeen(new AnomalyInsightsLastSeen(accoundId, Optional.of(category2), Optional.of(dateTime.minusDays(1))));
        assertThat(insertResult, is(true));

        insertResult = dao.upsertAnomalyInsightsLastSeen(new AnomalyInsightsLastSeen(accoundId, Optional.of(category2), Optional.of(dateTime)));
        assertThat(insertResult, is(true));

        results = dao.getAnomalyInsightsByAccountId(accoundId);
        assertThat(results.size(), is(2));
        assertThat(results.get(0).seenCategory.get(), is(category1));
        assertThat(results.get(0).updatedUTC.get(), is(dateTime.minusDays(2).withTimeAtStartOfDay()));
        assertThat(results.get(1).seenCategory.get(), is(category2));
        assertThat(results.get(1).updatedUTC.get(), is(dateTime.withTimeAtStartOfDay()));

        AnomalyInsightsLastSeen result = dao.getAnomalyInsightsByAccountIdAndCategory(accoundId,category1);
        assertThat(result.seenCategory.isPresent(), is(true));
        if(result.seenCategory.isPresent()){
            assertThat(result.seenCategory.get(), is(category1));
            assertThat(result.updatedUTC.get(), is(dateTime.minusDays(2).withTimeAtStartOfDay()));
        }

        result = dao.getAnomalyInsightsByAccountIdAndCategory(accoundId,category3);
        assertThat(result.seenCategory.isPresent(), is(false));


    }

}
