package com.hello.suripu.service.registration;

import com.google.common.base.Optional;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.service.SignedMessage;
import com.hello.suripu.service.resources.ResponseSigner;
import com.librato.rollout.RolloutClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

public class PillRegistration implements ResponseSigner {

    public static int PROTOBUF_VERSION = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(PillRegistration.class);

    @Inject
    RolloutClient featureFlipper;

    private final DeviceDAO deviceDAO;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;
    private final CommonDevice commonDevice;

    private PillRegistration(final DeviceDAO deviceDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB, final CommonDevice commonDevice) {
        this.deviceDAO = deviceDAO;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.commonDevice = commonDevice;
    }

    public static PillRegistration create(final DeviceDAO deviceDAO, final MergedUserInfoDynamoDB mergedUserInfoDynamoDB, final CommonDevice commonDevice) {
        return new PillRegistration(deviceDAO, mergedUserInfoDynamoDB, commonDevice);
    }


    public Optional<SenseCommandProtos.MorpheusCommand> attemptToPair(final byte[] signedBody, final String senseIdFromHeader, @Nullable final String ipAddress) throws Exception {
        final SignedMessage signedMessage = SignedMessage.parse(signedBody);  // This call will throw
        final SenseCommandProtos.MorpheusCommand morpheusCommand = SenseCommandProtos.MorpheusCommand.parseFrom(signedMessage.body);

        final SenseCommandProtos.MorpheusCommand.Builder builder = SenseCommandProtos.MorpheusCommand.newBuilder()
                .setVersion(PillRegistration.PROTOBUF_VERSION);

        final String pillId = morpheusCommand.getDeviceId();
        final String token = morpheusCommand.getAccountId();

        LOGGER.debug("deviceId = {}", senseIdFromHeader);
        LOGGER.debug("token = {}", token);

        final Optional<Long> accountIdOptional = commonDevice.getAccountIdFromTokenString(token, senseIdFromHeader);
        if(!accountIdOptional.isPresent()) {
            return Optional.absent();
        }
        final Long accountId = accountIdOptional.get();

        final String logMessage = String.format("AccountId from protobuf = %d", accountId);
        LOGGER.debug(logMessage);

        // this is only needed for devices with 000... in the header
        // WARNING: MUST BE CLEARED when the buffer is returned to Sense
        builder.setAccountId(token);

        final List<DeviceAccountPair> deviceAccountPairs = this.deviceDAO.getSensesForAccountId(accountId);
        if(deviceAccountPairs.isEmpty()){
            final String errorMessage = String.format("No sense paired with account %d when pill %s tries to register",
                    accountId, pillId);
            LOGGER.error(errorMessage);

            builder.setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR);

            return Optional.of(builder.build());
        }
        final String senseId = deviceAccountPairs.get(0).externalDeviceId;


        if(!commonDevice.validSignature(signedMessage, senseId)) {
            return Optional.absent();
        }

        if(!SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_PILL.equals(morpheusCommand.getType())){
            builder.setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR);

