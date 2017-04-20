package com.hello.suripu.core.accounts.pairings;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.models.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PairedAccounts {

    private final static Logger LOGGER = LoggerFactory.getLogger(PairedAccounts.class);
    
    private final MergedUserInfoDAO mergedUserInfoDAO;
    private final DeviceDAO deviceDAO;
    private final AccountDAO accountDAO;

    public PairedAccounts(final MergedUserInfoDAO mergedUserInfoDAO, final DeviceDAO deviceDAO, final AccountDAO accountDAO) {
        this.mergedUserInfoDAO = mergedUserInfoDAO;
        this.deviceDAO = deviceDAO;
        this.accountDAO = accountDAO;
    }

    public List<PairedAccount> to(final Long accountId) {
        final Optional<DeviceAccountPair> pairedTo = deviceDAO.getMostRecentSensePairByAccountId(accountId);
        if(!pairedTo.isPresent()) {
            return new ArrayList<>();
        }

        final String senseId = pairedTo.get().externalDeviceId;
        final List<UserInfo> userInfoList = mergedUserInfoDAO.getInfo(senseId);
        final List<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(senseId);
        final Set<Long> accountIdsInDynamo = userInfoList.stream().map(u -> u.accountId).collect(Collectors.toSet());
        final Set<Long> accountsIdsInPostgres = pairs.stream().map(p -> p.accountId).collect(Collectors.toSet());


        for(final Long idInDynamo: accountIdsInDynamo) {
            // it's in dynamo and not in postgres
            if(!accountsIdsInPostgres.contains(idInDynamo)) {
                LOGGER.warn("action=unlink-account-to-device msg=dynamo-postgres-out-of-sync account_id={} sense_id={}", idInDynamo, senseId);
                final Optional<UserInfo> oldUserInfo = mergedUserInfoDAO.unlinkAccountToDevice(idInDynamo, senseId);
                if(oldUserInfo.isPresent()) {
                    accountIdsInDynamo.remove(idInDynamo);
                }
            }
        }

        if(accountIdsInDynamo.size() != accountsIdsInPostgres.size()) {
            //TODO: deal with inconsistent pairings
            LOGGER.warn("action=fetch-paired-accounts requester={} sense_id={}", accountId, senseId);
        }

        // Use postgres view
        final List<PairedAccount> pairedAccounts = accountsIdsInPostgres.stream()
                .map(a -> accountDAO.getById(a)) // query from DB
                .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()) // remove missing accounts but always return a stream
                .map(a -> PairedAccount.from(a, accountId)) // map to a paired account
                .collect(Collectors.toList());

        return pairedAccounts;
    }



    public UnpairingStatus remove(final Long requesterId, final List<PairedAccount> accountsToUnpair) {
        final Optional<DeviceAccountPair> pairedTo = deviceDAO.getMostRecentSensePairByAccountId(requesterId);
        if(!pairedTo.isPresent()) {
            return UnpairingStatus.NO_SENSE_PAIRED;
        }


        final Optional<Account> owner = accountDAO.getById(requesterId);
        if(!owner.isPresent()) {
            return UnpairingStatus.UNKNOWN_OWNER;
        }

        // Filter out owner from the list of accounts to unpair.
        final List<PairedAccount> accountsExcludingRequester = accountsToUnpair.stream()
                .filter(a -> !a.id().equals(owner.get().extId()))  // filter keeps items where condition is true
                .collect(Collectors.toList());

        for(final PairedAccount accountToUnpair : accountsExcludingRequester) {

            final Optional<Account> accountOptional = accountDAO.getByExternalId(UUID.fromString(accountToUnpair.id()));
            if(!accountOptional.isPresent()) {
                LOGGER.error("action=unpair-accounts result=account-not-found paired_to={} account_ext_id={}", requesterId, accountToUnpair.id());
                return UnpairingStatus.UNKNOWN_EXTERNAL_ID;
            }

            final Long accountIdToUnpair = accountOptional.get().id.or(0L);
            final String senseId = pairedTo.get().externalDeviceId;

            // check this account was paired to this Sense
            final List<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(senseId);
            final Set<Long> pairedIds = pairs.stream().map(d -> d.accountId).collect(Collectors.toSet());
            if(!pairedIds.contains(accountIdToUnpair)) {
                LOGGER.error("error=invalid-pairing");
                return UnpairingStatus.NOT_PAIRED_TO_SAME_SENSE;
            }

            final Optional<UserInfo> userInfo = mergedUserInfoDAO.unlinkAccountToDevice(accountIdToUnpair, senseId);
            if(!userInfo.isPresent()) {
                LOGGER.error("error=failed-to-unlink action=unlink-account-to-device account_id={} sense_id={}", accountIdToUnpair, senseId);
            }
            final int success = deviceDAO.deleteSensePairing(senseId, accountIdToUnpair);
            if(success == 0) {
                LOGGER.error("action=delete-sense-pairing account_id={} sense_id={}", accountIdToUnpair, senseId);
            }
        }

        return UnpairingStatus.OK;
    }
}
