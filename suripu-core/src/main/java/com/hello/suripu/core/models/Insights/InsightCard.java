package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightCard {
    public enum Category {
        GENERIC(0),
        SLEEP_HYGIENE(1),
        LIGHT(2),
        SOUND(3),
        TEMPERATURE(4),
        HUMIDITY(5),
        AIR_QUALITY(6),
        SLEEP_DURATION(7),
        TIME_TO_SLEEP(8),
        SLEEP_TIME(9),
        WAKE_TIME(10),
        WORKOUT(11),
        CAFFEINE(12),
        ALCOHOL(13),
        DIET(14),
        DAYTIME_SLEEPINESS(15),
        DAYTIME_ACTIVITIES(16),
        SLEEP_SCORE(17),
        SLEEP_QUALITY(18);

        private int value;

        private Category(final int value) {this.value = value;}

        public int getValue() {return this.value;}
    }

    public enum TimePeriod {
        NONE(0),
        DAILY(1), // tracks daily changes
        RECENTLY(2), // moving average of 3 days
        WEEKLY(3),
        BI_WEEKLY(4),
        MONTHLY(5),
        QUARTERLY(6),
        ANNUALLY(7),
        SUMMARY(8);  // ALL data

        private int value;

        private TimePeriod(final int value) {this.value = value;}


        public int getValue() {return this.value;}
    }

    @JsonProperty("id")
    public final Long id;

    @JsonProperty("account_id")
    public final Optional<Long> accountId;

    @JsonProperty("title")
    public final String title;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("category")
    public final Category category;

    @JsonIgnore
    public final TimePeriod timePeriod; // insight computed over which time-period

    @JsonProperty("timestamp")
    public final DateTime timestamp; // created timestamp in UTC

    @JsonProperty("insights_info")
    public final List<GenericInsightCards> genericInsightCards;

    public InsightCard(final Long id, final Long accountId, final String title, final String message,
                       final Category category, final TimePeriod timePeriod, final DateTime timestamp,
                       final List<GenericInsightCards> genericInsightCards) {
        this.id = id;
        this.accountId = Optional.fromNullable(accountId);
        this.title = title;
        this.message = message;
        this.category = category;
        this.timePeriod = timePeriod;
        this.timestamp = timestamp;
        this.genericInsightCards = genericInsightCards;
    }

    public static InsightCard withGenericCards(final InsightCard insightCard, final List<GenericInsightCards> genericCards) {
        return new InsightCard(insightCard.id, insightCard.accountId.get(),
                insightCard.title, insightCard.message, insightCard.category, insightCard.timePeriod,
                insightCard.timestamp, genericCards);
    }
}
