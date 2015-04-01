package com.hello.suripu.core.db;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.InfoInsightCardsMapper;
import com.hello.suripu.core.db.mappers.SleepStatsSampleMapper;
import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.SleepStatsSample;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by kingshy on 12/17/14.
 * Assumption:
 * 1. sleep score and duration data are updated once a day, assume that it's always for the previous night
 */
public abstract class TrendsInsightsDAO {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendsInsightsDAO.class);

    @RegisterMapper(SleepStatsSampleMapper.class)
    @SqlQuery("SELECT * FROM sleep_stats_time WHERE account_id = :account_id AND duration > 0" +
            "ORDER BY local_utc_date LIMIT :days")
    public abstract ImmutableList<SleepStatsSample> getAccountRecentSleepStats(@Bind("account_id") Long accountId,
                                                                                     @Bind("days") int days);


    // Insights Stuff

    @RegisterMapper(InfoInsightCardsMapper.class)
    @SqlQuery("SELECT * FROM info_insight_cards WHERE category = (CAST(:category AS INSIGHT_CATEGORY)) ORDER BY id DESC LIMIT 1")
    public abstract List<InfoInsightCards> getGenericInsightCardsByCategory(@Bind("category") final String category);

    @RegisterMapper(InfoInsightCardsMapper.class)
    @SqlQuery("SELECT * FROM info_insight_cards ORDER BY id DESC")
    public abstract ImmutableList<InfoInsightCards> getAllGenericInsightCards();
}
