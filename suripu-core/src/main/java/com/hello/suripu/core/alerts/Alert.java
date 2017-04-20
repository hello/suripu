package com.hello.suripu.core.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

public class Alert {

    final Long id;
    final Long accountId;
    final String title;
    final String body;
    final AlertCategory category;
    final DateTime createdAt;

    private Alert(final Long id, final Long accountId, final String title, final String body, final AlertCategory category, final DateTime createdAt) {
        this.id = id;
        this.accountId = accountId;
        this.title = title;
        this.body = body;
        this.category = category;
        this.createdAt = createdAt;
    }

    public static Alert create(Long id, Long accountId, String title, String body, final AlertCategory category, final DateTime createdAt) {
        return new Alert(id, accountId, title, body, category,createdAt);
    }

    public static Alert unreachable(Long id, Long accountId, String title, String body, final DateTime createdAt) {
        return new Alert(id, accountId, title, body, AlertCategory.EXPANSION_UNREACHABLE,createdAt);
    }

    public static Alert muted(final Long accountId, final DateTime createdAt) {
        return new Alert(0L, accountId, "Sense Muted", "Sense is currently muted, and will not respond to Voice commands until unmuted.", AlertCategory.SENSE_MUTED, createdAt);
    }

    public static Alert pairingConflict(final Long accountId, final DateTime createdAt) {
        return Alert.create(0L, accountId, "Remove inactive accounts", "Your Sense may have old, inactive accounts paired to it. Please review and remove inactive accounts.", AlertCategory.REVIEW_ACCOUNTS_PAIRED_TO_SENSE, createdAt);
    }

    @JsonProperty("title")
    public String title() {
        return title;
    }

    @JsonProperty("body")
    public String body() {
        return body;
    }


    @JsonProperty("category")
    public AlertCategory category() {
        return category;
    }

    @JsonIgnore
    public Long id() {
        return id;
    }
}
