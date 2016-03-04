package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hello.suripu.core.models.Insight;

/**
 * Created by kingshy on 12/15/14.
 */
public class InfoInsightCards {

    public enum InsightType {
        DEFAULT(0),
        GENERIC(1);

        private int value;

        InsightType(final int value) {this.value = value;}

        public int getValue() {return this.value;}

        public static InsightType fromInteger(final int value) {
            for (final InsightType insightType : InsightType.values()) {
                if (value == insightType.getValue())
                    return insightType;
            }
            return InsightType.DEFAULT;
        }

        public static InsightType fromString(final String text) {
            if (text != null) {
                for (final InsightType insightType : InsightType.values()) {
                    if (text.equalsIgnoreCase(insightType.toString()))
                        return insightType;
                }
            }
            throw new IllegalArgumentException("Invalid InsightType string");
        }
    }

    @JsonProperty("id")
    public final int id;

    @JsonProperty("category")
    public final InsightCard.Category category;

    @JsonIgnore
    public final String categoryName;

    @JsonProperty("title")
    public final String title;

    @JsonProperty("text")
    public final String text;

    @JsonProperty("image_url")
    public final String imageUrl;

    @JsonProperty("insight_type")
    public final InsightType insightType;


    private InfoInsightCards(final int id, final InsightCard.Category category, final String title, final String text, final String imageUrl, final String categoryName, final InsightType insightType) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
        this.categoryName = categoryName;
        this.insightType = insightType;
    }

    public static InfoInsightCards create(final int id, final InsightCard.Category category, final String title, final String text, final String imageUrl) {
        return new InfoInsightCards(id, category, title, text, imageUrl, "", InsightType.DEFAULT);
    }

    public static InfoInsightCards create(final int id, final InsightCard.Category category, final String title, final String text, final String imageUrl, final String categoryName, final InsightType insightType) {
        return new InfoInsightCards(id, category, title, text, imageUrl, categoryName, insightType);
    }

}
