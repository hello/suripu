package com.hello.suripu.service.registration;

import com.google.common.base.Optional;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.core.resources.BaseResource;
import com.hello.suripu.service.SignedMessage;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

public class CommonDevice {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonDevice.class);

    private final OAuthTokenStore tokenStore;
    private final KeyStore keyStore;

    public CommonDevice(final OAuthTokenStore tokenStore, final KeyStore keyStore) {
        this.tokenStore = tokenStore;
        this.keyStore = keyStore;
    }

    public Optional<Long> getAccountIdFromTokenString(final String token, @NotNull final String senseId, final DateTime now) {
        final Optional<AccessToken> accessTokenOptional = tokenStore.getClientDetailsByToken(
                new ClientCredentials(new OAuthScope[]{OAuthScope.AUTH}, token), now);

        if(!accessTokenOptional.isPresent()) {
            final String logMessage = String.format("Token not found %s for device Id %s", token, senseId);
            LOGGER.error(logMessage);
            return Optional.absent();
        }

        return Optional.of(accessTokenOptional.get().accountId);
    }

    public Optional<Long> getAccountIdFromTokenString(final String token, @NotNull final String senseId) {
        return getAccountIdFromTokenString(token, senseId, DateTime.now(DateTimeZone.UTC));
    }

    /**
     *
     * @param signedMessage
     * @param senseId
     * @return
     */
    public Boolean validSignature(final SignedMessage signedMessage, @NotNull final String senseId) {
        final Optional<byte[]> keyBytesOptional = keyStore.get(senseId);
        if(!keyBytesOptional.isPresent()) {
            final String errorMessage = String.format("Missing AES key for device = %s", senseId);
            LOGGER.error(errorMessage);
            return false;
        }

        final Optional<SignedMessage.Error> error = signedMessage.validateWithKey(keyBytesOptional.get());

        if(error.isPresent()) {
            final String errorMessage = String.format("Fail to validate signature %s", error.get().message);
            LOGGER.error(errorMessage);
            return false;
        }

        return true;
    }


    public byte[] signAndSend(final String senseId, final SenseCommandProtos.MorpheusCommand command) throws FailedToSignException {
        final Optional<byte[]> keyBytesOptional = keyStore.get(senseId);
        if(!keyBytesOptional.isPresent()) {
            LOGGER.error("Missing AES key for deviceId = {}", senseId);
            BaseResource.throwPlainTextError(Response.Status.INTERNAL_SERVER_ERROR, "");
        }
        LOGGER.trace("Key used to sign device {} : {}", senseId, Hex.encodeHexString(keyBytesOptional.get()));

        final SenseCommandProtos.MorpheusCommand cleanCommand = SenseCommandProtos.MorpheusCommand.newBuilder(command)
                .clearAccountId()
                .build();

        final Optional<byte[]> signedResponse = SignedMessage.sign(cleanCommand.toByteArray(), keyBytesOptional.get());
        if(!signedResponse.isPresent()) {
            LOGGER.error("Failed signing message for deviceId = {}", senseId);
            BaseResource.throwPlainTextError(Response.Status.INTERNAL_SERVER_ERROR, "");
        }

        return signedResponse.get();
    }
}
