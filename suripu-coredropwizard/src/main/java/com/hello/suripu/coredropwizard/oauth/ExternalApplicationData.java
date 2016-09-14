package com.hello.suripu.coredropwizard.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class ExternalApplicationData {

    @JsonProperty("id")
    public final Long id;

    @JsonProperty("app_id")
    public final Long appId;

    @JsonProperty("device_id")
    public final String deviceId;

    @JsonProperty("data")
    public final String data;

    @JsonProperty("created_at")
    public final DateTime created;

    @JsonProperty("updated_at")
    public final DateTime updated;

    public ExternalApplicationData(
            final Long id,
            final Long appId,
            final String deviceId,
            final String data,
            final DateTime created,
            final DateTime updated
    ) {
        this.id = id;
        this.appId = appId;
        this.deviceId = deviceId;
        this.data = data;
        this.created = created;
        this.updated = updated;
    }

    public static class Builder {
        private Long id;
        private Long appId;
        private String deviceId;
        private String data;
        private DateTime created;
        private DateTime updated;

        public Builder() {
            created = DateTime.now(DateTimeZone.UTC);
            updated = DateTime.now(DateTimeZone.UTC);
        }


        public Builder withCreated(final DateTime created) {
            this.created = created;
            return this;
        }

        public Builder withDeviceId(final String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder withData(final String data) {
            this.data = data;
            return this;
        }

        public Builder withAppId(final Long appId) {
            this.appId = appId;
            return this;
        }

        public ExternalApplicationData build() {
            return new ExternalApplicationData(id, appId, deviceId, data, created, updated);
        }
    }
}
