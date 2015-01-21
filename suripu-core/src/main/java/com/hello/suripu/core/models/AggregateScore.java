package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hello.suripu.core.util.DateTimeUtil;
import org.joda.time.DateTime;

/**
 * Created by kingshy on 10/1/14.
 */
public class AggregateScore implements Comparable<AggregateScore> {
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

    public int getDateTimestamp() {
        final DateTime date = DateTimeUtil.ymdStringToDateTime(this.date);
        return (int) (date.getMillis()/1000.0f);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(AggregateScore.class)
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


    @Override
    public int compareTo(AggregateScore o) {
        final AggregateScore compareObject = (AggregateScore) o;
        final int compareTimestamp = compareObject.getDateTimestamp();
        final int objectTimestamp = this.getDateTimestamp();
        return objectTimestamp - compareTimestamp;  // ascending

    }
}