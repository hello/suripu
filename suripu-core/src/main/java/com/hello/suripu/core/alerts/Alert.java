package com.hello.suripu.core.alerts;

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

    public String title() {
        return title;
    }

    public String text() {
        return body;
    }

    public Long accountId() {
        return accountId;
    }

}
