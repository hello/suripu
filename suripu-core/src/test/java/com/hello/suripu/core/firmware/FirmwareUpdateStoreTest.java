package com.hello.suripu.core.firmware;


import com.hello.suripu.api.output.OutputProtos;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jnorgan on 3/16/15.
 */
public class FirmwareUpdateStoreTest {
    @Test
    public void testIsValidFirmwareUpdate(){

        final String sameFirmwareVersion = "12345678";
        final String diffFirmwareVersion = "123456789";
        final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = new ArrayList<>();
        fileDownloadList.add(OutputProtos.SyncResponse.FileDownload.newBuilder().build());

        final FirmwareUpdate firmwareFileList = FirmwareUpdate.create(sameFirmwareVersion, fileDownloadList);
        final FirmwareUpdate emptyFileList = FirmwareUpdate.missing();



        assertThat(FirmwareUpdateStore.isValidFirmwareUpdate(firmwareFileList, sameFirmwareVersion), is(false));
        assertThat(FirmwareUpdateStore.isValidFirmwareUpdate(firmwareFileList, diffFirmwareVersion), is(true));
        assertThat(FirmwareUpdateStore.isValidFirmwareUpdate(emptyFileList, diffFirmwareVersion), is(false));
    }

    @Test
    public void testIsExpiredPresignedUrl(){

        final Long futureSecs = (new Date().getTime() / 1000) + 9999;
        final String expiredUrl = "/sense/fake-build/kitsune.bin?AWSAccessKeyId=ABABABABABABABA&Expires=11111111&Signature=ffffffffffff";
        final String validUrl = "/sense/fake-build/kitsune.bin?AWSAccessKeyId=ABABABABABABABA&Expires=" + futureSecs + "&Signature=ffffffffffff";
        final String badUrl = "/sense/fake-build/kitsune.bin?AWSAccessKeyId=ABABABABABABABA&Signature=ffffffffffff";
        final Date now = new Date();

        assertThat(FirmwareUpdateStore.isExpiredPresignedUrl(expiredUrl, now), is(true));
        assertThat(FirmwareUpdateStore.isExpiredPresignedUrl(validUrl, now), is(false));
        assertThat(FirmwareUpdateStore.isExpiredPresignedUrl(badUrl, now), is(true));
        now.setTime(futureSecs * 1001);
        assertThat(FirmwareUpdateStore.isExpiredPresignedUrl(validUrl, now), is(true));
        assertThat(FirmwareUpdateStore.isExpiredPresignedUrl(validUrl, null), is(true));
    }


}
