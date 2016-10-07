package com.hello.suripu.core.sense.metadata;

public interface SenseMetadataDAO {
    SenseMetadata get(String senseId);
    Integer put(SenseMetadata metadata);
}
