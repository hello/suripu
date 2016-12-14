package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import com.hello.suripu.core.models.Calibration;
import com.hello.suripu.core.models.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kingshy on 11/25/14.
 */
public class DataUtils{

    private static final Logger LOGGER = LoggerFactory.getLogger(DataUtils.class);
    private static final float MAX_DUST_ANALOG_VALUE = 4095.0f;
    public static final float DUST_FLOAT_TO_INT_MULTIPLIER = 1000000f;
    public static final float AUDIO_FLOAT_TO_INT_MULTIPLIER = 1000.0f; // 3 decimal places
    public static final float FLOAT_2_INT_MULTIPLIER = 100;
    private static final int TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS = 389; // 389 => 7ºF, previous 278 => 5ºF;
    public static final float PEAK_DISTURBANCE_NOISE_FLOOR = 40.0f;

    public static final ImmutableSet<Integer> BLACKLISTED_FIRMWARE = ImmutableSet.of(
        601215661, //0.4.0
        1104455110, //0.8.31
        1558757421, //0.9.21
        1530439804, //0.9.22_rc7
        1358218663, //0.9.22_rc8
        1086645417, //0.11.18
        1035743552, //0.11.22
        750155781, //0.11.23
        1954620455, //0.11.26
        1142580827, //prepservicepack-v2
        482148662, //1.0.1
        710812107 //1.0.5
    );

    /**
     * Dust count calibration & conversion
     * @param rawDustCount raw dust count
     * @param calibrationOptional calibration info if any, absent otherwise
     * @return dust density in microgram per cubic meter
     */
    public static float convertRawDustCountsToDensity(final int rawDustCount, final Optional<Calibration> calibrationOptional) {

        if (calibrationOptional.isPresent()) {
            final int calibratedRawDustCount = calibrateRawDustCount(rawDustCount, calibrationOptional.get());
            return convertDustDataFromCountsToDensity(calibratedRawDustCount, calibrationOptional.get().senseId);
        }
        return convertDustDataFromCountsToDensity(rawDustCount, "");
    }


    /**
     * Dust count calibration. The offset (delta) is calculated in class Calibration
     * @param rawDustCount raw dust count
     * @param calibration calibration info
     * @return calibrated dust count
     */
    public static int calibrateRawDustCount(final int rawDustCount, final Calibration calibration) {
        return rawDustCount + calibration.dustCalibrationDelta;
    }


    /**
     * Dust count --> density conversion
     * @param dustCount dust count, either raw or calibrated
     * @param senseId external sense ID
     * @return dust density in microgram per cubic meter
     */
    public static float convertDustDataFromCountsToDensity(final int dustCount, final String senseId) {
        // TODO: add checks for firmware version when we switch sensor

        final float dustDensity = (dustCount / MAX_DUST_ANALOG_VALUE) * 4.1076f * (0.5f/2.9f);
        if(dustDensity < -0.1f) {
            LOGGER.error("bad calibration for device_id = {}: value was: {}", senseId, dustDensity);
        }

        return Math.max(0.001f, dustDensity) * 1000.0f;
    }

    public static float convertLightCountsToLux(final int rawCount) {
        // TODO: factor in Sense Color whenever we have that
        // applicable for DVT units onwards
        final float maxLux = 8000.0f;
        final float maxCount = 4194304.0f;

        // note internal to external intensity conversion:
        // white sense: 2x
        // dark sense: 10x
        final float whiteMultiplier = 2.0f;

        // set conversion to 2x for now until we have a way to get sense color
        if ((float) rawCount > maxCount) {
            return (whiteMultiplier * maxLux);
        }

        final float internalIntensity = ((float) rawCount / maxCount) * maxLux;
        return  (whiteMultiplier * internalIntensity);
    }

    public static float convertAudioRawToDB(final int rawAudioValue) {
        return ((float) rawAudioValue) / 1024.0f;
    }

    public static int floatToDbIntAudioDecibels(final float value) { return (int) (value * AUDIO_FLOAT_TO_INT_MULTIPLIER); }

    public static float dbIntToFloatAudioDecibels(final int valueFromDatabase) { return ((float) valueFromDatabase) / AUDIO_FLOAT_TO_INT_MULTIPLIER; }

    public static float calibrateTemperature(final int valueFromDatabase) {
        return dbIntToFloat(valueFromDatabase - TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS);
    }

    public static int celsiusToFahrenheit(final double value) { return (int) Math.round((value * 9.0) / 5.0) + 32; }

    public static int fahrenheitToCelsius(final double value) {
        return (int) ((value - 32.0) * (5.0/9.0));
    }

    public static float calibrateLight(final float lightInLux, Device.Color color) {
        float calibratedLight = lightInLux;

        switch (color) {

            case BLACK:
                calibratedLight *= 5.0f;
                break;
            case WHITE:
                break;
            case BLUE:
                break;
            case RED:
                break;
            case AQUA:
                break;
            case YELLOW:
                break;
        }

        return calibratedLight;
    }

    public static int floatToDBInt(final float value){
        return (int)(value * FLOAT_2_INT_MULTIPLIER);
    }

    public static float dbIntToFloat(final int valueFromDB){
        return valueFromDB / FLOAT_2_INT_MULTIPLIER;
    }

    public static float calibrateAudio(final float backgroundDB, final float peakDB, final Integer firmwareVersion) {
        final float removedNoiseFloor = peakDB - 40.0f;
        final float artificialNoiseFloor = 25.0f;

        //Don't allow devices on older firmware to see calibrated audio without the trim
        if (!BLACKLISTED_FIRMWARE.contains(firmwareVersion)) {
            return Math.max((removedNoiseFloor + artificialNoiseFloor), 0);
        }
        return Math.max(removedNoiseFloor, 0) + artificialNoiseFloor;
    }

    //TODO: rewrite
    public static double computeDewPoint(final double temperature, final double humidity) {
        final double saturationVaporPressure = 6.11 * Math.pow(10.0, (7.5 * (temperature / (237.7 + temperature))));
        final double actualVaporPressure = (humidity * saturationVaporPressure) / 100.0;
        final double logVaporPressure = Math.log(actualVaporPressure);
        return (-430.22 + 237.7 * logVaporPressure) / (-1.0 * logVaporPressure + 19.08);
    }

    public static double computeHumidity(final double temperature, final double dewPoint) {
        final double saturationVaporPressure = 6.11 * Math.pow(10.0, (7.5 * (temperature / (237.7 + temperature))));
        final double actualPressure = 6.11 * Math.pow(10.0, (7.5 * (dewPoint / (237.7 + dewPoint))));
        return (actualPressure/saturationVaporPressure) * 100.0;
    }

    public static float calibrateHumidity(final int temperatureFromDB, final int humidityFromDB) {
        // from http://www.gorhamschaffler.com/humidity_formulas.htm
        final double temperature = (double) dbIntToFloat(temperatureFromDB); // celsius
        final double dewPoint = computeDewPoint(temperature,  (double) dbIntToFloat(humidityFromDB));
        final double adjustedTemperature = temperature - ((double) TEMPERATURE_CALIBRATION_FACTOR_IN_CELSIUS/FLOAT_2_INT_MULTIPLIER);
        final double adjustedHumidity = computeHumidity(adjustedTemperature, dewPoint);
        return (float) adjustedHumidity;
    }
}
