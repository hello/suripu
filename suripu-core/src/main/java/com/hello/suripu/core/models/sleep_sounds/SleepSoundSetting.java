package com.hello.suripu.core.models.sleep_sounds;

import com.google.common.base.MoreObjects;
import com.hello.suripu.core.notifications.HelloPushMessage;
import org.joda.time.DateTime;

/**
 * Created by ksg on 2/28/17
 */
public class SleepSoundSetting {

    public final String senseId;
    public final Long accountId;
    public final DateTime datetime;
    public final Sound sound;
    public final Duration duration;
    public final Integer volumeScalingFactor;

    private SleepSoundSetting(final String senseId, final Long accountId, final DateTime datetime, final Sound sound, final Duration duration, final Integer volumeScalingFactor) {
        this.senseId = senseId;
        this.accountId = accountId;
        this.datetime = datetime;
        this.sound = sound;
        this.duration = duration;
        this.volumeScalingFactor = volumeScalingFactor;
    }

    public static SleepSoundSetting create(final String senseId, final Long accountId, final DateTime datetime, final Sound sound, final Duration duration, final Integer volumeScalingFactor) {
        return new SleepSoundSetting(senseId, accountId, datetime, sound, duration, volumeScalingFactor);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(HelloPushMessage.class)
                .add("sound", sound.name)
                .add("duration", duration.name)
                .add("volume", volumeScalingFactor)
                .toString();
    }

}
