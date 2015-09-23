package com.hello.suripu.service.registration;

import com.google.common.base.Optional;
import com.hello.suripu.api.ble.SenseCommandProtos;
import com.hello.suripu.core.db.DeviceDAO;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SenseRegistrationTest {
    private final DeviceDAO deviceDAO = mock(DeviceDAO.class);
    private final CommonDevice commonDevice = mock(CommonDevice.class);

    private final String senseId = "SenseId";
    private final String otherSenseId = "OtherSenseId";
    private final Long accountId = 123L;
    private final Long otherAccountId = 123L;
    private final Long internalId = 1L;
    private final SenseRegistration senseRegistration = SenseRegistration.create(deviceDAO, commonDevice);

    private final SenseCommandProtos.MorpheusCommand command = SenseCommandProtos.MorpheusCommand
            .newBuilder()
            .setVersion(SenseRegistration.PROTOBUF_VERSION)
            .setType(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE)
            .build();

    @Test
    public void testPairToDeviceAlreadyPaired() {
        final Optional<SenseCommandProtos.MorpheusCommand> commandOptional = getCommandForPairingStatus(PairingStatus.failed(PairingState.PAIRING_VIOLATION, ""));

        verify(deviceDAO, times(0)).registerSense(anyLong(), anyString());
        assertThat(commandOptional.isPresent(), is(true));
        assertThat(commandOptional.get().getError(), equalTo(SenseCommandProtos.ErrorType.DEVICE_ALREADY_PAIRED));
    }

    @Test
    public void testPairToDeviceNeverPaired() {
        when(deviceDAO.registerSense(anyLong(), anyString())).thenReturn(1L);
        final Optional<SenseCommandProtos.MorpheusCommand> commandOptional = getCommandForPairingStatus(PairingStatus.ok(PairingState.NOT_PAIRED, ""));

        verify(deviceDAO, times(1)).registerSense(anyLong(), anyString());
        assertThat(commandOptional.isPresent(), is(true));
        assertThat(commandOptional.get().hasError(), is(false));
        assertThat(commandOptional.get().getType(), equalTo(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE));
    }

    @Test
    public void testPairToSameDevice() {
        when(deviceDAO.registerSense(anyLong(), anyString())).thenReturn(1L);

        final Optional<SenseCommandProtos.MorpheusCommand> commandOptional = getCommandForPairingStatus(PairingStatus.failed(PairingState.PAIRED_WITH_CURRENT_ACCOUNT, ""));

        verify(deviceDAO, times(0)).registerSense(anyLong(), anyString());
        assertThat(commandOptional.isPresent(), is(true));
        assertThat(commandOptional.get().hasError(), is(false));
        assertThat(commandOptional.get().getType(), equalTo(SenseCommandProtos.MorpheusCommand.CommandType.MORPHEUS_COMMAND_PAIR_SENSE));
    }


    @Test
    public void testVersionIsPreserved() {
        final Optional<SenseCommandProtos.MorpheusCommand> commandOptional = getCommandForPairingStatus(PairingStatus.failed(PairingState.PAIRING_VIOLATION, ""));

        assertThat(commandOptional.isPresent(), is(true));
        assertThat(commandOptional.get().hasVersion(), is(true));
        assertThat(commandOptional.get().getVersion(), equalTo(SenseRegistration.PROTOBUF_VERSION));
    }


    private Optional<SenseCommandProtos.MorpheusCommand> getCommandForPairingStatus(final PairingStatus pairingStatus) {
        return senseRegistration.pair(command, senseId, accountId, pairingStatus);
    }
}
