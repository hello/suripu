package com.hello.suripu.core.roomstate;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.util.RoomConditionUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

public class CurrentRoomStateTest {

    @Test
    public void testDoesntThrowWithRegularConditions() {
        // temp = 22.61, humidity = 56.468544, particulates = 37.35597, light = 18933.0, sound = 0.0
         CurrentRoomState roomState = CurrentRoomState.fromTempHumidDustLightSound(
                22.61f, 56.468544f, 37.35597f, 18933.0f, 0.0f,
                 DateTime.now(), DateTime.now(), 10, "c");

        RoomConditionUtil.getRoomConditionV2LightOff(roomState, true);
        RoomConditionUtil.getRoomConditionV2LightOff(roomState, false);
    }

    @Test
    public void testDoesntThrowWithDataTooOld() {
        // temp = 22.61, humidity = 56.468544, particulates = 37.35597, light = 18933.0, sound = 0.0
        CurrentRoomState roomState = CurrentRoomState.fromRawData(
                0, 0, 0, 0, 0, 0,
                DateTime.now(DateTimeZone.UTC).minusMinutes(50).getMillis(),
                100,
                DateTime.now(DateTimeZone.UTC), 10, Optional.of(Calibration.createDefault("yo")));

        RoomConditionUtil.getGeneralRoomCondition(roomState);
        RoomConditionUtil.getRoomConditionV2LightOff(roomState, true);
        RoomConditionUtil.getRoomConditionV2LightOff(roomState, false);
    }
}
