package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


public class SleepLabel {

    @JsonIgnore
    public Long id;

    @JsonIgnore
    public long accountId;

    @JsonProperty("date_utc")
    public DateTime dateUTC;

    @JsonProperty("rating")
    public SleepRating rating;

    @JsonProperty("sleep_at_utc")
    public DateTime sleepTimeUTC;

    @JsonProperty("wakeup_at_utc")
    public DateTime wakeUpTimeUTC;

    @JsonProperty("timezone_offset")
    public int timeZoneOffset;


    public SleepLabel(
            final Long id,
            final long accountId,
            final DateTime date,
            final SleepRating sleepRating,
            final DateTime sleepTime,
            final DateTime wakeUpTime,
            final int timeZoneOffset
    ){
        this.id = id;
        this.accountId = accountId;
        this.dateUTC = date;
        this.rating = sleepRating;
        this.sleepTimeUTC = sleepTime;
        this.wakeUpTimeUTC = wakeUpTime;
        this.timeZoneOffset = timeZoneOffset;

    }

    @JsonCreator
    public SleepLabel(
            @JsonProperty("date_utc") final long date,  // in millisecs
            @JsonProperty("rating") final SleepRating sleepRating,
            @JsonProperty("sleep_at_utc") final long sleepTime, // in millisecs
            @JsonProperty("wakeup_at_utc") final long wakeUpTime,   // in millisecs
            @JsonProperty("timezone_offset") final int timeZoneOffset
    ){

        this.dateUTC = new DateTime(date, DateTimeZone.UTC);
        this.rating = sleepRating;
        this.sleepTimeUTC = new DateTime(sleepTime, DateTimeZone.UTC);
        this.wakeUpTimeUTC = new DateTime(wakeUpTime, DateTimeZone.UTC);
        this.timeZoneOffset = timeZoneOffset;

    }


}
