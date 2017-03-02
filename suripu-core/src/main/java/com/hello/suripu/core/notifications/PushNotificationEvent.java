package com.hello.suripu.core.notifications;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jakepiccolo on 5/3/16.
 */
public class PushNotificationEvent {
    public final Long accountId;
    public final PushNotificationEventType type;
    public final DateTime timestamp;
    public final HelloPushMessage helloPushMessage;
    public final Optional<String> senseId;
    public final DateTimeZone timeZone;
    public final Periodicity periodicity;

    protected PushNotificationEvent(final Long accountId, final PushNotificationEventType type, final DateTime timestamp,
                                    final HelloPushMessage helloPushMessage, final Optional<String> senseId,
                                    final DateTimeZone timeZone, final Periodicity periodicity)
    {
        this.accountId = accountId;
        this.type = type;
        this.timestamp = timestamp;
        this.helloPushMessage = helloPushMessage;
        this.senseId = senseId;
        this.timeZone = timeZone;
        this.periodicity = periodicity;
    }

    //region Object overrides

    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof PushNotificationEvent)) {
            return false;
        }

        final PushNotificationEvent event = (PushNotificationEvent) obj;
        return Objects.equals(accountId, event.accountId) &&
                Objects.equals(type, event.type) &&
                Objects.equals(timestamp, event.timestamp) &&
                Objects.equals(helloPushMessage, event.helloPushMessage) &&
                Objects.equals(senseId, event.senseId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(PushNotificationEvent.class)
                .add("accountId", accountId)
                .add("type", type)
                .add("timestamp", timestamp)
                .add("helloPushMessage", helloPushMessage)
                .add("senseId", senseId.or("missing"))
                .toString();
    }

    //endregion Object overrides

    //region Builder

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Long accountId;
        private PushNotificationEventType type;
        private DateTime timestamp;
        private HelloPushMessage helloPushMessage;
        private Optional<String> senseId = Optional.absent();
        private DateTimeZone timeZone = DateTimeZone.UTC;
        private Periodicity periodicity = Periodicity.MINUTELY;

        public Builder() {

        }

        public Builder(final PushNotificationEvent pushNotificationEvent) {
            this.accountId = pushNotificationEvent.accountId;
            this.type = pushNotificationEvent.type;
            this.timestamp = pushNotificationEvent.timestamp;
            this.helloPushMessage = pushNotificationEvent.helloPushMessage;
            this.senseId = pushNotificationEvent.senseId;
            this.timeZone = pushNotificationEvent.timeZone;
            this.periodicity = pushNotificationEvent.periodicity;
        }

        public PushNotificationEvent build() {
            checkNotNull(accountId,"accountId cannot be null");
            checkNotNull(type, "type cannot be null");
            checkNotNull(helloPushMessage, "message cannot be null");
            checkNotNull(timeZone, "timezone cannot be null");

            final DateTime eventTimestamp = timestamp == null ? DateTime.now(DateTimeZone.UTC) : timestamp;
            return new PushNotificationEvent(accountId, type, eventTimestamp, helloPushMessage, senseId, timeZone, periodicity);
        }

        public Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withType(final PushNotificationEventType type) {
            this.type = type;
            return this;
        }

        /**
         * Set the timestamp for the event. Defaults to DateTime.now(UTC)
         */
        public Builder withTimestamp(final DateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withHelloPushMessage(final HelloPushMessage helloPushMessage) {
            this.helloPushMessage = helloPushMessage;
            return this;
        }

        public Builder withSenseId(final String senseId) {
            if(senseId != null && senseId.isEmpty()) {
                this.senseId = Optional.absent();
            } else {
                this.senseId = Optional.fromNullable(senseId);
            }

            return this;
        }

        public Builder withTimeZone(final DateTimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public Builder withPeriodicity(final Periodicity periodicity) {
            this.periodicity = periodicity;
            return this;
        }
    }

    //endregion Builder
}
