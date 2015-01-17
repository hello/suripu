package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Insights.InfoInsightCards;
import com.hello.suripu.core.models.Insights.InsightCard;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by kingshy on 1/15/15.
 */
public class InfoInsightCardsMapper implements ResultSetMapper<InfoInsightCards> {
    @Override
    public InfoInsightCards map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        return new InfoInsightCards(r.getInt("id"),
                InsightCard.Category.fromString(r.getString("category")),
                r.getString("title"),
                r.getString("text"),
                r.getString("image_url"));
    }

}
