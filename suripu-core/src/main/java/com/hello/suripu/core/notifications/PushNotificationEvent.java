package com.hello.suripu.core.notifications;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jakepiccolo on 5/3/16.
 */
public class PushNotificationEvent {

    enum Type {
        SLEEP_SCORE,
        ROOM_CONDITIONS,
        INSIGHT,
        SLEEP_PILL_BATTERY_LOW,
        SENSE_OFFLINE,
        PILL_OFFLINE
    }

    public final Long accountId;
    public final Type type;
    public final DateTime timestamp;
    public final HelloPushMessage helloPushMessage;
    public final Optional<String> senseId;

    protected PushNotificationEvent(final Long accountId, final Type type, final DateTime timestamp,
                                    final HelloPushMessage helloPushMessage, final Optional<String> senseId)
    {
        this.accountId = accountId;
        this.type = type;
        this.timestamp = timestamp;
        this.helloPushMessage = helloPushMessage;
        this.senseId = senseId;
    }


    //region Object overrides

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof PushNotificationEvent)) {
            return false;
        }

        final PushNotificationEvent event = (PushNotificationEvent) obj;
        return this.accountId.equals(event.accountId) &&
                this.type.equals(event.type) &&
                this.timestamp.equals(event.timestamp) &&
                this.helloPushMessage.equals(event.helloPushMessage) &&
                this.senseId.equals(event.senseId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(PushNotificationEvent.class)
                .add("accountId", accountId)
                .add("type", type)
                .add("timestamp", timestamp)
                .add("helloPushMessage", helloPushMessage)
                .add("senseId", senseId)
                .toString();
    }

    //endregion Object overrides


    //region Builder

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Long accountId;
        private Type type;
        private DateTime timestamp;
        private HelloPushMessage helloPushMessage;
        private Optional<String> senseId = Optional.absent();

        public PushNotificationEvent build() {
            checkNotNull(accountId);
            checkNotNull(type);
            checkNotNull(timestamp);
            checkNotNull(helloPushMessage);
            return new PushNotificationEvent(accountId, type, timestamp, helloPushMessage, senseId);
        }

        public Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withType(final Type type) {
            this.type = type;
            return this;
        }

        public Builder withTimestamp(final DateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withHelloPushMessage(final HelloPushMessage helloPushMessage) {
            this.helloPushMessage = helloPushMessage;
            return this;
        }

        public Builder withSenseId(final String senseId) {
            this.senseId = Optional.fromNullable(senseId);
            return this;
        }
    }

    //endregion Builder
}
