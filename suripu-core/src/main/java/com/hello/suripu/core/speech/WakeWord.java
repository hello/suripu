package com.hello.suripu.core.speech;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by ksg on 7/20/16
 */
public enum WakeWord {

    ERROR("error", 0),
    OKAY_SENSE("okay sense", 1),
    HEY_SENSE("hey sense", 2),
    WHATEVER_SENSE("whatever sense", 3);

    private final String wakeWordText;
    private final int id;

    WakeWord(String wakeWordText, int id) {
        this.wakeWordText = wakeWordText;
        this.id = id;
    }

    @JsonValue
    public int getId() { return id; }

    public String getWakeWordText() { return wakeWordText; }

    public static WakeWord fromString(final String text) {
        if (text != null) {
            for (final WakeWord wakeWord : WakeWord.values()) {
                if (text.equalsIgnoreCase(wakeWord.toString()))
                    return wakeWord;
            }
        }
        return WakeWord.ERROR;
    }

    public static WakeWord fromInteger(final int value) {
        for (final WakeWord word : WakeWord.values()) {
            if (value == word.id)
                return word;
        }
        return WakeWord.ERROR;
    }

    public static WakeWord fromWakeWordText(final String text) {
        if (text != null) {
            for (final WakeWord wakeWord : WakeWord.values()) {
                if (text.equalsIgnoreCase(wakeWord.getWakeWordText()))
                    return wakeWord;
            }
        }
        return WakeWord.ERROR;

    }

}
