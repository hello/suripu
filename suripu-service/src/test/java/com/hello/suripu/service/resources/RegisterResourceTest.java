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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegisterResourceTest {

    private final SenseRegistration senseRegistration = mock(SenseRegistration.class);
    private final PillRegistration pillRegistration = mock(PillRegistration.class);


    private final String goodToken = "goodToken";
    private final String badToken = "badToken";
    private final byte[] goodKey = "1234567981234567".getBytes();
    private final byte[] badKey  = "9876543219876543".getBytes();
    private final String senseId = "senseId";

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

        public static FakeRegisterResource createWithIpAddress(SenseRegistration senseRegistration, PillRegistration pillRegistration, String senseId) {
            return new FakeRegisterResource(senseRegistration, pillRegistration, false, senseId);
        }
    }


    @Before
    public void setUp() {

    }

    @Test(expected = UninitializedMessageException.class)
    public void missingVersion() throws InvalidProtocolBufferException {
        final SenseCommandProtos.MorpheusCommand command = SenseCommandProtos.MorpheusCommand
                .newBuilder()
                .build();
    }


    @Test
    public void whatever() throws InvalidProtocolBufferException {

        final DeviceDAO deviceDAO = mock(DeviceDAO.class);
        final CommonDevice commonDevice = mock(CommonDevice.class);

        final SenseRegistration senseRegistration = SenseRegistration.create(deviceDAO, commonDevice);
        final Long accountId = 12L;
        final ImmutableList<DeviceAccountPair> pairs = ImmutableList.copyOf(Lists.newArrayList(new DeviceAccountPair(accountId, 99L, senseId, DateTime.now())));
        when(deviceDAO.getSensesForAccountId(accountId)).thenReturn(pairs);
        when(commonDevice.validSignature(any(SignedMessage.class), anyString())).thenReturn(true);
        when(commonDevice.getAccountIdFromTokenString(anyString(), anyString())).thenReturn(Optional.of(accountId));




        final FakeRegisterResource fakeRegisterResource = FakeRegisterResource.createWithIpAddress(senseRegistration, pillRegistration, senseId);
        final SenseCommandProtos.MorpheusCommand command = SenseCommandProtos.MorpheusCommand
                .newBuilder()
                .setAccountId(goodToken)
                .setDeviceId(senseId)
                .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
                .setVersion(SenseRegistration.PROTOBUF_VERSION)
                .build();

        when(commonDevice.signAndSend(anyString(), any(SenseCommandProtos.MorpheusCommand.class))).thenReturn(SignedMessage.signUnchecked(command.toByteArray(), goodKey));

        final byte[] signedBody = SenseSigner.sign(command.toByteArray(), goodKey);
        final byte[] signedResponse = fakeRegisterResource.registerSense(signedBody);
        final SenseCommandProtos.MorpheusCommand responseCommand = parse(signedResponse, goodKey);
    }



    protected SenseCommandProtos.MorpheusCommand parse(byte[] signedResponse, byte[] key) throws InvalidProtocolBufferException {
        final SignedMessage signedMessage = SignedMessage.parse(signedResponse);
        final Optional<SignedMessage.Error> errorOptional = signedMessage.validateWithKey(key);
        if(errorOptional.isPresent()) {
            throw new IllegalArgumentException(errorOptional.get().message);
        }

        return SenseCommandProtos.MorpheusCommand.parseFrom(signedMessage.body);
    }

}
