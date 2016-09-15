package com.hello.suripu.core.models.sleep_sounds;

import com.google.common.base.Optional;
import com.hello.suripu.core.firmware.HardwareVersion;

/**
 * Created by jakepiccolo on 4/4/16.
 */
public interface SoundMap {
    Optional<Sound> getSoundByFilePath(final String filePath, final HardwareVersion hardwareVersion);
}
