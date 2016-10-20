package com.hello.suripu.core.sense.voice;

import com.google.common.base.Optional;

public interface VoiceMetadataDAO {

    Optional<Long> getPrimaryAccount(String senseId);
    VoiceMetadata get(String senseId, Long currentAccount, Long primaryAccount);
    void updatePrimaryAccount(String senseId, long accountId);
}
