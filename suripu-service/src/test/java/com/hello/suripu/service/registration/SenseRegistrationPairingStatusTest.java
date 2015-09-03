package com.hello.suripu.service.registration;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceAccountPair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SenseRegistrationPairingStatusTest {

    private final String senseId = "SenseId";
    private final String otherSenseId = "OtherSenseId";
    private final Long accountId = 123L;
    private final Long otherAccountId = 123L;

    private DeviceAccountPair createAccountPair(final String externalId, final Long accountId) {
        return new DeviceAccountPair(accountId, 1L, externalId, DateTime.now(DateTimeZone.UTC));
    }

    @Test
    public void testPairingNoSensePairedToCurrentAccount() {
        final List<DeviceAccountPair> accountPairs = Lists.newArrayList();
        final PairingStatus pairingStatus = SenseRegistration.pairingStatus(accountPairs, senseId);
        assertThat(PairingState.NOT_PAIRED, equalTo(pairingStatus.pairingState));
    }

    @Test
    public void testPairingSameSensePairedToCurrentAccount() {
        final List<DeviceAccountPair> accountPairs = Lists.newArrayList(
                createAccountPair(senseId, accountId)
        );
        final PairingStatus pairingStatus = SenseRegistration.pairingStatus(accountPairs, senseId);
        assertThat(PairingState.PAIRED_WITH_CURRENT_ACCOUNT, equalTo(pairingStatus.pairingState));
    }

    @Test
    public void testPairingSensePairedToOtherAccount() {
        final List<DeviceAccountPair> accountPairs = Lists.newArrayList(
                createAccountPair(otherSenseId, otherAccountId)
        );
        final PairingStatus pairingStatus = SenseRegistration.pairingStatus(accountPairs, senseId);
        assertThat(PairingState.PAIRING_VIOLATION, equalTo(pairingStatus.pairingState));
    }
}
