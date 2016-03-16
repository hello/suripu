package com.hello.suripu.core.models.sleep_sounds;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

/**
 * Created by jakepiccolo on 2/19/16.
 */
public class SleepSoundStatus {
    @JsonProperty("playing")
    public final Boolean isPlaying;

    @JsonProperty("sound")
    public final Optional<Sound> sound;

    @JsonProperty("duration")
    public final Optional<Duration> duration;

    @JsonProperty("volume_percent")
    public final Optional<Integer> volumePercent;

    private SleepSoundStatus(final Boolean isPlaying, final Optional<Sound> sound, final Optional<Duration> duration, final Optional<Integer> volumePercent) {
        this.isPlaying = isPlaying;
        this.sound = sound;
        this.duration = duration;
        this.volumePercent = volumePercent;
    }

    public static SleepSoundStatus create() {
        return new SleepSoundStatus(false, Optional.<Sound>absent(), Optional.<Duration>absent(), Optional.<Integer>absent());
    }

    public static SleepSoundStatus create(final Sound sound, final Duration duration, final Integer volumePercent) {
        return new SleepSoundStatus(true, Optional.of(sound),
                Optional.of(duration), Optional.of(volumePercent));
    }

    public static SleepSoundStatus create(final Sound sound, final Duration duration) {
        return new SleepSoundStatus(true, Optional.of(sound),
                Optional.of(duration), Optional.<Integer>absent());
    }
}
