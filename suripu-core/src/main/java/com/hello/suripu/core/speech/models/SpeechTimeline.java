package com.hello.suripu.core.speech.models;

import org.joda.time.DateTime;

/**
 * Created by ksg on 7/20/16
 */
public class SpeechTimeline {

    public final Long accountId;
    public final DateTime dateTimeUTC;
    public final String senseId;
    public final String audioUUID;  // uuid string of audio file in S3

    public SpeechTimeline(final Long accountId,
                          final DateTime dateTimeUTC,
                          final String senseId,
                          final String audioUUID) {
        this.accountId = accountId;
        this.dateTimeUTC = dateTimeUTC;
        this.senseId = senseId;
        this.audioUUID = audioUUID;
    }
}
