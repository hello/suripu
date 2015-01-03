package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 12/15/14.
 */
public class GenericInsightCards {

    @JsonProperty("id")
    public final int id;

    @JsonProperty("category")
    public final InsightCard.Category category;

    @JsonProperty("url")
    public final String url;

    @JsonProperty("image_url")
    public final String imageUrl;

    public GenericInsightCards(int id, InsightCard.Category category, String url, String imageUrl) {
        this.id = id;
        this.category = category;
        this.url = url;
        this.imageUrl = imageUrl;
    }

}