            final String errorMessage = String.format("Wrong request command type %s", morpheusCommand.getType().toString());
            LOGGER.error(errorMessage);
            return Optional.of(builder.build());
        }

        final PairingStatus pairingStatus = pairingStatus(senseId, pillId, accountId);
        final SenseCommandProtos.MorpheusCommand.Builder wrappedBuilder = pair(builder, pairingStatus, senseId, pillId, accountId);
        return Optional.of(wrappedBuilder.build());
    }

    public final PairingStatus pairingStatus(final String senseId, final String pillId, final long accountId){
        final List<DeviceAccountPair> pillsPairedToCurrentAccount = deviceDAO.getPillsForAccountId(accountId);
        final List<DeviceAccountPair> accountsPairedToCurrentPill = deviceDAO.getLinkedAccountFromPillId(pillId);

        final Boolean multiplePillsAllowed = featureFlipper.deviceFeatureActive(FeatureFlipper.DEBUG_MODE_PILL_PAIRING, senseId, Collections.EMPTY_LIST);
        return pairingStatus(pillsPairedToCurrentAccount, accountsPairedToCurrentPill, pillId, accountId, multiplePillsAllowed);
    }


    /**
     * Returns PairingState for given pill
     * @param pillsPairedToCurrentAccount
     * @param accountsPairedToCurrentPill
     * @param pillId
     * @param accountId
     * @param multiplePillAllowed
     * @return
     */
    protected static PairingStatus pairingStatus(final List<DeviceAccountPair> pillsPairedToCurrentAccount, final List<DeviceAccountPair> accountsPairedToCurrentPill, final String pillId, final long accountId, final Boolean multiplePillAllowed){
        if(pillsPairedToCurrentAccount.size() > 1){  // This account already paired with multiple pills

            final String errorMessage = String.format("Account %d already paired with %d pill(s). # accounts paired: %d",
                    accountId,
                    pillsPairedToCurrentAccount.size(),
                    accountsPairedToCurrentPill.size());
            LOGGER.warn(errorMessage);
            return PairingStatus.failed(PairingState.PAIRING_VIOLATION, errorMessage);
        }

        if(accountsPairedToCurrentPill.isEmpty() && pillsPairedToCurrentAccount.isEmpty()){
            return PairingStatus.ok(PairingState.NOT_PAIRED);
        }

        if(accountsPairedToCurrentPill.size() == 1 && pillsPairedToCurrentAccount.size() == 1 && pillsPairedToCurrentAccount.get(0).externalDeviceId.equals(pillId)){
            // might be a firmware retry
            return PairingStatus.ok(PairingState.PAIRED_WITH_CURRENT_ACCOUNT);
        }

        if(multiplePillAllowed) {

            final String debugMessage = String.format("Debug mode for pairing pill %s for account %d.", pillId, accountId);
            LOGGER.info(debugMessage);
            if(pillsPairedToCurrentAccount.isEmpty() /* && accountsPairedToCurrentPill.size() >= 0 */ /* 2nd condition actually not needed */){
                return PairingStatus.ok(PairingState.NOT_PAIRED, debugMessage);
            }

            for(final DeviceAccountPair pill: pillsPairedToCurrentAccount){
                if(pill.externalDeviceId.equals(pillId)){
                    return PairingStatus.ok(PairingState.PAIRED_WITH_CURRENT_ACCOUNT, debugMessage);
                }
            }
            final String errorMessage = String.format("Account %d already paired with %d pills.", accountId, pillsPairedToCurrentAccount.size());
            LOGGER.error(errorMessage);
            return PairingStatus.failed(PairingState.PAIRING_VIOLATION, errorMessage);

        }

        if(accountsPairedToCurrentPill.size() > 1){
            final String errorMessage = String.format("Account %d already paired with multiple pills. pills paired %d, accounts paired %d",
                    accountId,
                    pillsPairedToCurrentAccount.size(),
                    accountsPairedToCurrentPill.size());
            LOGGER.warn(errorMessage);
            return PairingStatus.failed(PairingState.PAIRING_VIOLATION, errorMessage);
        }

        // else:
        if(accountsPairedToCurrentPill.size() == 1 && pillsPairedToCurrentAccount.isEmpty()){
            // pill already paired with an account, but this account is new, stolen pill?
            final String errorMessage  = String.format("Pill %s might got stolen, account %d is a theft!", pillId, accountId);
            LOGGER.error(errorMessage);
            return PairingStatus.failed(PairingState.PAIRING_VIOLATION, errorMessage);
        }

        if(pillsPairedToCurrentAccount.size() == 1 && accountsPairedToCurrentPill.isEmpty()){
            // account already paired with a pill, only one pill is allowed
            final String errorMessage = String.format("Account %d already paired with pill %s. Pill %s cannot pair to this account",
                    accountId,
                    pillsPairedToCurrentAccount.get(0).externalDeviceId,
                    pillId);
            LOGGER.error(errorMessage);
            return PairingStatus.failed(PairingState.PAIRING_VIOLATION, errorMessage);

        }

        final String errorMessage = String.format("Paired failed for account %d. pills paired %d, accounts paired %d",
                accountId,
                pillsPairedToCurrentAccount.size(),
                accountsPairedToCurrentPill.size());
        LOGGER.warn(errorMessage);

        return PairingStatus.failed(PairingState.PAIRING_VIOLATION, errorMessage);
    }


    // TODO: replace builder with command
    public SenseCommandProtos.MorpheusCommand.Builder pair(final SenseCommandProtos.MorpheusCommand.Builder builder, final PairingStatus pairingStatus, final String senseId, final String pillId, final Long accountId) {
        LOGGER.warn("Attempting to pair pill {} to account {}", pillId, accountId);

        if (PairingState.NOT_PAIRED.equals(pairingStatus.pairingState)) {
            deviceDAO.registerPill(accountId, pillId);
            final String message = String.format("Linked pill %s to account %d in DB", pillId, accountId);
            LOGGER.warn(message);

            mergedUserInfoDynamoDB.setNextPillColor(senseId, accountId, pillId);
        }

        if (pairingStatus.isOk()) {
            builder.setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_PILL);
        } else {
            builder.setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED);
            LOGGER.warn("Pill already paired {} ", pillId);
        }
        return builder;
    }

    @Override
    public byte[] signAndSend(String senseId, SenseCommandProtos.MorpheusCommand command) {
        return commonDevice.signAndSend(senseId, command);
    }
}
