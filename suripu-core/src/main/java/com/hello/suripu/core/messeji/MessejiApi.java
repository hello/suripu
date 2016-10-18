package com.hello.suripu.core.messeji;

import com.google.common.base.Optional;
import com.hello.messeji.api.Messeji;
import com.hello.suripu.core.models.sleep_sounds.Duration;
import com.hello.suripu.core.models.sleep_sounds.Sound;

public interface MessejiApi {
    Optional<Long> sendMessage(String senseId, Messeji.Message message);

    Optional<Long> playAudio(String senseId, Sender sender, Long order, Duration duration, Sound sound,
                             Integer fadeInSeconds, Integer fadeOutSeconds, Integer volumePercent, Integer timeoutFadeOutSeconds);

    Optional<Long> stopAudio(String senseId, Sender sender, Long order, int fadeOutDurationSeconds);

    Optional<Long> mute(String senseId, Sender sender, Long order, boolean mute);
    Optional<Long> setSystemVolume(String senseId, Sender sender, Long order, int volume);
}
