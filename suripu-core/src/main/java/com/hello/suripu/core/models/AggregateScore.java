package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Created by kingshy on 10/1/14.
 */
public class AggregateScore {
    @JsonProperty("score")
    public final Integer score;

    @JsonProperty("message")
    public final String message;

    @JsonProperty("date")
    public final String date;

    @JsonProperty("type")
    public final String scoreType;

    @JsonProperty("version")
    public final String version;

    public AggregateScore (final Integer score, final String message, final String date, final String scoreType, final String version) {
        this.score = score;
        this.message = message;
        this.date = date;
        this.scoreType = scoreType;
        this.version = version;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Score.class)
                .add("score", score)
                .add("message", message)
                .add("date", date)
                .add("type", scoreType)
                .add("version", version)
                .toString();
    }

}
