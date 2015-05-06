package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.configuration.QueueName;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.ClientDetails;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.service.SignedMessage;
import com.hello.suripu.service.utils.PairAction;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by pangwu on 5/5/15.
 *
 * This class demonstrates how to test a resource class and gain fine grain control of the input,
 * so we can cover as much internal code path as possible.
 */
public class RegisterResourceIntegrationTest extends ResourceTest {

    private static final String SENSE_ID = "test sense";
    private static final String ACCESS_TOKEN = "test access token";
    private static final byte[] KEY = "1234567891234567".getBytes();
    private RegisterResource registerResource;


    private void stubGetClientDetailsByTokenThatReuturnsNoAccessToken(final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore){
        when(tokenStore.getClientDetailsByToken(any(ClientCredentials.class), any(DateTime.class))).thenReturn(Optional.<AccessToken>absent());
    }

    private void stubGetClientDetails(final OAuthTokenStore<AccessToken, ClientDetails, ClientCredentials> tokenStore,
                                      final Optional<AccessToken> returnValue){
        when(tokenStore.getClientDetailsByToken(any(ClientCredentials.class), any(DateTime.class))).thenReturn(returnValue);
    }

    private AccessToken getAccessToken(){
        return new AccessToken(UUID.randomUUID(), UUID.randomUUID(), 0L, DateTime.now(), 1L, 1L, new OAuthScope[]{ OAuthScope.AUTH });
    }

    private void stubGetSensePairingState(final RegisterResource registerResource, final RegisterResource.PairState returnValue){
        doReturn(returnValue).when(registerResource).getSensePairingState(SENSE_ID, 1L);
    }

    private byte[] generateInvalidEncryptedMessage(){
        final byte[] raw = new byte[10];
        return raw;
    }

    private byte[] generateValidProtobufWithSignature(final byte[] key){
        final SenseCommandProtos.MorpheusCommand command = SenseCommandProtos.MorpheusCommand.newBuilder()
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setAccountId(ACCESS_TOKEN)
                .setVersion(1)
                .setDeviceId(SENSE_ID)
                .build();

        final byte[] body  = command.toByteArray();
        final Optional<byte[]> signedOptional = SignedMessage.sign(body, key);
        assertThat(signedOptional.isPresent(), is(true));
        final byte[] signed = signedOptional.get();
        final byte[] iv = Arrays.copyOfRange(signed, 0, 16);
        final byte[] sig = Arrays.copyOfRange(signed, 16, 16 + 32);
        final byte[] message = new byte[signed.length];
        copyTo(message, body, 0, body.length);
        copyTo(message, iv, body.length, body.length + iv.length);
        copyTo(message, sig, body.length + iv.length, message.length);
        return message;

    }

    private byte[] generateValidProtobufWithInvalidSignature(final byte[] key){
        final SenseCommandProtos.MorpheusCommand command = SenseCommandProtos.MorpheusCommand.newBuilder()
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setAccountId(ACCESS_TOKEN)
                .setVersion(1)
                .setDeviceId(SENSE_ID)
                .build();

        final byte[] body  = command.toByteArray();
        final Optional<byte[]> signedOptional = SignedMessage.sign(body, key);
        assertThat(signedOptional.isPresent(), is(true));
        final byte[] signed = signedOptional.get();
        final byte[] iv = Arrays.copyOfRange(signed, 0, 16);
        final byte[] message = new byte[signed.length];
        copyTo(message, body, 0, body.length);
        copyTo(message, iv, body.length, body.length + iv.length);
        copyTo(message, new byte[32], body.length + iv.length, message.length);
        return message;

    }

    private byte[] generateInvalidProtobuf(final byte[] key){
        final SenseCommandProtos.MorpheusCommand command = SenseCommandProtos.MorpheusCommand.newBuilder()
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setAccountId(ACCESS_TOKEN)
                .setVersion(1)
                .setDeviceId(SENSE_ID)
                .build();

        final byte[] body  = command.toByteArray();
        final Optional<byte[]> signedOptional = SignedMessage.sign(body, key);
        assertThat(signedOptional.isPresent(), is(true));
        return signedOptional.get();

    }

    private void copyTo(final byte[] dest, final byte[] src, final int start, final int end){
        for(int i = start; i < end; i++){
            dest[i] = src[i-start];
        }

    }

    @Before
    public void setUp(){
        super.setUp();

        BaseReourceTestHelper.kinesisLoggerFactoryStubGet(this.kinesisLoggerFactory, QueueName.LOGS, this.dataLogger);
        BaseReourceTestHelper.kinesisLoggerFactoryStubGet(this.kinesisLoggerFactory, QueueName.REGISTRATIONS, this.dataLogger);
        BaseReourceTestHelper.stubInject(this.objectGraphRoot, RegisterResource.class);

        final RegisterResource registerResource = new RegisterResource(deviceDAO,
                oAuthTokenStore,
                kinesisLoggerFactory,
                keyStore,
                mergedUserInfoDynamoDB,
                groupFlipper,
                objectGraphRoot,
                true);
        registerResource.request = httpServletRequest;
        this.registerResource = spy(registerResource);

        BaseReourceTestHelper.stubGetHeader(this.registerResource.request, "X-Forwarded-For", "127.0.0.1");
    }

