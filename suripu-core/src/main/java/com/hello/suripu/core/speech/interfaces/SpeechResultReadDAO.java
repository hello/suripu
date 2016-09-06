package com.hello.suripu.core.speech.interfaces;

import com.google.common.base.Optional;
import com.hello.suripu.core.speech.models.SpeechResult;

import java.util.List;

/**
 * Created by ksg on 8/30/16
 */
public interface SpeechResultReadDAO {
    Optional<SpeechResult> getItem(final String uuid);
    List<SpeechResult> getItems(final List<String> uuids);
}
