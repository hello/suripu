package com.hello.suripu.core.speech.models;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by ksg on 7/20/16
 */
public enum WakeWord {

    NULL("null", 0),
    OK_SENSE("ok_sense", 1),
    STOP("stop", 2),
    SNOOZE("snooze", 3),
    ALEXA("alexa", 4);


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
        return WakeWord.NULL;
    }

    public static WakeWord fromInteger(final int value) {
        for (final WakeWord word : WakeWord.values()) {
            if (value == word.id)
                return word;
        }
        return WakeWord.NULL;
    }

    public static WakeWord fromWakeWordText(final String text) {
        if (text != null) {
            for (final WakeWord wakeWord : WakeWord.values()) {
                if (text.equalsIgnoreCase(wakeWord.getWakeWordText()))
                    return wakeWord;
            }
        }
        return WakeWord.NULL;

    }

}
