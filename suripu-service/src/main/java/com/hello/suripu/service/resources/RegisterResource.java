package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.dropwizard.mikkusu.helpers.AdditionalMediaTypes;
import com.hello.suripu.api.ble.MorpheusBle;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.service.SignedMessage;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
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

    private final Boolean debug;

    private static enum PairAction{
        PAIR_MORPHEUS,
        PAIR_PILL
    }

    public RegisterResource(final DeviceDAO deviceDAO,
                            final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore,
                            final Boolean debug){
        this.deviceDAO = deviceDAO;
        this.tokenStore = tokenStore;
        this.debug = debug;
    }

    private byte[] pair(final byte[] encryptedRequest, final PairAction action) {

        final MorpheusBle.MorpheusCommand.Builder builder = MorpheusBle.MorpheusCommand.newBuilder()
                .setVersion(PROTOBUF_VERSION);
        // TODO: Fetch key from Datastore
        final byte[] keyBytes = "1234567891234567".getBytes();

        final SignedMessage signedMessage = SignedMessage.parse(encryptedRequest);
        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytes);

        if(error.isPresent()) {
            LOGGER.error(error.get().message);
            builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(MorpheusBle.ErrorType.INTERNAL_DATA_ERROR);
            return builder.build().toByteArray();
        }

        MorpheusBle.MorpheusCommand morpheusCommand;
        try {
            morpheusCommand = MorpheusBle.MorpheusCommand.parseFrom(signedMessage.body);
        } catch (IOException exception) {
            final String errorMessage = String.format("Failed parsing protobuf: %s", exception.getMessage());
            LOGGER.error(errorMessage);

            builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(MorpheusBle.ErrorType.INTERNAL_OPERATION_FAILED);
            return builder.build().toByteArray();
        }


        if (morpheusCommand.getType() != MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE) {
            builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(MorpheusBle.ErrorType.INTERNAL_DATA_ERROR);
            return builder.build().toByteArray();
        }

        final String deviceId = morpheusCommand.getDeviceId();
        final String token = morpheusCommand.getAccountId();

        final Optional<AccessToken> accessTokenOptional = this.tokenStore.getClientDetailsByToken(
                new ClientCredentials(new OAuthScope[]{OAuthScope.AUTH}, token),
                DateTime.now());

        if(!accessTokenOptional.isPresent()) {
            builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
            builder.setError(MorpheusBle.ErrorType.INTERNAL_OPERATION_FAILED);
            return builder.build().toByteArray();
        }

        final Long accountId = accessTokenOptional.get().accountId;

        try {
            switch (action){
                case PAIR_MORPHEUS:
                    this.deviceDAO.registerSense(accountId, deviceId);
                    builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE);
                    break;
                case PAIR_PILL:
                    this.deviceDAO.registerTracker(accountId, deviceId);
                    builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_PILL);
                    break;
            }

            builder.setAccountId(token);
            builder.setDeviceId(deviceId);

        } catch (UnableToExecuteStatementException sqlExp){
            final Matcher matcher = PG_UNIQ_PATTERN.matcher(sqlExp.getMessage());
            if (!matcher.find()) {
                LOGGER.error(sqlExp.getMessage());
                builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                builder.setError(MorpheusBle.ErrorType.INTERNAL_OPERATION_FAILED);
            }else {

                LOGGER.warn("Account {} tries to pair a paired device {} ",
                        accountId, deviceId);
                builder.setType(MorpheusBle.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR);
                builder.setError(MorpheusBle.ErrorType.DEVICE_ALREADY_PAIRED);
            }
        }

        return builder.build().toByteArray();
    }

    @POST
    @Path("/morpheus")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] registerMorpheus(final byte[] body) {
        return pair(body, PairAction.PAIR_MORPHEUS);

    }

    @POST
    @Path("/pill")
    @Consumes(AdditionalMediaTypes.APPLICATION_PROTOBUF)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Timed
    public byte[] registerPill(final byte[] body) {
        return pair(body, PairAction.PAIR_PILL);

    }
}
