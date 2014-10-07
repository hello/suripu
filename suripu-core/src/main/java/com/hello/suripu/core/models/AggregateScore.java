package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Created by kingshy on 10/1/14.
 */
public class AggregateScore {
    @JsonProperty("account_id")
    public final Long accountId;

    @JsonProperty("score")
    public final Integer score;

    @JsonProperty("date")
    public final String date;

    @JsonProperty("type")
    public final String scoreType;

    @JsonProperty("version")
    public final String version;

    public AggregateScore (final Long accountId, final Integer score,
                           final String date, final String scoreType, final String version) {
        this.accountId = accountId;
        this.score = score;
        this.date = date;
        this.scoreType = scoreType;
        this.version = version;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(Score.class)
                .add("account", accountId)
                .add("score", score)
                .add("date", date)
                .add("type", scoreType)
                .add("version", version)
                .toString();
    }

    @Override
    public boolean equals(final Object other) {
        boolean result = false;
        if (getClass() == other.getClass()) {
            AggregateScore score = (AggregateScore) other;
            result = (this.accountId == score.accountId &&
                    this.score == score.score &&
                    this.date.equals(score.date) &&
                    this.scoreType.equals(score.scoreType) &&
                    this.version.equals(score.version)
            );
        }
        return result;
    }


}
