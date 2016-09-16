package com.hello.suripu.core.sense.metadata;

import com.google.common.base.Optional;

public interface SenseMetadataDAO {
    SenseMetadata get(String senseId);
    Optional<SenseMetadata> getMaybe(String senseId);
    Integer put(SenseMetadata metadata);
}
