package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 12/15/14.
 */
public class InfoInsightCards {

    @JsonProperty("id")
    public final int id;

    @JsonProperty("category")
    public final InsightCard.Category category;

    @JsonProperty("text")
    public final String text;

    @JsonProperty("image_url")
    public final String imageUrl;

    public InfoInsightCards(int id, InsightCard.Category category, String text, String imageUrl) {
        this.id = id;
        this.category = category;
        this.text = text;
        this.imageUrl = imageUrl;
    }

}
