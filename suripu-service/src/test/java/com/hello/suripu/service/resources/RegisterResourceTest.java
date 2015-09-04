package com.hello.suripu.service.resources;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.UninitializedMessageException;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.DeviceAccountPair;
import com.hello.suripu.service.SignedMessage;
import com.hello.suripu.service.registration.CommonDevice;
import com.hello.suripu.service.registration.PillRegistration;
import com.hello.suripu.service.registration.SenseRegistration;
import com.hello.suripu.service.registration.SenseSigner;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegisterResourceTest {


    final DeviceDAO deviceDAO = mock(DeviceDAO.class);
    final CommonDevice commonDevice = mock(CommonDevice.class);
    final SenseRegistration senseRegistration = SenseRegistration.create(deviceDAO, commonDevice);

    private final PillRegistration pillRegistration = mock(PillRegistration.class);


    private final String goodToken = "goodToken";
    private final String badToken = "badToken";
    private final byte[] goodKey = "1234567981234567".getBytes();
    private final byte[] badKey  = "9876543219876543".getBytes();
    private final String senseId = "senseId";
    private final Long accountId = 12L;
    private final ImmutableList<DeviceAccountPair> accountPairs = ImmutableList.copyOf(Lists.newArrayList(new DeviceAccountPair(accountId, 99L, senseId, DateTime.now())));

    private static class FakeRegisterResource extends RegisterResource {

        private final String senseId;

        private FakeRegisterResource(final SenseRegistration senseRegistration, final PillRegistration pillRegistration, final Boolean debug, final String senseId) {
            super(senseRegistration, pillRegistration, debug);
            this.senseId = senseId;
        }

        @Override
        public String getSenseIdFromHeader() {
            return senseId;
        }

        public static FakeRegisterResource create(final SenseRegistration senseRegistration, final PillRegistration pillRegistration, final String senseId) {
            return new FakeRegisterResource(senseRegistration, pillRegistration, false, senseId);
        }

        public static FakeRegisterResource createWithoutSenseIdInHeader(final SenseRegistration senseRegistration, final PillRegistration pillRegistration) {
            return new FakeRegisterResource(senseRegistration, pillRegistration, false, null);
        }
    }


    @Before
    public void setUp() {

    }

    @Test(expected = UninitializedMessageException.class)
    public void missingVersion() throws InvalidProtocolBufferException {
        SenseCommandProtos.MorpheusCommand
                .newBuilder()
                .build();
    }

    @Test
    public void registerSenseSuccessfully() throws InvalidProtocolBufferException {


        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(accountPairs);
        when(commonDevice.validSignature(any(SignedMessage.class), anyString())).thenReturn(true);
        when(commonDevice.getAccountIdFromTokenString(goodToken, senseId)).thenReturn(Optional.of(accountId));

        final SenseCommandProtos.MorpheusCommand command = commandWithTokenAndSenseId(goodToken, senseId);

        when(commonDevice.signAndSend(anyString(), any(SenseCommandProtos.MorpheusCommand.class))).thenReturn(SignedMessage.signUnchecked(command.toByteArray(), goodKey));

        final byte[] signedBody = SenseSigner.sign(command.toByteArray(), goodKey);

        final FakeRegisterResource fakeRegisterResource = FakeRegisterResource.create(senseRegistration, pillRegistration, senseId);
        final byte[] signedResponse = fakeRegisterResource.registerSense(signedBody);
        final SenseCommandProtos.MorpheusCommand responseCommand = parse(signedResponse);
        assertThat(command.getType(), equalTo(responseCommand.getType()));
        assertThat(command.hasError(), is(false));
    }


    @Test(expected = WebApplicationException.class)
    public void registerSenseInvalidProtobuf(){
        final FakeRegisterResource fakeRegisterResource = FakeRegisterResource.create(senseRegistration, pillRegistration, senseId);
        fakeRegisterResource.registerSense("blah".getBytes());
    }

    @Test(expected = WebApplicationException.class)
    public void registerSenseInvalidKey(){

        when(commonDevice.validSignature(any(SignedMessage.class), anyString())).thenReturn(false);
        when(commonDevice.getAccountIdFromTokenString(anyString(), anyString())).thenReturn(Optional.of(accountId));

        final FakeRegisterResource fakeRegisterResource = FakeRegisterResource.create(senseRegistration, pillRegistration, senseId);
        final SenseCommandProtos.MorpheusCommand command = commandWithTokenAndSenseId(goodToken, senseId);

        final byte[] signedBody = SenseSigner.sign(command.toByteArray(), badKey);
        fakeRegisterResource.registerSense(signedBody);
    }


    @Test
    public void registerSenseMissingSenseIdInHeader() throws InvalidProtocolBufferException{

        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(accountPairs);
        when(commonDevice.validSignature(any(SignedMessage.class), anyString())).thenReturn(true);
        when(commonDevice.getAccountIdFromTokenString(anyString(), anyString())).thenReturn(Optional.of(accountId));

        final SenseCommandProtos.MorpheusCommand command = commandWithTokenAndSenseId(goodToken, senseId);

        when(commonDevice.signAndSend(anyString(), any(SenseCommandProtos.MorpheusCommand.class))).thenReturn(SignedMessage.signUnchecked(command.toByteArray(), goodKey));

        final byte[] signedBody = SenseSigner.sign(command.toByteArray(), goodKey);

        final FakeRegisterResource fakeRegisterResource = FakeRegisterResource.createWithoutSenseIdInHeader(senseRegistration, pillRegistration); // no sense id in header
        final byte[] signedResponse = fakeRegisterResource.registerSense(signedBody);
        final SenseCommandProtos.MorpheusCommand responseCommand = parse(signedResponse);
        assertThat(command.getType(), equalTo(responseCommand.getType()));
        assertThat(command.hasError(), is(false));
    }


    @Test(expected = WebApplicationException.class)
    public void registerBadToken() throws InvalidProtocolBufferException{

        when(commonDevice.getAccountIdFromTokenString(badToken, senseId)).thenReturn(Optional.<Long>absent());

        final SenseCommandProtos.MorpheusCommand command = commandWithTokenAndSenseId(badToken, senseId);
        final byte[] signedBody = SenseSigner.sign(command.toByteArray(), goodKey);

        final FakeRegisterResource fakeRegisterResource = FakeRegisterResource.create(senseRegistration, pillRegistration, senseId); // no sense id in header
        fakeRegisterResource.registerSense(signedBody);
    }

    protected SenseCommandProtos.MorpheusCommand parse(byte[] signedResponse) throws InvalidProtocolBufferException {
        final SignedMessage signedMessage = SenseSigner.reverseParse(signedResponse);
        return SenseCommandProtos.MorpheusCommand.parseFrom(signedMessage.body);
    }

    protected SenseCommandProtos.MorpheusCommand commandWithTokenAndSenseId(final String token, final String deviceId) {
        final SenseCommandProtos.MorpheusCommand command = SenseCommandProtos.MorpheusCommand
                .newBuilder()
                .setAccountId(token)
                .setDeviceId(deviceId)
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(SenseRegistration.PROTOBUF_VERSION)
                .build();
        return command;
    }

}
