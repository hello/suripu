package com.hello.suripu.core.util;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.device.v2.Sense;
import com.hello.suripu.core.util.calibration.SenseOneFiveDataConversion;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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


//    @Test
    public void testRGBCNeuralScale() throws IOException {

        final float MAX_LUX_ERROR = 100;
        final float MAX_LUX_ERROR_RELATIVE = 0.3f;
        final float MAX_ERROR_RATE = 0.15f;

        final List<Integer> sense1_ambient_light = readSenseData("fixtures/calibration/sense1als_sense15clear.csv", "sense 1.0");
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
    public void testLuxCountToLux_one() throws IOException {
        final float LUXMETER_GOLDSTANDARD_MIN = 500;
        final float LUXMETER_GOLDSTANDARD_MAX = 650;
        final float MAX_ERROR_RATE = 0.02f;

        int errorCount = 0;

        final List<Integer> luxCountData = readSenseData("fixtures/calibration/sense15lux_count_black_550lux.csv", "sense 1.5");
        for (final Integer datum : luxCountData) {
            final float lux = SenseOneFiveDataConversion.convertLuxCountToLux( datum, Device.Color.BLACK);
            if (lux > LUXMETER_GOLDSTANDARD_MAX || lux < LUXMETER_GOLDSTANDARD_MIN) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / luxCountData.size();
        assertThat( errorRate < MAX_ERROR_RATE, is(Boolean.TRUE));
    }

    @Test
    public void testLuxCountToLux_two() throws IOException {
        final float LUXMETER_GOLDSTANDARD_MIN = 500;
        final float LUXMETER_GOLDSTANDARD_MAX = 650;
        final float MAX_ERROR_RATE = 0.02f;

        int errorCount = 0;

        final List<Integer> luxCountData = readSenseData("fixtures/calibration/sense15lux_count_white_550lux.csv", "sense 1.5");
        for (final Integer datum : luxCountData) {
            final float lux = SenseOneFiveDataConversion.convertLuxCountToLux( datum, Device.Color.WHITE);
            if (lux > LUXMETER_GOLDSTANDARD_MAX || lux < LUXMETER_GOLDSTANDARD_MIN) {
                errorCount += 1;
            }
        }

        final float errorRate = (float) errorCount / luxCountData.size();
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

        final List<Integer> sense15uv_raw = readSenseData("fixtures/calibration/sense15uv_raw.csv", "sense 1.0");
        for (final int uv_raw : sense15uv_raw) {
            final float uv_index = SenseOneFiveDataConversion.convertRawToUV(uv_raw);
            assertThat(uv_index < ALERT_UV_HIGH, is(Boolean.TRUE));
        }
    }

    @Test
    public void testVOC_one() throws IOException {

        final List<Integer> sense15tvoc_raw = readSenseData("fixtures/calibration/sense15tvoc_raw.csv", "sense 1.5");
        for (final int tvoc_raw : sense15tvoc_raw) {
            final float mugMcube = SenseOneFiveDataConversion.convertRawToVOC(tvoc_raw);
            assertThat(mugMcube < ALERT_TVOC_HIGH, is(Boolean.TRUE));
        }
    }

    @Test
    public void testCO2_one() throws IOException {

        final List<Integer> sense15co2_raw = readSenseData("fixtures/calibration/sense15co2_raw.csv", "sense 1.5");
        for (final int co2_raw : sense15co2_raw) {
            final float ppm = SenseOneFiveDataConversion.convertRawToCO2(co2_raw);
            assertThat(ppm < ALERT_CO2_HIGH, is(Boolean.TRUE));
        }
    }

    @Test
    public void testRawToMilliBar_one() throws IOException {

        final List<Integer> sense15_pa_raw = readSenseData("fixtures/calibration/sense15pa_raw.csv", "sense 1.5");
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

        final List<Integer> sense1_tmp = readSenseData("fixtures/calibration/sense1tmp_sense15tmp.csv", "sense 1.0");
        final List<Integer> sense15_tmp = readSenseData("fixtures/calibration/sense1tmp_sense15tmp.csv", "sense 1.5");

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
        final float MAX_ERROR_RATE = 0.06f;

        final List<Integer> sense15_temp = readSenseData("fixtures/calibration/sense15tmp_raw_boot_start.csv", "sense 1.5");
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

        final List<Integer> sense1_hum_data = readSenseData("fixtures/calibration/sense1hum_sense15hum.csv", "sense 1.0");
        final List<Integer> sense1_temp = readSenseData("fixtures/calibration/sense1tmp_sense15tmp.csv", "sense 1.0");

        final List<Integer> sense15_hum_data = readSenseData("fixtures/calibration/sense1hum_sense15hum.csv", "sense 1.5");
        final List<Integer> sense15_temp = readSenseData("fixtures/calibration/sense1tmp_sense15tmp.csv", "sense 1.5");

        int errorCount = 0;

        final int length = Math.min(sense1_hum_data.size(), sense15_hum_data.size());
        for (int i = 0; i < length; i++) {
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

        final List<Integer> sense15_temp = readSenseData("fixtures/calibration/sense15tmp_raw_boot_start.csv", "sense 1.5");
        final List<Integer> sense15_hum = readSenseData("fixtures/calibration/sense15hum_raw_boot_start.csv", "sense 1.5");

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


    @Test
    public void testNumerical_random() {
        final Random random = new Random();

        final int num_trials = 200;
        for (int i = 0; i < num_trials; i++) {

            final int int1 = random.nextInt(16); //We don't expect larger ints
            final int int2 = random.nextInt(16); //so may as well reserve test resources to more likely values
            final int int3 = random.nextInt(16);
            final int int4 = random.nextInt(16);

            final float maxF = 2000.0f;
            final float minF = -100.0f;
            final float float1 = random.nextFloat() * (maxF - minF) + minF;

            SenseOneFiveDataConversion.convertRawRGBCToAmbientLight(int1, int2, int3, int4, Device.Color.WHITE);
            SenseOneFiveDataConversion.convertRawRGBCToAmbientLight(int1, int2, int3, int4, Device.Color.BLACK);
            SenseOneFiveDataConversion.convertLuxToNeuralLux(float1);
            SenseOneFiveDataConversion.convertLuxCountToLux(int1, Device.Color.WHITE);
            SenseOneFiveDataConversion.convertLuxCountToLux(int1, Device.Color.BLACK);
            SenseOneFiveDataConversion.convertRawToColorTemp(int1, int2, int3, int4);
            SenseOneFiveDataConversion.convertRawToUV(int1);
            SenseOneFiveDataConversion.convertRawToVOC(int1);
            SenseOneFiveDataConversion.convertRawToCO2(int1);
            SenseOneFiveDataConversion.convertRawToMilliBar(int1);
            SenseOneFiveDataConversion.convertRawToCelsius(int1, Optional.of(int2));
            SenseOneFiveDataConversion.getTempCalibration(int1, int2);
            SenseOneFiveDataConversion.convertRawToHumidity(int1);
            SenseOneFiveDataConversion.convertRawAudioToDb(int1);

        }

    }

    private static List<Integer> readSenseData(final String fileName, final String senseVersion) throws IOException {

        final URL userCSVFile = Resources.getResource(fileName);
        final String csvString = Resources.toString(userCSVFile, Charsets.UTF_8);
        final String[] lines = csvString.split("\\n");

        final int sense10index = 0;
        final int sense15index = 1;

        final List<Integer> sense10Data = Lists.newArrayList();
        final List<Integer> sense15Data = Lists.newArrayList();
        for(int i = 1; i < lines.length; i++) {
            final String[] line = lines[i].replaceAll("\\s+","").split(",");

            final String sense10Datum = line[sense10index];
            final String sense15Datum = line[sense15index];

            if (!sense10Datum.contains("na")) {
                sense10Data.add(Integer.parseInt(sense10Datum));
            }

            if (!sense15Datum.contains("na")) {
                sense15Data.add(Integer.parseInt(sense15Datum));
            }

        }

        if (senseVersion == "sense 1.0") {
            return sense10Data;
        } else if (senseVersion == "sense 1.5") {
            return sense15Data;
        }

        return Lists.newArrayList();
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