    @Test(expected = WebApplicationException.class)
    public void testPairingCannotDecryptMessage(){
        this.registerResource.pair(SENSE_ID,
                generateInvalidEncryptedMessage(),
                this.keyStore,
                PairAction.PAIR_MORPHEUS);
        verify(this.registerResource).throwPlainTextError(Response.Status.BAD_REQUEST, "");

    }

    @Test(expected = WebApplicationException.class)
    public void testPairingCannotParseProtobuf(){
        this.registerResource.pair(SENSE_ID,
                generateInvalidProtobuf(KEY),
                this.keyStore,
                PairAction.PAIR_MORPHEUS);
        verify(this.registerResource).throwPlainTextError(Response.Status.BAD_REQUEST, "");
    }


    /*
    * simulate scenario that no account can be found with the token provided
     */
    @Test
    public void testPairingCannotFindToken(){
        // simulate scenario that no account can be found with the token provided
        stubGetClientDetailsByTokenThatReuturnsNoAccessToken(this.oAuthTokenStore);

        final SenseCommandProtos.MorpheusCommand.Builder builder = this.registerResource.pair(SENSE_ID,
                generateValidProtobufWithSignature(KEY),
                this.keyStore,
                PairAction.PAIR_MORPHEUS);
        assertThat(builder.getType(), is(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR));
        assertThat(builder.getError(), is(SenseCommandProtos.ErrorType.INTERNAL_OPERATION_FAILED));
    }


    @Test(expected = WebApplicationException.class)
    public void testPairingInvalidSignature(){
        stubGetClientDetails(this.oAuthTokenStore, Optional.of(getAccessToken()));
        BaseReourceTestHelper.stubKeyFromKeyStore(this.keyStore, SENSE_ID, Optional.of(KEY));

        this.registerResource.pair(SENSE_ID,
                generateValidProtobufWithInvalidSignature(KEY),
                this.keyStore,
                PairAction.PAIR_MORPHEUS);
        verify(this.registerResource).throwPlainTextError(Response.Status.UNAUTHORIZED, "invalid signature");

    }

    @Test(expected = WebApplicationException.class)
    public void testPairingNoKey(){
        stubGetClientDetails(this.oAuthTokenStore, Optional.of(getAccessToken()));
        BaseReourceTestHelper.stubKeyFromKeyStore(this.keyStore, SENSE_ID, Optional.<byte[]>absent());

        this.registerResource.pair(SENSE_ID,
                generateValidProtobufWithInvalidSignature(KEY),
                this.keyStore,
                PairAction.PAIR_MORPHEUS);
        verify(this.registerResource).throwPlainTextError(Response.Status.UNAUTHORIZED, "no key");
        verify(this.deviceDAO, times(0)).registerSense(1L, SENSE_ID);
    }

    @Test
    public void testPairSense(){
        stubGetClientDetails(this.oAuthTokenStore, Optional.of(getAccessToken()));
        BaseReourceTestHelper.stubKeyFromKeyStore(this.keyStore, SENSE_ID, Optional.of(KEY));
        stubGetSensePairingState(this.registerResource, RegisterResource.PairState.NOT_PAIRED);

        final SenseCommandProtos.MorpheusCommand command = this.registerResource.pair(SENSE_ID,
                generateValidProtobufWithSignature(KEY),
                this.keyStore,
                PairAction.PAIR_MORPHEUS).build();
        verify(this.deviceDAO, times(1)).registerSense(1L, SENSE_ID);
        assertThat(command.getType(), is(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE));

    }

    @Test
    public void testPairAlreadyPairedSense(){
        stubGetClientDetails(this.oAuthTokenStore, Optional.of(getAccessToken()));
        BaseReourceTestHelper.stubKeyFromKeyStore(this.keyStore, SENSE_ID, Optional.of(KEY));
        stubGetSensePairingState(this.registerResource, RegisterResource.PairState.PAIRED_WITH_CURRENT_ACCOUNT);

        final SenseCommandProtos.MorpheusCommand command = this.registerResource.pair(SENSE_ID,
                generateValidProtobufWithSignature(KEY),
                this.keyStore,
                PairAction.PAIR_MORPHEUS).build();
        verify(this.deviceDAO, times(0)).registerSense(1L, SENSE_ID);
        assertThat(command.getType(), is(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE));

    }

    @Test
    public void testPairAlreadyPairedSenseToDifferentAccount(){
        stubGetClientDetails(this.oAuthTokenStore, Optional.of(getAccessToken()));
        BaseReourceTestHelper.stubKeyFromKeyStore(this.keyStore, SENSE_ID, Optional.of(KEY));
        stubGetSensePairingState(this.registerResource, RegisterResource.PairState.PAIRING_VIOLATION);

        final SenseCommandProtos.MorpheusCommand command = this.registerResource.pair(SENSE_ID,
                generateValidProtobufWithSignature(KEY),
                this.keyStore,
                PairAction.PAIR_MORPHEUS).build();
        verify(this.deviceDAO, times(0)).registerSense(1L, SENSE_ID);
        assertThat(command.getType(), is(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_ERROR));
        assertThat(command.getError(), is(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED));

    }
}
