package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;


public class Score {

    @JsonIgnore
    public final long accountId;

    @JsonProperty("temperature")
    public final int temperature;

    @JsonProperty("humidity")
    public final int humidity;

    @JsonProperty("sound")
    public final int sound;

    @JsonProperty("air_quality")
    public final int airQuality;

    @JsonProperty("light")
    public final int light;

    @JsonProperty("date")
    public final DateTime dateTime;

    public Score(
            final long accountId,
            final int temperature,
            final int humidity,
            final int sound,
            final int airQuality,
            final int light,
            final DateTime dateTime) {

        checkNotNull(accountId, "AccountId can not be null");
        this.accountId = accountId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.sound = sound;
        this.airQuality = airQuality;
        this.light = light;
        this.dateTime = dateTime;
    }

    // TODO: add equals and hashcode method

    @Override
    public String toString() {
        return Objects.toStringHelper(Score.class)
                .add("accountId", accountId)
                .add("temperature", temperature)
                .add("humidity", humidity)
                .add("sound", sound)
                .add("air_quality", airQuality)
                .add("light", light)
                .toString();
    }

    public static class Builder {
        private Long accountId;
        private Integer temperature = 0;
        private Integer humidity = 0;
        private Integer sound =0;
        private Integer airQuality = 0;
        private Integer light = 0;
        private DateTime dateTime = DateTime.now();


        public Builder withAccountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withTemperature(Integer temp) {
            this.temperature = temperature;
            return this;
        }

        public Builder withHumidity(Integer humidity) {
            this.humidity = humidity;
            return this;
        }

        public Builder withSound(Integer sound) {
            this.sound = sound;
            return this;
        }

        public Builder withAirQuality(Integer airQuality) {
            this.airQuality = airQuality;
            return this;
        }

        public Builder withLight(Integer light) {
            this.light = light;
            return this;
        }

        public Builder withDateTime(DateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public Score build() {
            return new Score(accountId, temperature, humidity, sound, airQuality, light, dateTime);
        }
    }
}
