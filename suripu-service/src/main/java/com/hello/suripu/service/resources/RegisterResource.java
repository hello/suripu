package com.hello.suripu.service.resources;

import com.amazonaws.AmazonServiceException;
import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.api.ble.SenseCommandProtos.MorpheusCommand;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.flipper.FeatureFlipper;
import com.hello.suripu.core.flipper.GroupFlipper;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.service.SignedMessage;
import com.librato.rollout.RolloutClient;
import com.yammer.metrics.annotation.Timed;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by pangwu on 10/10/14.
 */
@Path("/register")
public class RegisterResource extends BaseResource {
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");
    private static int PROTOBUF_VERSION = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterResource.class);
    private final DeviceDAO deviceDAO;
    final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore;
    private final KinesisLoggerFactory kinesisLoggerFactory;
    private final KeyStore senseKeyStore;
    private final MergedUserInfoDynamoDB mergedUserInfoDynamoDB;

    private final Boolean debug;

    private enum PairState{
        NOT_PAIRED,
        PAIRED_WITH_CURRENT_ACCOUNT,
        PAIRING_VIOLATION;
    }

    @Context
    HttpServletRequest request;

    @Inject
    RolloutClient featureFlipper;

    private final GroupFlipper groupFlipper;

    private static enum PairAction{
        PAIR_MORPHEUS,
        PAIR_PILL
    }

    public RegisterResource(final DeviceDAO deviceDAO,
                            final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore,
                            final KinesisLoggerFactory kinesisLoggerFactory,
                            final KeyStore senseKeyStore,
                            final MergedUserInfoDynamoDB mergedUserInfoDynamoDB,
                            final GroupFlipper groupFlipper,
                            final Boolean debug){
        this.deviceDAO = deviceDAO;
        this.tokenStore = tokenStore;
        this.debug = debug;
        this.kinesisLoggerFactory = kinesisLoggerFactory;
        this.senseKeyStore = senseKeyStore;
        this.mergedUserInfoDynamoDB = mergedUserInfoDynamoDB;
        this.groupFlipper = groupFlipper;
    }

    private boolean checkCommandType(final MorpheusCommand morpheusCommand, final PairAction action){
        switch (action){
            case PAIR_PILL:
                return morpheusCommand.getType() == MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_PILL;
            case PAIR_MORPHEUS:
                return morpheusCommand.getType() == MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE;
            default:
                return false;
        }
    }

    private void setPillColor(final String senseId, final long accountId, final String pillId){

        try {
            // WARNING: potential race condition here.
            final Optional<Color> pillColor = this.mergedUserInfoDynamoDB.setNextPillColor(senseId, accountId, pillId);
            LOGGER.info("Pill {} set to color {} on sense {}", pillId, pillColor.get(), senseId);
        }catch (AmazonServiceException ase){
            LOGGER.error("Set pill {} color for sense {} faile: {}", pillId, senseId, ase.getErrorMessage());
        }
    }

    private PairState getSensePairingState(final String senseId, final long accountId){
        final List<DeviceAccountPair> pairedSense = this.deviceDAO.getSensesForAccountId(accountId);
        if(pairedSense.size() > 1){  // This account already paired with multiple senses
            return PairState.PAIRING_VIOLATION;
        }

        if(pairedSense.size() == 0){
            return PairState.NOT_PAIRED;
        }

        if(pairedSense.get(0).externalDeviceId.equals(senseId)){
            return PairState.PAIRED_WITH_CURRENT_ACCOUNT;  // only one sense, and it is current sense, firmware retry request
        }else{
            return PairState.PAIRING_VIOLATION;  // already paired with another one.
        }
    }

    private PairState getPillPairingState(final String senseId, final String pillId, final long accountId){
        final List<DeviceAccountPair> pillsPairedToCurrentAccount = this.deviceDAO.getPillsForAccountId(accountId);
        final List<DeviceAccountPair> accountsPairedToCurrentPill = this.deviceDAO.getLinkedAccountFromPillId(pillId);
        if(pillsPairedToCurrentAccount.size() > 1){  // This account already paired with multiple pills
            LOGGER.warn("Account {} already paired with multiple pills. pills paired {}, accounts paired {}",
                    accountId,
                    pillsPairedToCurrentAccount.size(),
                    accountsPairedToCurrentPill.size());
            return PairState.PAIRING_VIOLATION;
        }

        if(accountsPairedToCurrentPill.size() == 0 && pillsPairedToCurrentAccount.size() == 0){
            return PairState.NOT_PAIRED;
        }

        if(accountsPairedToCurrentPill.size() == 1 && pillsPairedToCurrentAccount.size() == 1 && pillsPairedToCurrentAccount.get(0).externalDeviceId.equals(pillId)){
            // might be a firmware retry
            return PairState.PAIRED_WITH_CURRENT_ACCOUNT;
        }

        final List<String> groups = groupFlipper.getGroups(senseId);
        if(featureFlipper.deviceFeatureActive(FeatureFlipper.DEBUG_MODE_PILL_PAIRING, senseId, groups)) {
            LOGGER.info("Debug mode for pairing pill {} to sense {}.", pillId, senseId);
            if(pillsPairedToCurrentAccount.size() == 0 /* && accountsPairedToCurrentPill.size() >= 0 */ /* 2nd condition actually not needed */){
                return PairState.NOT_PAIRED;
            }else{
                for(final DeviceAccountPair pill:pillsPairedToCurrentAccount){
                    if(pill.externalDeviceId.equals(pillId)){
                        return PairState.PAIRED_WITH_CURRENT_ACCOUNT;
                    }
                }
                LOGGER.error("Debug mode: account {} already paired with {} pills.", accountId, pillsPairedToCurrentAccount.size());
                return PairState.PAIRING_VIOLATION;
            }
        }

        if(accountsPairedToCurrentPill.size() > 1){
            LOGGER.warn("Account {} already paired with multiple pills. pills paired {}, accounts paired {}",
                    accountId,
                    pillsPairedToCurrentAccount.size(),
                    accountsPairedToCurrentPill.size());
            return PairState.PAIRING_VIOLATION;
        }

        // else:
        if(accountsPairedToCurrentPill.size() == 1 && pillsPairedToCurrentAccount.size() == 0){
            // pill already paired with an account, but this account is new, stolen pill?
            LOGGER.error("Pill {} might got stolen, account {} is a theft!", pillId, accountId);
        }
        if(pillsPairedToCurrentAccount.size() == 1 && accountsPairedToCurrentPill.size() == 0){
            // account already paired with a pill, only one pill is allowed
            LOGGER.error("Account {} already paired with pill {}", accountId, pillsPairedToCurrentAccount.get(0).externalDeviceId);
        }

        LOGGER.warn("Paired failed for account {}. pills paired {}, accounts paired {}",
                accountId,
                pillsPairedToCurrentAccount.size(),
                accountsPairedToCurrentPill.size());
        return PairState.PAIRING_VIOLATION;

    }

    private MorpheusCommand.Builder pair(final byte[] encryptedRequest, final KeyStore keyStore, final PairAction action) {

        final MorpheusCommand.Builder builder = MorpheusCommand.newBuilder()
                .setVersion(PROTOBUF_VERSION);

        final SignedMessage signedMessage = SignedMessage.parse(encryptedRequest);
        MorpheusCommand morpheusCommand = MorpheusCommand.getDefaultInstance();
        try {
            morpheusCommand = MorpheusCommand.parseFrom(signedMessage.body);

        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);
            // We can't return a proper error because we can't decode the protobuf
            throwPlainTextError(Response.Status.BAD_REQUEST, "");
        }

        final String deviceId = morpheusCommand.getDeviceId();
        builder.setDeviceId(deviceId);

        String senseId = "";
        String pillId = "";

        final String token = morpheusCommand.getAccountId();
        LOGGER.debug("deviceId = {}", deviceId);
        LOGGER.debug("token = {}", token);

        final Optional<AccessToken> accessTokenOptional = this.tokenStore.getClientDetailsByToken(
                new ClientCredentials(new OAuthScope[]{OAuthScope.AUTH}, token),
                DateTime.now());

        if(!accessTokenOptional.isPresent()) {
            builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.INTERNAL_OPERATION_FAILED);
            LOGGER.error("Token not found {} for device Id {}", token, deviceId);
            return builder;
        }

        final Long accountId = accessTokenOptional.get().accountId;
        LOGGER.debug("accountId = {}", accountId);
        builder.setAccountId(token); // this is only needed for devices with 000... in the header

        switch (action) {
            case PAIR_MORPHEUS:
                senseId = deviceId;
                break;
            case PAIR_PILL:
                pillId = deviceId;
                final List<DeviceAccountPair> deviceAccountPairs = this.deviceDAO.getSensesForAccountId(accountId);
                if(deviceAccountPairs.size() == 0){
                    LOGGER.error("No sense paired with account {} when pill {} tries to register", accountId, pillId);
                    builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                    builder.setError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR);
                    return builder;
                }
                senseId = deviceAccountPairs.get(0).externalDeviceId;
                break;
        }

        final Optional<byte[]> keyBytesOptional = keyStore.get(senseId);
        if(!keyBytesOptional.isPresent()) {
            LOGGER.error("Missing AES key for device = {}", senseId);
            throwPlainTextError(Response.Status.UNAUTHORIZED, "");
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytesOptional.get());

        if(error.isPresent()) {
            LOGGER.error("Fail to validate signature {}", error.get().message);
            throwPlainTextError(Response.Status.UNAUTHORIZED, "");
        }

        if(!checkCommandType(morpheusCommand, action)){
            builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR);
            LOGGER.error("Wrong request command type {}", morpheusCommand.getType());
            return builder;
        }

        try {
            switch (action){
                case PAIR_MORPHEUS: {
                    final PairState pairState = getSensePairingState(senseId, accountId);
                    if (pairState == PairState.NOT_PAIRED) {
                        this.deviceDAO.registerSense(accountId, senseId);
                    }

                    if (pairState == PairState.NOT_PAIRED || pairState == PairState.PAIRED_WITH_CURRENT_ACCOUNT) {
                        builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE);
                    } else {
                        LOGGER.error("Account {} tries to pair multiple senses", accountId);
                        builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                        builder.setError(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED);
                    }
                }
                break;
                case PAIR_PILL: {
                    LOGGER.warn("Attempting to pair pill {} to account {}", pillId, accountId);
                    final PairState pairState = getPillPairingState(senseId, pillId, accountId);
                    if (pairState == PairState.NOT_PAIRED) {
                        this.deviceDAO.registerPill(accountId, deviceId);
                        LOGGER.warn("Registered pill {} to account {}", pillId, accountId);
                        this.setPillColor(senseId, accountId, deviceId);
                    }

                    if (pairState == PairState.NOT_PAIRED || pairState == PairState.PAIRED_WITH_CURRENT_ACCOUNT) {
                        builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_PILL);
                    } else {
                        builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                        builder.setError(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED);
                        LOGGER.warn("Pill already paired {} ", pillId);
                    }
                }
                break;
            }
            //builder.setAccountId(morpheusCommand.getAccountId());

        } catch (UnableToExecuteStatementException sqlExp){
            final Matcher matcher = PG_UNIQ_PATTERN.matcher(sqlExp.getMessage());
            if (!matcher.find()) {
                LOGGER.error("SQL error {}", sqlExp.getMessage());
                builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                builder.setError(SenseCommandProtos.ErrorType.INTERNAL_OPERATION_FAILED);
            }else {
                LOGGER.error(sqlExp.getMessage());
                //TODO: enforce the constraint
                LOGGER.warn("Account {} tries to pair a paired device {} ",
                        accountId, deviceId);
                builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                builder.setError(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED);
            }
        }

        try {
            final String ip = request.getHeader("X-Forwarded-For");
            LoggingProtos.Registration.Builder registration = LoggingProtos.Registration.newBuilder()
                    .setAccountId(accountId)
                    .setDeviceId(deviceId)
                    .setTimestamp(DateTime.now().getMillis());

            if (ip != null) {
                registration.setIpAddress(ip);
            }

            final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.REGISTRATIONS);
            dataLogger.put(accountId.toString(), registration.build().toByteArray());
        } catch (Exception e) {
            LOGGER.error("Failed inserting registration into kinesis stream: {}", e.getMessage());
        }

        return builder;
    }

    private byte[] signAndSend(final String senseId, final MorpheusCommand.Builder morpheusCommandBuilder, final KeyStore keyStore) {
        final Optional<byte[]> keyBytesOptional = keyStore.get(senseId);
        if(!keyBytesOptional.isPresent()) {
            LOGGER.error("Missing AES key for deviceId = {}", senseId);
            return plainTextError(Response.Status.INTERNAL_SERVER_ERROR, "");
        }
        LOGGER.trace("Key used to sign device {} : {}", senseId, Hex.encodeHexString(keyBytesOptional.get()));

        final Optional<byte[]> signedResponse = SignedMessage.sign(morpheusCommandBuilder.build().toByteArray(), keyBytesOptional.get());
        if(!signedResponse.isPresent()) {
            LOGGER.error("Failed signing message for deviceId = {}", senseId);
            return plainTextError(Response.Status.INTERNAL_SERVER_ERROR, "");
        }

        return signedResponse.get();
    }

    @POST
    @Path("/morpheus")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Deprecated
    public byte[] registerMorpheus(final byte[] body) {
        final String senseIdFromHeader = this.request.getHeader(HelloHttpHeader.SENSE_ID);
        if(senseIdFromHeader != null){
            LOGGER.info("Sense Id from http header {}", senseIdFromHeader);
        }
        final MorpheusCommand.Builder builder = pair(body, senseKeyStore, PairAction.PAIR_MORPHEUS);

        if(senseIdFromHeader != null && senseIdFromHeader.equals(KeyStoreDynamoDB.DEFAULT_FACTORY_DEVICE_ID)){
            senseKeyStore.put(builder.getDeviceId(), Hex.encodeHexString(KeyStoreDynamoDB.DEFAULT_AES_KEY));
            LOGGER.error("Key for device {} has been automatically generated", builder.getDeviceId());
        }

        return signAndSend(builder.getDeviceId(), builder, senseKeyStore);
    }

    @POST
    @Path("/sense")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] registerSense(final byte[] body) {
        final String senseIdFromHeader = this.request.getHeader(HelloHttpHeader.SENSE_ID);
        if(senseIdFromHeader != null){
            LOGGER.info("Sense Id from http header {}", senseIdFromHeader);
        }
        final MorpheusCommand.Builder builder = pair(body, senseKeyStore, PairAction.PAIR_MORPHEUS);
        if(senseIdFromHeader != null){
            return signAndSend(senseIdFromHeader, builder, senseKeyStore);
        }
        return signAndSend(builder.getDeviceId(), builder, senseKeyStore);
    }

    @POST
    @Path("/pill")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] registerPill(final byte[] body) {
        final MorpheusCommand.Builder builder = pair(body, senseKeyStore, PairAction.PAIR_PILL);
        final String token = builder.getAccountId();


        // WARNING: never return the account id, it will overflow buffer for old versions
        builder.clearAccountId();

        final String senseIdFromHeader = this.request.getHeader(HelloHttpHeader.SENSE_ID);
        if(senseIdFromHeader != null && !senseIdFromHeader.equals(KeyStoreDynamoDB.DEFAULT_FACTORY_DEVICE_ID)){
            LOGGER.info("Sense Id from http header {}", senseIdFromHeader);
            return signAndSend(senseIdFromHeader, builder, senseKeyStore);
        }

        // TODO: Remove this and get sense id from header after the firmware is fixed.
        final Optional<AccessToken> accessTokenOptional = this.tokenStore.getClientDetailsByToken(
                new ClientCredentials(new OAuthScope[]{OAuthScope.AUTH}, token),
                DateTime.now());

        if(!accessTokenOptional.isPresent()) {
            LOGGER.error("Did not find accessToken {}", token);
            return plainTextError(Response.Status.BAD_REQUEST, "");
        }

        final Long accountId = accessTokenOptional.get().accountId;
        final List<DeviceAccountPair> deviceAccountPairs = this.deviceDAO.getSensesForAccountId(accountId);
        if(deviceAccountPairs.size() == 0) {
            return plainTextError(Response.Status.BAD_REQUEST, "");
        }

        final String senseId = deviceAccountPairs.get(0).externalDeviceId;
        return signAndSend(senseId, builder, senseKeyStore);
    }
}
