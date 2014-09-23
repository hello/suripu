package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;


public class SleepScore {
    private final static Logger LOGGER = LoggerFactory.getLogger(SleepScore.class);

    @JsonIgnore
    public Long id;

    @JsonIgnore
    public long accountId;

    @JsonProperty("date_hour_utc")
    public DateTime dateBucketUTC;

    @JsonProperty("pill_id")
    public long pillID;

    @JsonProperty("sleep_duration")
    public int sleepDuration;

    @JsonProperty("custom")
    public boolean custom;

    @JsonProperty("total_hour_score")
    public int bucketScore;

    @JsonProperty("agitation_num")
    public int agitationNum;

    @JsonProperty("agitation_tot")
    public long agitationTot;

    @JsonProperty("updated")
    public DateTime updated;

    @JsonProperty("timezone_offset")
    public int timeZoneOffset;


    public SleepScore(
            final Long id,
            final long accountId,
            final DateTime date,
            final long pillID,
            final int sleepDuration,
            final int bucketScore,
            final boolean custom,
            final int agitationNum,
            final long agitationTot,
            final DateTime updated,
            final int timeZoneOffset
    ){
        this.id = id;
        this.accountId = accountId;
        this.dateBucketUTC = date;
        this.pillID = pillID;
        this.sleepDuration = sleepDuration;
        this.bucketScore = bucketScore;
        this.custom = custom;
        this.agitationNum = agitationNum;
        this.agitationTot = agitationTot;
        this.updated = updated;
        this.timeZoneOffset = timeZoneOffset;

    }

    @Override
    public String toString() {
        return Objects.toStringHelper(SleepScore.class)
                .add("pill", pillID)
                .add("account", accountId)
                .add("date_bucket_utc", dateBucketUTC)
                .add("tz_offset", timeZoneOffset)
                .add("score", this.bucketScore)
                .add("agitation_num", agitationNum)
                .add("agitation_tot", agitationTot)
                .toString();
    }

    public static List<SleepScore> computeSleepScore(final Long accountID,
                                                     final String pillID,
                                                     final SortedSet<PillSample> pillData,
                                                     final int dateBucketPeriod) {

        LOGGER.debug("======= Computing scores for this pill {}, {}", pillID, accountID);

        final List<SleepScore> sleepScores = new ArrayList<>();
        final PillSample firstData = pillData.first();
        final int timeZoneOffset = firstData.timeZoneOffset;

        float agitationNum = 0;
        float agitationTot = 0;
        int duration = 0;
        int minute = (int) firstData.dateTime.getMinuteOfHour()/dateBucketPeriod;
        DateTime lastBucketDT = firstData.dateTime.withMinuteOfHour(minute * dateBucketPeriod);

        for (final PillSample data: pillData) {
            minute = (int) data.dateTime.getMinuteOfHour() / dateBucketPeriod;
            final DateTime bucket = data.dateTime.withMinuteOfHour(minute * dateBucketPeriod);
            if (bucket.compareTo(lastBucketDT) != 0) {
                SleepScore sleepScore = new SleepScore(0L, accountID,
                        lastBucketDT,
                        Long.parseLong(pillID),
                        duration,
                        (int) (agitationNum/((float) dateBucketPeriod) * 100.0), // score
                        false, // no customized score yet
                        (int) agitationNum,
                        (long) agitationTot,
                        DateTime.now(),
                        timeZoneOffset
                );
                LOGGER.debug("created new score object for {}", sleepScore.toString());
                sleepScores.add(sleepScore);

                agitationNum = 0;
                agitationTot = 0;
                duration = 0;
                lastBucketDT = bucket;
            }

            LOGGER.debug("Sensor Sample {}", data.toString());
            final float value = data.val;
            if (value != -1) {
                agitationNum++;
                agitationTot = agitationTot + value;
            }
            duration++;
        }

        if (duration != 0) {
            SleepScore sleepScore = new SleepScore(0L, accountID,
                    lastBucketDT,
                    Long.parseLong(pillID),
                    duration,
                    (int) ((agitationNum)/((float) dateBucketPeriod) * 100.0),
                    false, // no customized score for now
                    (int) agitationNum,
                    (long) agitationTot,
                    DateTime.now(),
                    timeZoneOffset
            );
            LOGGER.debug("created new score object for {}", sleepScore.toString());
            sleepScores.add(sleepScore);

        }
        return sleepScores;
    }

}
