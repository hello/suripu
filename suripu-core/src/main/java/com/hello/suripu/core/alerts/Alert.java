package com.hello.suripu.core.alerts;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Alert {

    final Long accountId;
    final String title;
    final String body;
    final DateTime createdAt;

    private Alert(Long accountId, String title, String body, final DateTime createdAt) {
        this.accountId = accountId;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
    }

    public static Alert create(Long accountId, String title, String body, final DateTime createdAt) {
        return new Alert(accountId, title, body, createdAt);
    }

    @JsonProperty("title")
    public String title() {
        return title;
    }

    @JsonProperty("body")
    public String body() {
        return body;
    }
}
