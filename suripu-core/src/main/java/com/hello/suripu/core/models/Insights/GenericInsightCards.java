package com.hello.suripu.core.models.Insights;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kingshy on 12/15/14.
 */
public class GenericInsightCards {

    public enum Category {
        GENERIC(0),
        SLEEP_HYGIENE(1),
        LIGHT(2),
        SOUND(3),
        TEMPERATURE(4),
        HUMIDITY(5),
        AIR_QUALITY(6),
        SLEEP_DURATION(7),
        SLEEP_TIME(8),
        WAKEUP_TIME(9),
        WORKOUT(10),
        CAFFEINE(11),
        ALCOHOL(12),
        DAYTIME_SLEEPINESS(13),
        DIET(14),
        SLEEP_QUALITY(15);

        private int value;

        private Category(final int value) {this.value = value;}

        public int getValue() {return this.value;}

    }

    @JsonProperty("id")
    public final int id;

    @JsonProperty("category")
    public final Category category;

    @JsonProperty("url")
    public final String url;

    @JsonProperty("image_url")
    public final String imageUrl;

    public GenericInsightCards(int id, Category category, String url, String imageUrl) {
        this.id = id;
        this.category = category;
        this.url = url;
        this.imageUrl = imageUrl;
    }

}
