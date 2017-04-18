package com.hello.suripu.core.accounts.pairings;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.UserInfo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PairedAccountsTest {

    final DeviceDAO deviceDAO = mock(DeviceDAO.class);
    final AccountDAO accountDAO = mock(AccountDAO.class);
    final MergedUserInfoDAO mergedUserInfoDAO = mock(MergedUserInfoDAO.class);
    final PairedAccounts pairedAccounts = new PairedAccounts(mergedUserInfoDAO, deviceDAO, accountDAO);
    final UUID uuid = UUID.fromString("40c66598-f05f-4db9-b12c-0339099309b2");
            
    final Random r = new Random(System.currentTimeMillis());

    final Long requester = 888L;
    final Long originalOwner = 124L;
    final Long originalOwnerPartner = 234L;
    final String deviceId = "test_Sense";

    final Map<Long, DeviceAccountPair> pairs = ImmutableMap.of(
            requester,  new DeviceAccountPair(requester, 0L, deviceId, DateTime.now(DateTimeZone.UTC)),
            originalOwner,  new DeviceAccountPair(originalOwner, 0L, deviceId, DateTime.now(DateTimeZone.UTC)),
            originalOwnerPartner,  new DeviceAccountPair(originalOwnerPartner, 0L, deviceId, DateTime.now(DateTimeZone.UTC))
    );

    final Map<Long, UserInfo> userInfos = ImmutableMap.of(
            requester, UserInfo.createEmpty(deviceId, requester),
            originalOwner, UserInfo.createEmpty(deviceId, originalOwner),
            originalOwnerPartner, UserInfo.createEmpty(deviceId, originalOwnerPartner)
    );

    private static Optional<Account> makeAccount(final Long id) {
        final Account account = new Account.Builder()
                .withId(id)
                .withExternalId(UUID.randomUUID())
                .withEmail("email@email.com")
                .build();
        return Optional.of(account);
    }


    @Test
    public void testPairedAccountsConsistentPairing() {

        // Setting up initial conditions
        when(deviceDAO.getMostRecentSensePairByAccountId(requester)).thenReturn(Optional.of(pairs.get(requester)));
        when(deviceDAO.getAccountIdsForDeviceId(deviceId)).thenReturn(ImmutableList.copyOf(pairs.values()));
        when(mergedUserInfoDAO.getInfo(deviceId)).thenReturn(Lists.newArrayList(userInfos.values()));


//        when(accountDAO.getById(anyLong())).thenReturn(makeAccount(r.nextLong()));
        when(accountDAO.getById(anyLong())).thenAnswer(invocation -> {
            long id = (long) invocation.getArguments()[0];
            return makeAccount(id);
        });
        
        final List<PairedAccount> pairedAccounts = this.pairedAccounts.to(requester);
        assertFalse("paired account is empty", pairedAccounts.isEmpty());
        assertTrue("should contain 3 accounts", pairedAccounts.size() == userInfos.size());
    }

    @Test
    public void testPairedAccountsNotPairedToSense() {
        when(deviceDAO.getMostRecentSensePairByAccountId(requester)).thenReturn(Optional.absent());
        final List<PairedAccount> pairedAccounts = this.pairedAccounts.to(requester);
        assertTrue("no paired accounts should be found", pairedAccounts.isEmpty());
    }


    @Test
    public void testPairedAccountsSingleUser() {
        when(deviceDAO.getMostRecentSensePairByAccountId(requester)).thenReturn(Optional.of(pairs.get(requester)));
        when(deviceDAO.getAccountIdsForDeviceId(deviceId)).thenReturn(ImmutableList.copyOf(Lists.newArrayList(pairs.get(requester))));
        when(mergedUserInfoDAO.getInfo(deviceId)).thenReturn(Lists.newArrayList(userInfos.get(requester)));
        when(accountDAO.getById(requester)).thenReturn(makeAccount(requester));
        
        final List<PairedAccount> pairedAccounts = this.pairedAccounts.to(requester);

        assertTrue("no paired accounts should be found", pairedAccounts.size() == 1);
    }

    @Test
    public void unpairUserNotPairedToSense() {
        when(deviceDAO.getMostRecentSensePairByAccountId(requester)).thenReturn(Optional.absent());
        final PairedAccounts.UnpairingStatus status = this.pairedAccounts.remove(requester, Lists.newArrayList());
        assertEquals("no sense paired", PairedAccounts.UnpairingStatus.NO_SENSE_PAIRED, status);
    }

    @Test
    public void unpairUserEmptyList() {
        when(deviceDAO.getMostRecentSensePairByAccountId(requester)).thenReturn(Optional.of(pairs.get(requester)));
        when(mergedUserInfoDAO.unlinkAccountToDevice(anyLong(), anyString())).thenReturn(Optional.absent());
        final PairedAccounts.UnpairingStatus status = this.pairedAccounts.remove(requester, Lists.newArrayList());
        assertEquals("all good no user to unpair", PairedAccounts.UnpairingStatus.OK, status);
    }

    @Test
    public void unpairInvalidExternalId() {
        when(deviceDAO.getMostRecentSensePairByAccountId(requester)).thenReturn(Optional.of(pairs.get(requester)));
        when(accountDAO.getByExternalId(uuid)).thenReturn(Optional.absent());

        final List<PairedAccount> accountsToUnpair = Lists.newArrayList(
                PairedAccount.fromExtId("name", uuid.toString(), false)
        );
        final PairedAccounts.UnpairingStatus status = this.pairedAccounts.remove(requester, accountsToUnpair);
        assertEquals("unknown user", PairedAccounts.UnpairingStatus.UNKNOWN_EXTERNAL_ID, status);
    }

    @Test
    public void unpairInvalidPairing() {
        when(deviceDAO.getMostRecentSensePairByAccountId(requester)).thenReturn(Optional.of(pairs.get(requester)));
        final Long accountIdNotPairedToSense = 999L;
        when(accountDAO.getByExternalId(uuid)).thenReturn(makeAccount(accountIdNotPairedToSense));
        when(deviceDAO.getAccountIdsForDeviceId(deviceId)).thenReturn(ImmutableList.copyOf(Lists.newArrayList(pairs.values())));

        final List<PairedAccount> accountsToUnpair = Lists.newArrayList(
                PairedAccount.fromExtId("name", uuid.toString(), false)
        );
        final PairedAccounts.UnpairingStatus status = this.pairedAccounts.remove(requester, accountsToUnpair);
        assertEquals("not paired to same sense", PairedAccounts.UnpairingStatus.NOT_PAIRED_TO_SAME_SENSE, status);
    }
}
