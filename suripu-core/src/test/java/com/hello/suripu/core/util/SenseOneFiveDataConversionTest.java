package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.util.calibration.SenseOneFiveDataConversion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by jyfan on 9/21/16.
 */
public class SenseOneFiveDataConversionTest {

    //Sanity check ranges
    //See https://github.com/hello/research/blob/master/Jingyun_LabBooks/lab_1dot5sensors.pdf
    final float ALERT_MBAR_HIGH = 1300;
    final float ALERT_MBAR_LOW = 860;

    final float ALERT_TVOC_HIGH = 500;

    final float ALERT_CO2_HIGH = 1200;

    final float ALERT_UV_HIGH = 2;

    @Test
    public void testRGBtoLux_one() throws IOException {
        final float LUXMETER_GOLDSTANDARD_MIN = 500;
        final float LUXMETER_GOLDSTANDARD_MAX = 550;
        final float MAX_ERROR_RATE = 0.01f;

        int errorCount = 0;

        final List<List<Integer>> rgbcData = readRGBCData("fixtures/calibration/sense15rgbc_raw_white_500lux.csv");
        for (final List<Integer> rgbcDatum : rgbcData) {
            final int r = rgbcDatum.get(0);
            final int g = rgbcDatum.get(1);
            final int b = rgbcDatum.get(2);
            final int c = rgbcDatum.get(3);

            final float lux = SenseOneFiveDataConversion.convertRawRGBCToAmbientLight(r, g, b, c, Device.Color.WHITE);
            if (lux > LUXMETER_GOLDSTANDARD_MAX || lux < LUXMETER_GOLDSTANDARD_MIN) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / rgbcData.size();
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testRGBtoLux_two() throws IOException {
        final float LUXMETER_GOLDSTANDARD_MIN = 500;
        final float LUXMETER_GOLDSTANDARD_MAX = 550;
        final float MAX_ERROR_RATE = 0.02f;

        int errorCount = 0;

        final List<List<Integer>> rgbcData = readRGBCData("fixtures/calibration/sense15rgbc_raw_black_500lux.csv");
        for (final List<Integer> rgbcDatum : rgbcData) {
            final int r = rgbcDatum.get(0);
            final int g = rgbcDatum.get(1);
            final int b = rgbcDatum.get(2);
            final int c = rgbcDatum.get(3);

            final float lux = SenseOneFiveDataConversion.convertRawRGBCToAmbientLight(r, g, b, c, Device.Color.BLACK);
            if (lux > LUXMETER_GOLDSTANDARD_MAX || lux < LUXMETER_GOLDSTANDARD_MIN) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / rgbcData.size();
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }


    @Test
    public void testRGBCNeuralScale() throws IOException {

        final float MAX_LUX_ERROR = 100;
        final float MAX_LUX_ERROR_RELATIVE = 0.3f;
        final float MAX_ERROR_RATE = 0.15f;

        final List<Integer> sense1_ambient_light = readSense1Data("fixtures/calibration/sense1als_sense15clear.csv");
        final List<List<Integer>> rgbcData = readRGBCData("fixtures/calibration/sense15rgbc_raw.csv");

        final int length = Math.min(sense1_ambient_light.size(), rgbcData.size());

        int errorCount = 0;
        for (int i = 0; i < length; i++) {
            final float sense1_lux = DataUtils.convertLightCountsToLux(sense1_ambient_light.get(i));

            final List<Integer> rgbcDatum = rgbcData.get(i);

            final int r = rgbcDatum.get(0);
            final int g = rgbcDatum.get(1);
            final int b = rgbcDatum.get(2);
            final int c = rgbcDatum.get(3);

            final float lux = SenseOneFiveDataConversion.convertRawRGBCToAmbientLight(r, g, b, c, Device.Color.WHITE);
            final float lux_neural = SenseOneFiveDataConversion.convertLuxToNeuralLux(lux);


            final float errorLux = Math.abs(sense1_lux - lux_neural);
            final float errorLuxRelative = errorLux / sense1_lux;

            if (errorLux > MAX_LUX_ERROR && errorLuxRelative > MAX_LUX_ERROR_RELATIVE) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / length;
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testClearToLux_one() throws IOException {
        final float MAX_LUX_ERROR = 100;
        final float MAX_LUX_ERROR_RELATIVE = 0.3f;
        final float MAX_ERROR_RATE = 0.15f;

        final List<Integer> sense1_ambient_light = readSense1Data("fixtures/calibration/sense1als_sense15clear.csv");
        final List<Integer> sense15_ambient_light = readSense15Data("fixtures/calibration/sense1als_sense15clear.csv");

        int errorCount = 0;

        final int length = Math.min(sense1_ambient_light.size(), sense15_ambient_light.size());
        for (int i = 0; i < length; i++) {
            final float sense1_lux = DataUtils.convertLightCountsToLux(sense1_ambient_light.get(i));
            final float sense15_lux = SenseOneFiveDataConversion.approxClearToAmbientLight(sense15_ambient_light.get(i));

            final float error_lux = Math.abs(sense1_lux - sense15_lux);
            final float errorLuxRelative = error_lux / sense1_lux;

            if (error_lux > MAX_LUX_ERROR && errorLuxRelative > MAX_LUX_ERROR_RELATIVE) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / length; //0.013
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testClearToLux_two() throws IOException {
        final float MAX_LUX_ERROR = 100;
        final float MAX_LUX_ERROR_RELATIVE = 0.3f;
        final float MAX_ERROR_RATE = 0.15f;

        final List<Integer> sense1_ambient_light = readSense1Data("fixtures/calibration/sense1als_sense15lite.csv");
        final List<Integer> sense15_ambient_light = readSense15Data("fixtures/calibration/sense1als_sense15lite.csv");

        int errorCount = 0;

        final int length = Math.min(sense1_ambient_light.size(), sense15_ambient_light.size());
        for (int i = 0; i < length; i++) {
            final float sense1_lux = DataUtils.convertLightCountsToLux(sense1_ambient_light.get(i));
            final float sense15_lux = SenseOneFiveDataConversion.approxClearToAmbientLight(sense15_ambient_light.get(i));

            final float error_lux = Math.abs(sense1_lux - sense15_lux);
            final float errorLuxRelative = error_lux / sense1_lux;

            if (error_lux > MAX_LUX_ERROR && errorLuxRelative > MAX_LUX_ERROR_RELATIVE) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / length; //0.013
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testColorTemp_one() throws IOException {
        final float OFFICE_CT_ALERT_HIGH = 4000;
        final float OFFICE_CT_ALERT_LOW = 3000;
        final float MAX_ERROR_RATE = 0.01f;

        int errorCount = 0;

        final List<List<Integer>> rgbcData = readRGBCData("fixtures/calibration/sense15rgbc_raw_white_500lux.csv");
        for (final List<Integer> rgbcDatum : rgbcData) {
            final int r = rgbcDatum.get(0);
            final int g = rgbcDatum.get(1);
            final int b = rgbcDatum.get(2);
            final int c = rgbcDatum.get(3);

            final float kelvin = SenseOneFiveDataConversion.convertRawToColorTemp(r, g, b, c);
            if (kelvin > OFFICE_CT_ALERT_HIGH || kelvin < OFFICE_CT_ALERT_LOW) {

                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / rgbcData.size();
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testCounttoUVIndex_one() throws IOException {

        final List<Integer> sense15uv_raw = readSense15Data("fixtures/calibration/sense15uv_raw.csv");
        for (final int uv_raw : sense15uv_raw) {
            final float uv_index = SenseOneFiveDataConversion.convertRawToUV(uv_raw);
            assertThat(uv_index < ALERT_UV_HIGH, is(Boolean.TRUE));
        }
    }

    @Test
    public void testVOC_one() throws IOException {

        final List<Integer> sense15tvoc_raw = readSense15Data("fixtures/calibration/sense15tvoc_raw.csv");
        for (final int tvoc_raw : sense15tvoc_raw) {
            final float mugMcube = SenseOneFiveDataConversion.convertRawToVOC(tvoc_raw);
            assertThat(mugMcube < ALERT_TVOC_HIGH, is(Boolean.TRUE));
        }
    }

    @Test
    public void testCO2_one() throws IOException {

        final List<Integer> sense15co2_raw = readSense15Data("fixtures/calibration/sense15co2_raw.csv");
        for (final int co2_raw : sense15co2_raw) {
            final float ppm = SenseOneFiveDataConversion.convertRawToCO2(co2_raw);
            assertThat(ppm < ALERT_CO2_HIGH, is(Boolean.TRUE));
        }
    }

    @Test
    public void testRawToMilliBar_one() throws IOException {

        final List<Integer> sense15_pa_raw = readSense15Data("fixtures/calibration/sense15pa_raw.csv");
        for (final int pa_raw : sense15_pa_raw) {
            final float milliBar = SenseOneFiveDataConversion.convertRawToMilliBar(pa_raw);
            assertThat(milliBar < ALERT_MBAR_HIGH, is(Boolean.TRUE));
            assertThat(milliBar > ALERT_MBAR_LOW, is(Boolean.TRUE));
        }
    }

    @Test
    public void testRawToCelsius_one() throws IOException {
        final float MAX_TMP_ERROR_CELSIUS = 5f;
        final float MAX_ERROR_RATE = 0.01f;

        final List<Integer> sense1_tmp = readSense1Data("fixtures/calibration/sense1tmp_sense15tmp.csv");
        final List<Integer> sense15_tmp = readSense15Data("fixtures/calibration/sense1tmp_sense15tmp.csv");

        int errorCount = 0;

        final int length = Math.min(sense1_tmp.size(), sense15_tmp.size());
        for (int i = 0; i < length; i++) {
            Optional<Integer> tempRawLastOptional;
            if (i == 0) {
                tempRawLastOptional = Optional.absent();
            } else {
                tempRawLastOptional = Optional.of(sense15_tmp.get(i-1));
            }
            final float sense1_tmp_c = DataUtils.calibrateTemperature(sense1_tmp.get(i));
            final float sense15_tmp_c = SenseOneFiveDataConversion.convertRawToCelsius(sense15_tmp.get(i), tempRawLastOptional);

            final float error_tmp_c = Math.abs(sense1_tmp_c - sense15_tmp_c);


            if (error_tmp_c > MAX_TMP_ERROR_CELSIUS) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / length;
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testRawToCelsius_two() throws IOException {
        final float ACURITE_GOLDSTANDARD = 23;
        final float MAX_TEMP_ERROR_C = 2;
        final float MAX_ERROR_RATE = 0.05f;

        final List<Integer> sense15_temp = readSense15Data("fixtures/calibration/sense15tmp_raw_boot_start.csv");
        final List<Integer> uptime = IntStream.range(0, sense15_temp.size()).boxed().collect(Collectors.toList());

        int error = 0;
        final int length = Math.min(sense15_temp.size(), uptime.size());
        for (int i = 0; i < length; i++) {
            Optional<Integer> tempRawLastOptional;
            if (i == 0) {
                tempRawLastOptional = Optional.absent();
            } else {
                tempRawLastOptional = Optional.of(sense15_temp.get(i-1));
            }

            final int temp_raw = sense15_temp.get(i);
            final float temp_c = SenseOneFiveDataConversion.convertRawToCelsius(temp_raw, tempRawLastOptional);

            final float error_temp_c = Math.abs(temp_c - ACURITE_GOLDSTANDARD);

            if (error_temp_c > MAX_TEMP_ERROR_C) {
                error += 1;
            }
        }

        final float errorRate = (float) error / length;

        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testRawToHumidity_one() throws IOException {
        final float MAX_HUM_ERROR = 10;
        final float MAX_ERROR_RATE = 0.05f;

        final List<Integer> sense1_hum_data = readSense1Data("fixtures/calibration/sense1hum_sense15hum.csv");
        final List<Integer> sense1_temp = readSense1Data("fixtures/calibration/sense1tmp_sense15tmp.csv");

        final List<Integer> sense15_hum_data = readSense15Data("fixtures/calibration/sense1hum_sense15hum.csv");
        final List<Integer> sense15_temp = readSense15Data("fixtures/calibration/sense1tmp_sense15tmp.csv");

        int errorCount = 0;

        final int length = Math.min(sense1_hum_data.size(), sense15_hum_data.size());
        for (int i = 0; i < length; i++) {
            Optional<Integer> tempRawLastOptional;
            if (i == 0) {
                tempRawLastOptional = Optional.absent();
            } else {
                tempRawLastOptional = Optional.of(sense15_temp.get(i-1));
            }

            final float sense1_hum = DataUtils.calibrateHumidity(sense1_temp.get(i), sense1_hum_data.get(i));
            final float sense15_hum = SenseOneFiveDataConversion.convertRawToHumidity(sense15_hum_data.get(i)); //Past max calibration time
            final float error_hum = Math.abs(sense1_hum - sense15_hum);

            if (error_hum > MAX_HUM_ERROR) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / length;
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testRawToHumidity_two() throws IOException {
        final float ACURITE_GOLDSTANDARD = 42; //+3-ed
        final float MAX_TEMP_ERROR_HUM = 10; //error is biased to be around 12
        final float MAX_ERROR_RATE = 0.05f;

        final List<Integer> sense15_temp = readSense15Data("fixtures/calibration/sense15tmp_raw_boot_start.csv");
        final List<Integer> sense15_hum = readSense15Data("fixtures/calibration/sense15hum_raw_boot_start.csv");

        int error = 0;
        final int length = Math.min(sense15_temp.size(), sense15_hum.size());
        for (int i = 0; i < length; i++) {
            final int hum_raw = sense15_hum.get(i);

            final float hum = SenseOneFiveDataConversion.convertRawToHumidity(hum_raw);

            final float error_temp_hum = Math.abs(hum - ACURITE_GOLDSTANDARD);

            if (error_temp_hum > MAX_TEMP_ERROR_HUM) {
                error += 1;
            }
        }

        final float errorRate = (float) error / length;
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));

    }

    private static List<Integer> readSense1Data(final String fileName) throws IOException {

        final List<Integer> sense1Data = Lists.newArrayList();
        final URL userCSVFile = Resources.getResource(fileName);
        final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);
        final String sense1String = csvString.split("\\n")[0];

        final String[] sense1dataStringList = sense1String.replaceAll("\\s+","").split(",");
        for (String sense1DataString : sense1dataStringList) {
            sense1Data.add(Integer.parseInt(sense1DataString));
        }

        return sense1Data;
    }

    private static List<Integer> readSense15Data(final String fileName) throws IOException {

        final List<Integer> sense15Data = Lists.newArrayList();
        final URL userCSVFile = Resources.getResource(fileName);
        final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);
        final String sense15String = csvString.split("\\n")[1];

        final String[] sense15dataStringList = sense15String.replaceAll("\\s+","").split(",");
        for (String sense15DataString : sense15dataStringList) {
            sense15Data.add(Integer.parseInt(sense15DataString));
        }

        return sense15Data;
    }

    private static List<List<Integer>> readRGBCData(final String fileName) throws IOException {

        final List<List<Integer>> rgbcList = Lists.newArrayList();
        final URL userCSVFile = Resources.getResource(fileName);
        final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);

        final String[] rows = csvString.split("\\n");
        for (final String row : rows) {
            String[] rowDataString = row.replaceAll("\\s+","").split(",");
            final List<Integer> rowData = Lists.newArrayList();
            for (final String string : rowDataString) {
                rowData.add(Integer.parseInt(string));
            }
            rgbcList.add(rowData);
        }

        return rgbcList;
    }

}
