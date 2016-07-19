package com.hello.suripu.core.firmware;

import com.google.common.base.Optional;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FirmwareBuildInfoParserTest {

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
        final String oldFirmwareBuildInfoHighFWVersion = "version: da952d21\n" +
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



        Optional<String> fwVersion = FirmwareBuildInfoParser.parse(oldFirmwareBuildInfo);
        assertThat(fwVersion.isPresent(), is(true));
        assertThat(fwVersion.get(), is("782503713"));

        fwVersion = FirmwareBuildInfoParser.parse(oldFirmwareBuildInfoHighFWVersion);
        assertThat(fwVersion.isPresent(), is(false));

        fwVersion = FirmwareBuildInfoParser.parse(newFirmwareBuildInfo);
        assertThat(fwVersion.isPresent(), is(true));
        assertThat(fwVersion.get(), is("3300"));

        fwVersion = FirmwareBuildInfoParser.parse(newFirmwareBadBuildInfo);
        assertThat(fwVersion.isPresent(), is(true));
        assertThat(fwVersion.get(), is("3300"));

        fwVersion = FirmwareBuildInfoParser.parse(badBuildInfo);
        assertThat(fwVersion.isPresent(), is(false));
    }
}
