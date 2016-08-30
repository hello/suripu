package com.hello.suripu.core.sense.metadata;

public interface SenseMetadataDAO {
    SenseMetadata get(String senseId);
    Boolean put(SenseMetadata metadata);
}
