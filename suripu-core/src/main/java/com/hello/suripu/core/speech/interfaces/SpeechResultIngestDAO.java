package com.hello.suripu.core.speech.interfaces;

import com.hello.suripu.core.speech.models.SpeechResult;

/**
 * Created by ksg on 8/30/16
 */
public interface SpeechResultIngestDAO {
    Boolean putItem(SpeechResult speechResult);
    Boolean updateItem(SpeechResult speechResult);
}
