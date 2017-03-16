package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ComparisonChain;
import com.hello.suripu.core.models.MultiDensityImage;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.util.UUID;

/**
 * Created by kingshy on 10/24/14.
 */
public class InsightCard implements Comparable<InsightCard> {
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
        SLEEP_QUALITY(18), // movement during sleep
        WAKE_VARIANCE(19),
        BED_LIGHT_DURATION(20),
        BED_LIGHT_INTENSITY_RATIO(21),
        PARTNER_MOTION(22),
        DRIVE(23),
        EAT(24),
        LEARN(25),
        LOVE(26),
        PLAY(27),
        RUN(28),
        SWIM(29),
        WORK(30),
        GOAL_GO_OUTSIDE(31),
        GOAL_COFFEE(32),
        GOAL_SCHEDULE_THOUGHTS(33),
        GOAL_SCREENS(34),
        GOAL_WAKE_VARIANCE(35),
        SLEEP_DEPRIVATION(36),
        CORRELATION_TEMP(37);

        private int value;

        Category(final int value) {this.value = value;}

        public int getValue() {return this.value;}

        public String toCategoryString() {
            return String.format("%03d", this.getValue());
        }

        public static Category fromInteger(final int value) {
            for (final Category category : Category.values()) {
                if (value == category.getValue())
                    return category;
            }
            return Category.GENERIC;
        }

        public static Category fromString(final String text) {
            if (text != null) {
                for (final Category category : Category.values()) {
                    if (text.equalsIgnoreCase(category.toString()))
                        return category;
                }
            }
            throw new IllegalArgumentException("Invalid Category string");
        }

    }

    public static final int ONE_DAY = 1;
    public static final int RECENT_DAYS = 3;
    public static final int PAST_WEEK = 7;

    public static final String EMPTY_CATEGORY_NAME = "";

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

        TimePeriod(final int value) {this.value = value;}

        public int getValue() {return this.value;}

        public static TimePeriod fromString(final String text) {
            if (text != null) {
                for (final TimePeriod period : TimePeriod.values()) {
                    if (text.equalsIgnoreCase(period.toString()))
                        return period;
                }
            }
            throw new IllegalArgumentException("Invalid TimePeriod string");
        }

    }

    public enum InsightType {
        DEFAULT, //Contains block quotes
        BASIC; //No block quotes. Used for intro insights.

        public static InsightType fromString(final String text) {
            for (final InsightType insightType : InsightType.values()) {
                if (insightType.toString().equalsIgnoreCase(text))
                    return insightType;
            }
            throw new IllegalArgumentException(String.format("Illegal InsightType string %s", text));
        }
    }

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

    @JsonProperty("info_preview")
    public final Optional<String> infoPreview;

    @JsonProperty("image")
    public final Optional<MultiDensityImage> image;

    @JsonIgnore
    public final String categoryName;

    @JsonProperty("category_name")
    public String categoryName() {
        return categoryName.isEmpty() ? category.name().toLowerCase() : categoryName;
    }

    @JsonProperty("insight_type")
    public final InsightType insightType;

    @JsonProperty("id")
    public final Optional<UUID> id;


    private InsightCard(final Long accountId, final String title, final String message,
                        final Category category, final TimePeriod timePeriod, final DateTime timestamp,
                        final Optional<String> infoPreview, final Optional<MultiDensityImage> image,
                        final String categoryName, final InsightType insightType,
                        final Optional<UUID> idOptional ) {
        this.accountId = Optional.fromNullable(accountId);
        this.title = title;
        this.message = message;
        this.category = category;
        this.timePeriod = timePeriod;
        this.timestamp = timestamp;
        this.infoPreview = infoPreview;
        this.image = image;
        this.categoryName = categoryName;
        this.insightType = insightType;
        this.id = idOptional;
    }

    // public static methods to create InsightCard

    // custom Insight Processors such as WakeVariance
    public static InsightCard createBasicInsightCard(final Long accountId, final String title, final String message,
                                                     final Category category, final TimePeriod timePeriod, final DateTime timestamp,
                                                     final InsightType insightType) {
        return new InsightCard(accountId, title, message, category, timePeriod, timestamp,
                Optional.<String>absent(),
                Optional.<MultiDensityImage>absent(),
                EMPTY_CATEGORY_NAME,
                insightType,
                Optional.<UUID>absent());
    }

    // marketing and goals insight
    public static InsightCard createInsightCardWithCategoryName(final Long accountId, final String title, final String message,
                                                                final Category category, final TimePeriod timePeriod, final DateTime timestamp,
                                                                final String categoryName, final InsightType insightType) {
        return new InsightCard(accountId, title, message, category, timePeriod, timestamp,
                Optional.<String>absent(), Optional.<MultiDensityImage>absent(), categoryName, insightType,
                Optional.<UUID>absent());
    }

    // introduction cards
    public static InsightCard createIntroductionInsightCardsWithId(final Long accountId, final String title, final String message,
                                                                   final Category category, final TimePeriod timePeriod, final DateTime timestamp,
                                                                   final String categoryName, final InsightType insightType) {
        return new InsightCard(accountId, title, message, category, timePeriod, timestamp,
                Optional.<String>absent(), Optional.<MultiDensityImage>absent(), categoryName, insightType,
                Optional.of(UUID.randomUUID()));
    }

    // DynamoDB (has almost everything)
    public static InsightCard createInsightCardNoCategoryName(final Long accountId, final String title, final String message,
                                                              final Category category, final TimePeriod timePeriod, final DateTime timestamp,
                                                              final Optional<String> infoPreview, final Optional<MultiDensityImage> image,
                                                              final InsightType insightType,
                                                              final Optional<UUID> idOptional) {
        return new InsightCard(accountId, title, message, category, timePeriod, timestamp, infoPreview, image, EMPTY_CATEGORY_NAME, insightType, idOptional);
    }


    @Override
    public int compareTo(InsightCard o) {
        // TODO: test this
        final InsightCard compareObject = (InsightCard) o;
        return ComparisonChain.start()
                .compare(this.timestamp, compareObject.timestamp)
                .result();
//        final DateTime compareTimestamp = compareObject.timestamp;
//        final DateTime objectTimestamp = this.timestamp;
//        return (int) (objectTimestamp.getMillis() - compareTimestamp.getMillis()); // ascending

    }

    public InsightCard withInfoPreview(final Optional<String> infoPreview) {
        return new InsightCard(
                this.accountId.get(),
                this.title,
                this.message,
                this.category,
                this.timePeriod,
                this.timestamp,
                infoPreview,
                this.image,
                EMPTY_CATEGORY_NAME,
                this.insightType,
                this.id
                );
    }

    /**
     * Returns copy of insight card with image added
     * @param card
     * @param image
     * @return
     */
    public static InsightCard withImage(final InsightCard card, @NotNull final MultiDensityImage image) {
        return new InsightCard(card.accountId.get(), card.title, card.message, card.category,
                card.timePeriod, card.timestamp, card.infoPreview, Optional.of(image), EMPTY_CATEGORY_NAME, card.insightType,
                card.id);
    }


    /**
     * Returns copy of insight card with image and category name
     * @param card
     * @param image
     * @param categoryName
     * @return
     */
    public static InsightCard withImageAndCategory(final InsightCard card, @NotNull final MultiDensityImage image, @NotNull String categoryName) {
        return new InsightCard(card.accountId.get(), card.title, card.message, card.category,
                card.timePeriod, card.timestamp, card.infoPreview, Optional.of(image), categoryName, card.insightType,
                card.id);
    }
}
