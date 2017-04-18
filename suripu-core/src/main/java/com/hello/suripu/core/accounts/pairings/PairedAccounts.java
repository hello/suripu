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

import static com.hello.suripu.core.accounts.pairings.PairedAccounts.UnpairingStatus.DB_OUT_OF_SYNC;
import static com.hello.suripu.core.accounts.pairings.PairedAccounts.UnpairingStatus.OK;

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

        final List<UserInfo> userInfoList = mergedUserInfoDAO.getInfo(pairedTo.get().externalDeviceId);
        final List<DeviceAccountPair> pairs = deviceDAO.getAccountIdsForDeviceId(pairedTo.get().externalDeviceId);
        final Set<Long> accountIdsInDynamo = userInfoList.stream().map(u -> u.accountId).collect(Collectors.toSet());
        final Set<Long> accountsIdsInPostgres = pairs.stream().map(p -> p.accountId).collect(Collectors.toSet());


        if(accountIdsInDynamo.size() != accountsIdsInPostgres.size()) {
            //TODO: deal with inconsistent pairings
            LOGGER.warn("action=fetch-paired-accounts requester={} sense_id={}", accountId, pairedTo.get().externalDeviceId);
        }

        // Use dynamo view
        final List<PairedAccount> pairedAccounts = accountIdsInDynamo.stream()
                .map(a -> accountDAO.getById(a)) // query from DB
                .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()) // remove missing accounts but always return a stream
                .map(a -> PairedAccount.from(a, accountId)) // map to a paired account
                .collect(Collectors.toList());

        return pairedAccounts;
    }

    public enum UnpairingStatus {
        OK,
        DB_OUT_OF_SYNC,
        NO_SENSE_PAIRED,
        UNKNOWN_EXTERNAL_ID,
        NOT_PAIRED_TO_SAME_SENSE
    }

    public UnpairingStatus remove(final Long requesterId, final List<PairedAccount> accountsToUnpair) {
        final Optional<DeviceAccountPair> pairedTo = deviceDAO.getMostRecentSensePairByAccountId(requesterId);
        if(!pairedTo.isPresent()) {
            return UnpairingStatus.NO_SENSE_PAIRED;
        }

        for(final PairedAccount accountToUnpair : accountsToUnpair) {

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
                return DB_OUT_OF_SYNC;
            }
            final int success = deviceDAO.deleteSensePairing(senseId, accountIdToUnpair);
            if(success == 0) {
                LOGGER.error("action=delete-sense-pairing account_id={} sense_id={}", accountIdToUnpair, senseId);
                return DB_OUT_OF_SYNC;
            }
        }

        return OK;
    }
}
