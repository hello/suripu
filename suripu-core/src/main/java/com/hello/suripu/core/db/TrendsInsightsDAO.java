package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.mappers.InfoInsightCardsMapper;
import com.hello.suripu.core.insights.InfoInsightCards;
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

    // Insights Stuff

    public List<InfoInsightCards> getGenericInsightCardsByCategory(final String category) {
        final List<Optional<InfoInsightCards>> optionalInfoInsightCards = this.getGenericOptionalInsightCardsByCategory(category);

        final List<InfoInsightCards> infoInsightCards = filterAbsentCards(optionalInfoInsightCards);
        return infoInsightCards;
    }

    @RegisterMapper(InfoInsightCardsMapper.class)
    @SqlQuery("SELECT * FROM info_insight_cards WHERE category = (CAST(:category AS INSIGHT_CATEGORY)) ORDER BY id DESC LIMIT 1")
    public abstract List<Optional<InfoInsightCards>> getGenericOptionalInsightCardsByCategory(@Bind("category") final String category);


    public ImmutableList<InfoInsightCards> getAllGenericInsightCards() {
        final List<Optional<InfoInsightCards>> optionalInfoInsightCards = this.getAllGenericOptionalInsightCards();

        final List<InfoInsightCards> InfoInsightCards = filterAbsentCards(optionalInfoInsightCards);
        return ImmutableList.copyOf(InfoInsightCards);
    }

    @RegisterMapper(InfoInsightCardsMapper.class)
    @SqlQuery("SELECT * FROM info_insight_cards ORDER BY id DESC")
    public abstract List<Optional<InfoInsightCards>> getAllGenericOptionalInsightCards();


    private static List<InfoInsightCards> filterAbsentCards(final List<Optional<InfoInsightCards>> optionalInfoInsightCards) {
        final List<InfoInsightCards> infoInsightCards = Lists.newArrayList();

        for (Optional<InfoInsightCards> optionalInsightCard : optionalInfoInsightCards) {
            if (!optionalInsightCard.isPresent()) {
                continue;
            }
            infoInsightCards.add(optionalInsightCard.get());
        }
        return infoInsightCards;
    }
}
