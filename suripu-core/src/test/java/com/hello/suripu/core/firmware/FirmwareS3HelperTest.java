package com.hello.suripu.core.firmware;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FirmwareS3HelperTest {

    @Test
    public void testComputeFileMetadata() {

        final AmazonS3 s3 = mock(AmazonS3.class);
        final FirmwareS3Helper helper = FirmwareS3Helper.create(s3, s3, "bucketname");

        final byte[] content = "hello".getBytes();

        final ByteArrayInputStream stream = new ByteArrayInputStream(content);

        final S3Object s3Object = new S3Object();
        s3Object.setObjectContent(stream);

        when(s3.getObject(any(String.class), any(String.class))).thenReturn(s3Object);
        final FileMetaData metaData = helper.fileMetadata("string");

        final FileMetaData expected = FileMetaData.create(DigestUtils.sha1(content), content.length);

        assertEquals(expected, metaData);
    }
}
