package com.hello.suripu.core.firmware;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileMetaDataTest {

    @Test
    public void testEqualAndHashCode() {
        final byte[] sha = "hello".getBytes();
        final FileMetaData metaData = FileMetaData.create(sha, sha.length);

        final byte[] otherSha = "hello".getBytes();
        final FileMetaData otherMetaData = FileMetaData.create(otherSha, otherSha.length);

        assertEquals(metaData, otherMetaData);
        assertEquals(metaData.hashCode(), otherMetaData.hashCode());
    }
}
