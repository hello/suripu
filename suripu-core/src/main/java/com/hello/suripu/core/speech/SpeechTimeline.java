package com.hello.suripu.core.speech;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

/**
 * Created by ksg on 7/20/16
 */
public class SpeechTimeline {

    public final Long accountId;
    public final DateTime dateTimeUTC;
    public final String senseId;
    public final String encryptedUUID;  // uuid string of audio file in S3
    public final Optional<String> decryptedUUID;

    private SpeechTimeline(final Long accountId,
                          final DateTime dateTimeUTC,
                          final String senseId,
                          final String encryptedUUID,
                          final Optional<String> decryptedUUID) {
        this.accountId = accountId;
        this.dateTimeUTC = dateTimeUTC;
        this.senseId = senseId;
        this.encryptedUUID = encryptedUUID;
        this.decryptedUUID = decryptedUUID;
    }

    public SpeechTimeline withDecryptedUUID(final String decryptedUUID) {
        return new SpeechTimeline(this.accountId, this.dateTimeUTC, this.senseId, this.encryptedUUID, Optional.of(decryptedUUID));
    }

    public static SpeechTimeline create(final Long accountId,
                                        final DateTime dateTimeUTC,
                                        final String senseId,
                                        final String encryptedUUID) {
        return new SpeechTimeline(accountId, dateTimeUTC, senseId, encryptedUUID, Optional.<String>absent());
    }
}
