package com.hello.suripu.core.firmware;


import com.hello.suripu.api.output.OutputProtos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jnorgan on 3/16/15.
 */
public class FirmwareUpdateStoreTest {
    @Test
    public void testIsValidFirmwareUpdate(){

        final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = new ArrayList<>();
        final OutputProtos.SyncResponse.FileDownload.Builder fileDownloadBuilder = OutputProtos.SyncResponse.FileDownload.newBuilder();
        final boolean copyToSerialFlash = true;
        final boolean resetApplicationProcessor = true;
        final String serialFlashFilename = "mcuimgx.bin";
        final String serialFlashPath = "/sys/";
        final String sdCardFilename = "mcuimgx.bin";
        final String sdCardPath = "/";

        fileDownloadBuilder.setCopyToSerialFlash(copyToSerialFlash);
        fileDownloadBuilder.setResetApplicationProcessor(resetApplicationProcessor);
        fileDownloadBuilder.setSerialFlashFilename(serialFlashFilename);
        fileDownloadBuilder.setSerialFlashPath(serialFlashPath);
        fileDownloadBuilder.setSdCardFilename(sdCardFilename);
        fileDownloadBuilder.setSdCardPath(sdCardPath);


        fileDownloadList.add(fileDownloadBuilder.build());
        final Pair<Integer, List<OutputProtos.SyncResponse.FileDownload>> firmwareFileList = new Pair<>(12345678, fileDownloadList);
        final Pair<Integer, List<OutputProtos.SyncResponse.FileDownload>> emptyFileList = new Pair(-1, Collections.EMPTY_LIST);
        final Integer sameFirmwareVersion = 12345678;
        final Integer diffFirmwareVersion = 123456789;

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
    }
}
