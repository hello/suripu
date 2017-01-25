package com.hello.suripu.core.processors.insights;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.DeviceData;
import com.hello.suripu.core.models.Insights.InsightCard;
import com.hello.suripu.core.models.Insights.Message.TemperatureMsgEN;
import com.hello.suripu.core.preferences.TemperatureUnit;
import com.hello.suripu.core.util.DataUtils;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by jyfan on 8/10/16.
 */
public class TemperatureHumidityInsightsTest {

    private final Long FAKE_ACCOUNT_ID = 9999L;
    private final Long FAKE_DEVICE_ID = 9998L;
    private final String FAKE_EXTERNAL_ID = "fakeit";
    private final TemperatureUnit UNIT_CELSIUS = TemperatureUnit.CELSIUS;
    private final TemperatureUnit UNIT_FAHRENH = TemperatureUnit.FAHRENHEIT;
    private final int TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS = 389; //Private constant in DatUtils

    @Test
    public void test_tempPerfect() {

        final int min_temp_raw = (TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS * (int) DataUtils.FLOAT_2_INT_MULTIPLIER) + TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS; //undo temp calibration
        final int max_temp_raw = (TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS * (int) DataUtils.FLOAT_2_INT_MULTIPLIER) + TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS;
        final int middle_temp_raw = (min_temp_raw + max_temp_raw) / 2;

        final List<DeviceData> deviceDataList = Lists.newArrayList(
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, min_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, middle_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, max_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8));

        final Optional<InsightCard> insightCardOptional = TemperatureHumidity.processData(FAKE_ACCOUNT_ID, deviceDataList, Optional.of(Device.Color.WHITE), Optional.absent(), UNIT_CELSIUS);
        assertThat(insightCardOptional.isPresent(), is(Boolean.TRUE));

        final int medTempCelsius = (int) DataUtils.calibrateTemperature(middle_temp_raw);
        final String expectedMessage = TemperatureMsgEN.getTempMsgPerfect(medTempCelsius, UNIT_CELSIUS.toString()).message;
        assertThat(insightCardOptional.get().message, is(expectedMessage));
    }

    @Test
    public void test_tempTooCold() {

        final int min_temp_raw = (TemperatureHumidity.ALERT_TEMP_MIN_CELSIUS * (int) DataUtils.FLOAT_2_INT_MULTIPLIER) + TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS; //undo temp calibration
        final int max_temp_raw = (TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS * (int) DataUtils.FLOAT_2_INT_MULTIPLIER) + TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS;
        final int middle_temp_raw = (min_temp_raw + max_temp_raw) / 2;

        final List<DeviceData> deviceDataList = Lists.newArrayList(
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, min_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, middle_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, max_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8));

        final Optional<InsightCard> insightCardOptional = TemperatureHumidity.processData(FAKE_ACCOUNT_ID, deviceDataList, Optional.of(Device.Color.WHITE), Optional.absent(), UNIT_CELSIUS);
        assertThat(insightCardOptional.isPresent(), is(Boolean.TRUE));

        final int medTempCelsius = (int) DataUtils.calibrateTemperature(middle_temp_raw);
        final String expectedMessage = TemperatureMsgEN.getTempMsgTooCold(medTempCelsius, UNIT_CELSIUS.toString(), TemperatureHumidity.IDEAL_TEMP_MIN_CELSIUS).message;
        assertThat(insightCardOptional.get().message, is(expectedMessage));
    }

    @Test
    public void test_tempTooHot() {

        final int min_temp_raw = (TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS * (int) DataUtils.FLOAT_2_INT_MULTIPLIER) + TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS; //undo temp calibration
        final int max_temp_raw = (TemperatureHumidity.ALERT_TEMP_MAX_CELSIUS * (int) DataUtils.FLOAT_2_INT_MULTIPLIER) + TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS;
        final int middle_temp_raw = (min_temp_raw + max_temp_raw) / 2;

        final List<DeviceData> deviceDataList = Lists.newArrayList(
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, min_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, middle_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8),
                DeviceData.senseOne(FAKE_ACCOUNT_ID, FAKE_DEVICE_ID, FAKE_EXTERNAL_ID, max_temp_raw, 2, 3, 4, 5, 6, 7, 8, 1f, 1, 2, DateTime.now(), 1, 2, 3, 4, 5, 6, 7, 8));

        final Optional<InsightCard> insightCardOptional = TemperatureHumidity.processData(FAKE_ACCOUNT_ID, deviceDataList, Optional.of(Device.Color.WHITE), Optional.absent(), UNIT_CELSIUS);

        assertThat(insightCardOptional.isPresent(), is(Boolean.TRUE));

        final int medTempCelsius = (int) DataUtils.calibrateTemperature(middle_temp_raw);
        final String expectedMessage = TemperatureMsgEN.getTempMsgTooHot(medTempCelsius, UNIT_CELSIUS.toString(), TemperatureHumidity.IDEAL_TEMP_MAX_CELSIUS).message;
        assertThat(insightCardOptional.get().message, is(expectedMessage));
    }

}
