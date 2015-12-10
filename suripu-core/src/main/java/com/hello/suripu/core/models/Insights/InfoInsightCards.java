package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 12/15/14.
 */
public class InfoInsightCards {

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



    private InfoInsightCards(final int id, final InsightCard.Category category, final String title, final String text, final String imageUrl, final String categoryName) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.text = text;
        this.imageUrl = imageUrl;
        this.categoryName = categoryName;
    }

    public static InfoInsightCards create(final int id, final InsightCard.Category category, final String title, final String text, final String imageUrl) {
        return new InfoInsightCards(id, category, title, text, imageUrl, "");
    }

    public static InfoInsightCards create(final int id, final InsightCard.Category category, final String title, final String text, final String imageUrl, final String categoryName) {
        return new InfoInsightCards(id, category, title, text, imageUrl, categoryName);
    }

}
