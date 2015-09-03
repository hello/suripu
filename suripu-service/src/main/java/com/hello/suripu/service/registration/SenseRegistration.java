package com.hello.suripu.service.registration;

import com.google.common.base.Optional;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.service.SignedMessage;
import com.hello.suripu.service.resources.ResponseSigner;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.regex.Matcher;

import static com.google.common.base.Preconditions.checkNotNull;

public class SenseRegistration implements ResponseSigner {

    public static int PROTOBUF_VERSION = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(SenseRegistration.class);
    private final DeviceDAO deviceDAO;
    private final CommonDevice commonDevice;

    private SenseRegistration(final DeviceDAO deviceDAO, final CommonDevice commonDevice) {
        this.deviceDAO = deviceDAO;
        this.commonDevice = commonDevice;
    }


    public static SenseRegistration create(final DeviceDAO deviceDAO, final CommonDevice commonDevice) {
        return new SenseRegistration(deviceDAO, commonDevice);
    }

    public Optional<SenseCommandProtos.MorpheusCommand> attemptToPair(final byte[] signedBody) throws Exception {
        final SenseCommandProtos.MorpheusCommand.Builder builder = SenseCommandProtos.MorpheusCommand.newBuilder()
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(PROTOBUF_VERSION);

        final SignedMessage signedMessage = SignedMessage.parse(signedBody);  // This call will throw
        final SenseCommandProtos.MorpheusCommand morpheusCommand = SenseCommandProtos.MorpheusCommand.parseFrom(signedMessage.body);

        final String senseId = morpheusCommand.getDeviceId();
        builder.setDeviceId(senseId);

        final String token = morpheusCommand.getAccountId();

        LOGGER.debug("deviceId = {}", senseId);
        LOGGER.debug("token = {}", token);


        final Optional<Long> accountIdOptional = commonDevice.getAccountIdFromTokenString(token, senseId);
        if(!accountIdOptional.isPresent()) {
            return Optional.absent();
        }
        final Long accountId = accountIdOptional.get();

        final String logMessage = String.format("AccountId from protobuf = %d", accountId);
        LOGGER.debug(logMessage);;

        // this is only needed for devices with 000... in the header
        // WARNING: MUST BE CLEARED when the buffer is returned to Sense
        builder.setAccountId(token);

        if(!commonDevice.validSignature(signedMessage, senseId)) {
            return Optional.absent();
        }

        final PairingStatus pairStatus = pairingStatus(senseId, accountId);
        return pair(builder.build(), senseId, accountId, pairStatus);
    }

    public Optional<SenseCommandProtos.MorpheusCommand> pair(final SenseCommandProtos.MorpheusCommand command, final String senseId, final Long accountId, final PairingStatus pairingStatus) {
        try {

            if (PairingState.NOT_PAIRED.equals(pairingStatus.pairingState)) {
                deviceDAO.registerSense(accountId, senseId);
            }

        } catch (UnableToExecuteStatementException sqlExp){
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(sqlExp.getMessage());

            final SenseCommandProtos.ErrorType errorType = matcher.find()
                    ? SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED
                    : SenseCommandProtos.ErrorType.INTERNAL_OPERATION_FAILED;

            final SenseCommandProtos.MorpheusCommand.Builder builder = SenseCommandProtos.MorpheusCommand
                    .newBuilder(command)
                    .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR)
                    .setError(errorType);

            final String errorMessage = String.format("SQL error %s", sqlExp.getMessage());
            LOGGER.error(errorMessage);

            return Optional.of(builder.build());
        }

        final SenseCommandProtos.MorpheusCommand.Builder builder = SenseCommandProtos.MorpheusCommand
                .newBuilder(command);

        if(pairingStatus.isOk()) {
            builder.setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE);
        } else {
            builder.setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED);
        }

        return Optional.of(builder.build());
    }

    /**
     * @param senseId
     * @param accountId
     * @return
     */
    public PairingStatus pairingStatus(final String senseId, final Long accountId){
        final List<DeviceAccountPair> pairedSenses = deviceDAO.getSensesForAccountId(accountId);
        return pairingStatus(pairedSenses, senseId);
    }


    protected static PairingStatus pairingStatus(@NotNull final List<DeviceAccountPair> pairedSenses, @NotNull final String senseId){
        checkNotNull(pairedSenses, "pairedSenses can't be null");
        checkNotNull(senseId, "senseId can't be null");
        if(pairedSenses.size() > 1){  // This account already paired with multiple senses
            return PairingStatus.failed(PairingState.PAIRING_VIOLATION, "Account has multiple senses");
        }

        if(pairedSenses.isEmpty()){
            return PairingStatus.ok(PairingState.NOT_PAIRED);
        }

        if(pairedSenses.get(0).externalDeviceId.equals(senseId)){
            return PairingStatus.ok(PairingState.PAIRED_WITH_CURRENT_ACCOUNT, "only one sense, and it is current sense, firmware retry request");
        }

        // already paired with another one.
        return PairingStatus.failed(PairingState.PAIRING_VIOLATION, "already paired with another account");
    }

    @Override
    public byte[] signAndSend(final String senseId, final SenseCommandProtos.MorpheusCommand command) {
       return commonDevice.signAndSend(senseId, command);
    }
}
