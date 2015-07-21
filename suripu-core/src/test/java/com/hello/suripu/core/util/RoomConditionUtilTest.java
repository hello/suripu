package com.hello.suripu.core.util;

import com.hello.suripu.api.input.DataInputProtos.periodic_data;
import com.hello.suripu.api.output.OutputProtos.SyncResponse.RoomConditions;
import com.hello.suripu.core.models.CurrentRoomState;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jnorgan on 7/21/15.
 */
public class RoomConditionUtilTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomConditionUtilTest.class);

    @Test
    public void testRoomConditionResponse () {

        final Integer unixTime = (int) (DateTime.now(DateTimeZone.UTC).getMillis() / 1000L);
        final periodic_data.Builder dataBuilder = periodic_data.newBuilder()
                .setTemperature(2389) //(val - 389) / 100; >26=ALERT (2989); >23=WARNING (2689)
                .setHumidity(3092)// 20=ALERT; 30=WARNING
                .setDustMax(2000) // 301=ALERT; 51=WARNING
                .setLight(3) //8=ALERT; 3=WARNING
                .setAudioPeakBackgroundEnergyDb(0) //rawBackground; Not Taken Into Account
                .setAudioPeakDisturbanceEnergyDb(200) //rawPeak---> Math.max(peakDB/1024 - 40, 0) + 25; 90=ALERT; 40=WARNING
                .setUnixTime(unixTime)
                .setFirmwareVersion(521360154);

        final periodic_data data = dataBuilder.build();
        final Long timestampMillis = data.getUnixTime() * 1000L;
        final DateTime roundedDateTime = new DateTime(timestampMillis, DateTimeZone.UTC).withSecondOfMinute(0);

        final CurrentRoomState currentRoomState = CurrentRoomState.fromRawData(
                data.getTemperature(),
                data.getHumidity(),
                data.getDustMax(),
                data.getLight(),
                data.getAudioPeakBackgroundEnergyDb(),
                data.getAudioPeakDisturbanceEnergyDb(),
                roundedDateTime.getMillis(),
                data.getFirmwareVersion(),
                DateTime.now(),
                2);

        final RoomConditions newConditions = RoomConditions.valueOf(
                RoomConditionUtil.getGeneralRoomConditionV2(currentRoomState).ordinal());
        final RoomConditions oldConditions = RoomConditions.valueOf(
                RoomConditionUtil.getGeneralRoomCondition(currentRoomState).ordinal());

        LOGGER.debug("Light: {}, Audio: {}", currentRoomState.light.value, currentRoomState.sound.value);
        LOGGER.debug("New Conditions: {}", newConditions);
        LOGGER.debug("Old Conditions: {}", oldConditions);
        assertThat((newConditions == RoomConditions.WARNING), is(true));
        assertThat((oldConditions == RoomConditions.IDEAL), is(true));
    }

}
