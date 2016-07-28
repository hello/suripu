package com.hello.suripu.core.speech;

/**
 * Created by ksg on 7/20/16
 */
public enum SpeechToTextService {
    HELLO("hello"),
    GOOGLE("google"),
    WATSON("watson");

    protected String value;
    SpeechToTextService(final String value) { this.value = value; }

    public static SpeechToTextService fromString(final String text) {
        if (text != null) {
            for (final SpeechToTextService service : SpeechToTextService.values()) {
                if (text.equalsIgnoreCase(service.toString()))
                    return service;
            }
        }
        throw new IllegalArgumentException("Invalid service string");
    }

}
