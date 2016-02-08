package com.hello.suripu.core.models.questions;

/**
 * Created by ksg on 02/1/16
 */
public enum QuestionCategory {
    NONE("none"),
    ONBOARDING("onboarding"),
    BASE("base"),
    DAILY("daily"),
    ANOMALY_LIGHT("anomaly_light");

    private String value;

    private QuestionCategory(final String value) { this.value = value; }

    public String getValue() { return this.value; }

    public static QuestionCategory fromString(final String text) {
        if (text != null) {
            for (final QuestionCategory category : QuestionCategory.values()) {
                if (text.equalsIgnoreCase(category.toString())) {
                    return category;
                }
            }
        }
        return QuestionCategory.NONE;
    }
}
