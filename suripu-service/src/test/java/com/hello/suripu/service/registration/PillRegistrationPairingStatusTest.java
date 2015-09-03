package com.hello.suripu.service.registration;

import com.google.common.collect.Lists;
import com.hello.suripu.core.models.DeviceAccountPair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PillRegistrationPairingStatusTest {

    private final String senseId = "senseId";
    private final String pillId = "pill";
    private final String otherPillId = "otherPill";
    private final Long accountId = 123L;
    private final Long otherAccount = 456L;

    private DeviceAccountPair createAccountPair(final String externalId, final Long accountId) {
        return new DeviceAccountPair(accountId, 1L, externalId, DateTime.now(DateTimeZone.UTC));
    }

    @Test
    public void testNoPairing() {
        final PairingStatus pairingStatus = PillRegistration.pairingStatus(
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST,
                pillId,
                accountId,
                false
        );
        assertThat(pairingStatus.pairingState, equalTo(PairingState.NOT_PAIRED));
    }

    @Test
    public void testTooManyPillsForAccount() {

        final List<DeviceAccountPair> deviceAccountPairList = Lists.newArrayList(
                createAccountPair(pillId, accountId),
                createAccountPair(pillId, accountId)
        );

        final PairingStatus pairingStatus = PillRegistration.pairingStatus(
                deviceAccountPairList,
                Collections.EMPTY_LIST,
                pillId,
                accountId,
                false
        );
        assertThat(pairingStatus.pairingState, equalTo(PairingState.PAIRING_VIOLATION));
    }


    @Test
    public void testPillPairedToOtherAccount() {
        final List<DeviceAccountPair> accountsPairedToCurrentPill = Lists.newArrayList(
                createAccountPair(pillId, otherAccount)
        );

        final PairingStatus pairingStatus = PillRegistration.pairingStatus(
                Collections.EMPTY_LIST,
                accountsPairedToCurrentPill,
                pillId,
                accountId,
                false
        );
        assertThat(pairingStatus.pairingState, equalTo(PairingState.PAIRING_VIOLATION));
    }


    @Test
    public void testPillAlreadyPairedToSameAccount() {
        final List<DeviceAccountPair> accountsPairedToCurrentPill = Lists.newArrayList(
                createAccountPair(pillId, accountId)
        );

        final List<DeviceAccountPair> pillsPairedToCurrentAccount = Lists.newArrayList(
                createAccountPair(pillId, accountId)
        );

        final PairingStatus pairingStatus = PillRegistration.pairingStatus(
                pillsPairedToCurrentAccount,
                accountsPairedToCurrentPill,
                pillId,
                accountId,
                false
        );
        assertThat(pairingStatus.pairingState, equalTo(PairingState.PAIRED_WITH_CURRENT_ACCOUNT));
    }

    @Test
    public void testPillNotPairedDebugModeOn() {
        final List<DeviceAccountPair> accountsPairedToCurrentPill = Lists.newArrayList(
                createAccountPair(pillId, otherAccount)
        );

        final List<DeviceAccountPair> pillsPairedToCurrentAccount = Lists.newArrayList();

        final PairingStatus pairingStatus = PillRegistration.pairingStatus(
                pillsPairedToCurrentAccount,
                accountsPairedToCurrentPill,
                pillId,
                accountId,
                true
        );
        assertThat(pairingStatus.pairingState, equalTo(PairingState.NOT_PAIRED));
    }


    @Test
    public void testPillAlreadyPairedToSameAccountDebugModeOn() {
        final PairingStatus pairingStatus = PillRegistration.pairingStatus(
                Lists.newArrayList(createAccountPair(pillId, accountId)), // we have same pill already paired
                Collections.EMPTY_LIST, // nobody has this pill paired
                pillId,
                accountId,
                true
        );
        assertThat(pairingStatus.pairingState, equalTo(PairingState.PAIRED_WITH_CURRENT_ACCOUNT));
    }


    @Test
    public void testPillPairingViolationDebugModeOn() {
        final List<DeviceAccountPair> accountsPairedToCurrentPill = Lists.newArrayList(
                createAccountPair(pillId, accountId)
        );

        final PairingStatus pairingStatus = PillRegistration.pairingStatus(
                Lists.newArrayList(createAccountPair(otherPillId, accountId)), // we have another pill paired
                accountsPairedToCurrentPill,
                pillId,
                accountId,
                true
        );
        assertThat(pairingStatus.pairingState, equalTo(PairingState.PAIRING_VIOLATION));
    }
}
