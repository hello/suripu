package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


public class SleepScore {

    @JsonIgnore
    public Long id;

    @JsonIgnore
    public long accountId;

    @JsonProperty("date_hour_utc")
    public DateTime dateHourUTC;

    @JsonProperty("pill_id")
    public long pillID;

    @JsonProperty("sleep_duration")
    public int sleepDuration;

    @JsonProperty("custom")
    public boolean custom;

    @JsonProperty("total_hour_score")
    public int totalHourScore;

    @JsonProperty("sax_symbols")
    public String saxSymbols;

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
            final int totalHourScore,
            final boolean custom,
            final String saxSymbols,
            final int agitationNum,
            final long agitationTot,
            final DateTime updated,
            final int timeZoneOffset
    ){
        this.id = id;
        this.accountId = accountId;
        this.dateHourUTC = date;
        this.pillID = pillID;
        this.sleepDuration = sleepDuration;
        this.totalHourScore = totalHourScore;
        this.custom = custom;
        this.saxSymbols = saxSymbols;
        this.agitationNum = agitationNum;
        this.agitationTot = agitationTot;
        this.updated = updated;
        this.timeZoneOffset = timeZoneOffset;

    }

    @JsonCreator
    public SleepScore(
            @JsonProperty("date_utc") final long date,  // in millisecs
            @JsonProperty("updated") final int updated,
            @JsonProperty("timezone_offset") final int timeZoneOffset
    ){
        // TODO
        this.dateHourUTC = new DateTime(date, DateTimeZone.UTC);
        this.updated = new DateTime(updated, DateTimeZone.UTC);
        this.timeZoneOffset = timeZoneOffset;

    }

    @Override
    public String toString() {
        return "Pill: " + this.pillID + ", Account:" + this.accountId +
                ", Date: " + this.dateHourUTC + ", Offset: " + this.timeZoneOffset +
                ", Score: " + this.totalHourScore + ", Agitation_Num: " +
                this.agitationNum + ", Agitation_Tot: " + this.agitationTot;
    }


}
