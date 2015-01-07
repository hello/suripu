package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.api.ble.SenseCommandProtos.MorpheusCommand;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.logging.DataLogger;
import com.hello.suripu.core.logging.KinesisLoggerFactory;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.util.HelloHttpHeader;
import com.hello.suripu.service.SignedMessage;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pangwu on 10/10/14.
 */
@Path("/register")
public class RegisterResource {
    private static final Pattern PG_UNIQ_PATTERN = Pattern.compile("ERROR: duplicate key value violates unique constraint \"(\\w+)\"");
    private static int PROTOBUF_VERSION = 0;
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterResource.class);
    private final DeviceDAO deviceDAO;
    final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore;
    private final KinesisLoggerFactory kinesisLoggerFactory;
    private final KeyStore senseKeyStore;

    private final Boolean debug;

    private enum PairState{
        NOT_PAIRED,
        PAIRED_WITH_CURRENT_ACCOUNT,
        PAIRING_VIOLATION;
    }

    @Context
    HttpServletRequest request;

    private static enum PairAction{
        PAIR_MORPHEUS,
        PAIR_PILL
    }

    public RegisterResource(final DeviceDAO deviceDAO,
                            final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore,
                            final KinesisLoggerFactory kinesisLoggerFactory,
                            final KeyStore senseKeyStore,
                            final Boolean debug){
        this.deviceDAO = deviceDAO;
        this.tokenStore = tokenStore;
        this.debug = debug;
        this.kinesisLoggerFactory = kinesisLoggerFactory;
        this.senseKeyStore = senseKeyStore;
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

    public static PairState getSensePairingState(final DeviceDAO deviceDAO, final String senseId, final long accountId){
        final List<DeviceAccountPair> pairedSense = deviceDAO.getSensesForAccountId(accountId);
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

    public static PairState getPillPairingState(final DeviceDAO deviceDAO, final String pillId, final long accountId){
        final List<DeviceAccountPair> pairedPills = deviceDAO.getPillsForAccountId(accountId);
        final List<DeviceAccountPair> pairedAccounts = deviceDAO.getLinkedAccountFromPillId(pillId);
        if(pairedPills.size() > 1 || pairedAccounts.size() > 1){  // This account already paired with multiple pills
            return PairState.PAIRING_VIOLATION;
        }

        if(pairedAccounts.size() == 0 && pairedPills.size() == 0){
            return PairState.NOT_PAIRED;
        }

        if(pairedAccounts.size() == 1 && pairedPills.size() == 1 && pairedPills.get(0).externalDeviceId.equals(pillId)){
            // might be a firmware retry
            return PairState.PAIRED_WITH_CURRENT_ACCOUNT;
        }

        // else:
        if(pairedAccounts.size() == 1 && pairedPills.size() == 0){
            // pill already paired with an account, but this account is new, stolen pill?
            LOGGER.error("Pill {} might got stolen, account {} is a theft!", pillId, accountId);
        }
        if(pairedPills.size() == 1 && pairedAccounts.size() == 0){
            // account already paired with a pill, only one pill is allowed
            LOGGER.error("Account {} already paired with pill {}", accountId, pairedPills.get(0).externalDeviceId);
        }
        return PairState.PAIRING_VIOLATION;

    }


    private MorpheusCommand.Builder pair(final byte[] encryptedRequest, final KeyStore keyStore, final PairAction action) {

        final MorpheusCommand.Builder builder = MorpheusCommand.newBuilder()
                .setVersion(PROTOBUF_VERSION);

        final SignedMessage signedMessage = SignedMessage.parse(encryptedRequest);
        MorpheusCommand morpheusCommand;
        try {
            morpheusCommand = MorpheusCommand.parseFrom(signedMessage.body);

        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);
            // We can't return a proper error because we can't decode the protobuf
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        final String deviceId = morpheusCommand.getDeviceId();
        builder.setDeviceId(deviceId);

        String senseId = deviceId;

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

        switch (action) {
            case PAIR_MORPHEUS:
                senseId = deviceId;
                break;
            case PAIR_PILL:
                final List<DeviceAccountPair> deviceAccountPairs = this.deviceDAO.getSensesForAccountId(accountId);
                if(deviceAccountPairs.size() == 0){
                    LOGGER.error("No sense paired with account {} when pill {} tries to register", accountId, deviceId);
                    builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                    builder.setError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR);
                    return builder;
                }
                senseId = deviceAccountPairs.get(0).externalDeviceId;
        }

        final Optional<byte[]> keyBytesOptional = keyStore.get(senseId);
        if(!keyBytesOptional.isPresent()) {
            LOGGER.error("Missing AES key for device = {}", senseId);
            builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR);
            return builder;
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytesOptional.get());

        if(error.isPresent()) {
            LOGGER.error("Fail to validate signature {}", error.get().message);
            builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(SenseCommandProtos.ErrorType.INTERNAL_DATA_ERROR);
            return builder;
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
                    final PairState pairState = getSensePairingState(this.deviceDAO, senseId, accountId);
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
                    final PairState pairState = getPillPairingState(this.deviceDAO, deviceId, accountId);
                    if (pairState == PairState.NOT_PAIRED) {
                        this.deviceDAO.registerTracker(accountId, deviceId);
                    }

                    if (pairState == PairState.NOT_PAIRED || pairState == PairState.PAIRED_WITH_CURRENT_ACCOUNT) {
                        builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_PILL);
                    } else {
                        builder.setType(MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                        builder.setError(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED);
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

        final String ip = request.getHeader("X-Forwarded-For");
        LoggingProtos.Registration registration = LoggingProtos.Registration.newBuilder()
                .setAccountId(accountId)
                .setDeviceId(deviceId)
                .setTimestamp(DateTime.now().getMillis())
                .setIpAddress(ip)
                .build();

        final DataLogger dataLogger = kinesisLoggerFactory.get(QueueName.REGISTRATIONS);
        dataLogger.put(accountId.toString(), registration.toByteArray());

        return builder;
    }

    private Response signAndSend(final String senseId, final MorpheusCommand.Builder morpheusCommandBuilder, final KeyStore keyStore) {
        final Optional<byte[]> keyBytesOptional = keyStore.get(senseId);
        if(!keyBytesOptional.isPresent()) {
            LOGGER.error("Missing AES key for deviceId = {}", senseId);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        final Optional<byte[]> signedResponse = SignedMessage.sign(morpheusCommandBuilder.build().toByteArray(), keyBytesOptional.get());
        if(!signedResponse.isPresent()) {
            LOGGER.error("Failed signing message for deviceId = {}", senseId);
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.ok().entity(signedResponse.get()).build();
    }

    @POST
    @Path("/morpheus")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public Response registerMorpheus(final byte[] body) {
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
    public Response registerPill(final byte[] body) {
        final MorpheusCommand.Builder builder = pair(body, senseKeyStore, PairAction.PAIR_PILL);
        final String senseIdFromHeader = this.request.getHeader(HelloHttpHeader.SENSE_ID);
        if(senseIdFromHeader != null){
            LOGGER.info("Sense Id from http header {}", senseIdFromHeader);
            return signAndSend(senseIdFromHeader, builder, senseKeyStore);
        }

        // TODO: Remove this and get sense id from header after the firmware is fixed.
        final Optional<AccessToken> accessTokenOptional = this.tokenStore.getClientDetailsByToken(
                new ClientCredentials(new OAuthScope[]{OAuthScope.AUTH}, builder.getAccountId()),
                DateTime.now());

        if(!accessTokenOptional.isPresent()) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final Long accountId = accessTokenOptional.get().accountId;
        final List<DeviceAccountPair> deviceAccountPairs = this.deviceDAO.getSensesForAccountId(accountId);
        if(deviceAccountPairs.size() == 0) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        final String senseId = deviceAccountPairs.get(0).externalDeviceId;
        return signAndSend(senseId, builder, senseKeyStore);
    }
}
