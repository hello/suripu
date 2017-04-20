package com.hello.suripu.core.notifications;

public enum PushNotificationEventType {

    GENERIC("generic"),
    SLEEP_SCORE("sleep_score"),
    SENSE_STATUS("sense_status"),
    PILL_BATTERY("pill_battery"),
    PILL_STATUS("pill_status"),
    INSIGHT("insight");

    private String value;

    PushNotificationEventType(String value) {
        this.value = value;
    }

    public String shortName() {
        return value;
    }

    public static PushNotificationEventType fromString(String value) {
        for(PushNotificationEventType type : PushNotificationEventType.values()) {
            if(type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid PushNotificationEventType: " + value);
    }

}
