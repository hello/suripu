package com.hello.suripu.core.firmware;


import com.google.common.base.Optional;

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

        final String sameFirmwareVersion = "12345678";
        final String diffFirmwareVersion = "123456789";
        final List<OutputProtos.SyncResponse.FileDownload> fileDownloadList = new ArrayList<>();
        fileDownloadList.add(OutputProtos.SyncResponse.FileDownload.newBuilder().build());

        final Pair<String, List<OutputProtos.SyncResponse.FileDownload>> firmwareFileList = new Pair<>(sameFirmwareVersion, fileDownloadList);
        final Pair<String, List<OutputProtos.SyncResponse.FileDownload>> emptyFileList = new Pair("0", Collections.EMPTY_LIST);



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

    @Test
    public void testGetFirmwareFromBuildInfo() {
        final String oldFirmwareBuildInfo = "version: 2ea40f21\n" +
            "last_tag: 1.0.5.3.4\n" +
            "travis_branch: 1.0.5.3.4\n" +
            "travis_tag: 1.0.5.3.4\n" +
            "travis_build_id: 19787387\n" +
            "travis_build_number: 3278\n" +
            "travis_job_id: 30690259\n" +
            "travis_job_number: 3278.1";
        final String newFirmwareBuildInfo = "version: 3300\n" +
            "last_tag: 1.0.5.3.3\n" +
            "travis_branch: alphas\n" +
            "travis_tag: \n" +
            "travis_build_id: 19896757\n" +
            "travis_build_number: 3300\n" +
            "travis_job_id: 30867510\n" +
            "travis_job_number: 3300.1";
        final String newFirmwareBadBuildInfo = "version: 3300\n" +
            "last_tag: 1.0.5.3.3\n" +
            "travis_branch: alphas\n" +
            "travis_tag: \n" +
            "travis_build_id: 19896757\n" +
            "travis_build_number: 9999\n" +
            "travis_job_id: 30867510\n" +
            "travis_job_number: 3300.1";
        final String badBuildInfo = "last_tag: 1.0.5.3.3\n" +
            "travis_branch: alphas\n" +
            "travis_tag: \n" +
            "travis_build_id: 19896757\n" +
            "travis_build_number: 9999\n" +
            "travis_job_id: 30867510\n" +
            "travis_job_number: 3300.1";

        Optional<String> fwVersion = FirmwareUpdateStore.getFirmwareVersionFromBuildInfo(oldFirmwareBuildInfo);
        assertThat(fwVersion.isPresent(), is(true));
        assertThat(fwVersion.get(), is("782503713"));

        fwVersion = FirmwareUpdateStore.getFirmwareVersionFromBuildInfo(newFirmwareBuildInfo);
        assertThat(fwVersion.isPresent(), is(true));
        assertThat(fwVersion.get(), is("3300"));

        fwVersion = FirmwareUpdateStore.getFirmwareVersionFromBuildInfo(newFirmwareBadBuildInfo);
        assertThat(fwVersion.isPresent(), is(true));
        assertThat(fwVersion.get(), is("3300"));

        fwVersion = FirmwareUpdateStore.getFirmwareVersionFromBuildInfo(badBuildInfo);
        assertThat(fwVersion.isPresent(), is(false));
    }
}
