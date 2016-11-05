package com.hello.suripu.core.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Alert {

    final Long id;
    final Long accountId;
    final String title;
    final String body;
    final DateTime createdAt;

    private Alert(Long id, Long accountId, String title, String body, final DateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
    }

    public static Alert create(Long id, Long accountId, String title, String body, final DateTime createdAt) {
        return new Alert(id, accountId, title, body, createdAt);
    }

    @JsonProperty("title")
    public String title() {
        return title;
    }

    @JsonProperty("body")
    public String body() {
        return body;
    }

    @JsonIgnore
    public Long id() {
        return id;
    }
}
