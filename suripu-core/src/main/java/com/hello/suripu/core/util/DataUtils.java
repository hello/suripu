package com.hello.suripu.core.util;

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

    // AQI ranges from 0 to 500;
    // see http://www.sparetheair.com/publications/AQI_Lookup_Table-PM25.pdf
    final private static int DUST_DENSITY_TO_AQI[] = new int[] {
            0, 4, 8, 13, 17, 21, 25, 29, 33, 38, 42, 46, 50, 53, 55, 57, 59, 61, 63, 66,
            68, 70, 72, 74, 76, 78, 80, 82, 84, 87, 89, 91, 93, 95, 97, 99, 102, 105, 107, 110,
            112, 115, 117, 119, 122, 124, 127, 129, 132, 134, 137, 139, 142, 144, 147, 149, 151, 152, 152, 153,
            153, 154, 154, 155, 155, 156, 156, 157, 157, 158, 158, 159, 160, 160, 161, 161, 162, 162, 163, 163,
            164, 164, 165, 165, 166, 166, 167, 167, 168, 168, 169, 169, 170, 170, 171, 171, 172, 172, 173, 173,
            174, 174, 175, 176, 176, 177, 177, 178, 178, 179, 179, 180, 180, 181, 181, 182, 182, 183, 183, 184,
            184, 185, 185, 186, 186, 187, 187, 188, 188, 189, 189, 190, 190, 191, 192, 192, 193, 193, 194, 194,
            195, 195, 196, 196, 197, 197, 198, 198, 199, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209,
            210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229,
            230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249,
            250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268, 269,
            270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282, 283, 284, 285, 286, 287, 288, 289,
            290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302, 303, 304, 305, 306, 307, 308, 309,
            310, 311, 312, 313, 314, 315, 316, 317, 318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329,
            330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, 341, 342, 343, 344, 345, 346, 347, 348, 349,
            350, 351, 352, 353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365, 366, 367, 368, 369,
            370, 371, 372, 373, 374, 375, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389,
            390, 391, 392, 393, 394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 403, 404, 405, 405, 406, 407,
            407, 408, 409, 409, 410, 411, 411, 412, 413, 413, 414, 415, 415, 416, 417, 417, 418, 419, 419, 420,
            420, 421, 422, 422, 423, 424, 424, 425, 426, 426, 427, 428, 428, 429, 430, 430, 431, 432, 432, 433,
            434, 434, 435, 436, 436, 437, 438, 438, 439, 440, 440, 441, 442, 442, 443, 444, 444, 445, 446, 446,
            447, 448, 448, 449, 450, 450, 451, 452, 452, 453, 454, 454, 455, 455, 456, 457, 457, 458, 459, 459,
            460, 461, 461, 462, 463, 463, 464, 465, 465, 466, 467, 467, 468, 469, 469, 470, 471, 471, 472, 473,
            473, 474, 475, 475, 476, 477, 477, 478, 479, 479, 480, 481, 481, 482, 483, 483, 484, 485, 485, 486,
            487, 487, 488, 489, 489, 490, 490, 491, 492, 492, 493, 494, 494, 495, 496, 496, 497, 498, 498, 499, 500
    };


    public static int floatToDbIntDust (final float value) { return  (int) (value * DUST_FLOAT_TO_INT_MULTIPLIER);}

    public static float dbIntToFloatDust(final int valueFromDB) {return ((float)valueFromDB) / DUST_FLOAT_TO_INT_MULTIPLIER;}

    public static float convertRawDustCountsToDensity(final int rawDustCount, final Calibration calibration, final int firmwareVersion) {
        // Expected output unit: microgram per cubic meter
        final int calibratedRawDustCount = calibrateRawDustCount(rawDustCount, calibration);
        return convertDustDataFromCountsToDensity(calibratedRawDustCount, firmwareVersion) * 1000.0f;
    }

    public static int calibrateRawDustCount(final int rawDustCount, final Calibration calibration) {
        return rawDustCount + calibration.dustCalibrationDelta;
    }

    public static int convertDustDensityToAQI (final float value) {
        // this simply converts dust density from milligram per cubic meter to microgram per cubic meter
        final int roundValue = Math.round(value * 1000.0f);

        if(roundValue >= DUST_DENSITY_TO_AQI.length) {
            LOGGER.trace("Dust density {} is beyond maximum convertible value", roundValue);
            return DUST_DENSITY_TO_AQI[DUST_DENSITY_TO_AQI.length - 1];
        }

        if(roundValue < 0) {
            LOGGER.trace("Dust density {} is less than minimum convertible value", roundValue);
            return DUST_DENSITY_TO_AQI[0];
        }

        return DUST_DENSITY_TO_AQI[roundValue];
    }

    public static float convertDustDataFromCountsToDensity(final int calibratedDustCount, final int firmwareVersion) {
        // TODO: add checks for firmware version when we switch sensor

        final float dustDensity = (calibratedDustCount / MAX_DUST_ANALOG_VALUE) * 4.1076f * (0.5f/2.9f);
        return dustDensity; // milligram per cubic meter
    }

    public static float convertLightCountsToLux(final int rawCount) {
        // TODO: factor in Sense Color whenever we have that
        // applicable for DVT units onwards
        final float maxLux = 125.0f;
        final float maxCount = 65536.0f; // 16-bit counts

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

    public static float calibrateAudio(final float backgroundDB, final float peakDB) {
        return Math.max(peakDB - 40, 0) + 25;
    }

    private static double computeDewPoint(final double temperature, final double humidity) {
        final double saturationVaporPressure = 6.11 * Math.pow(10.0, (7.5 * (temperature / (237.7 + temperature))));
        final double actualVaporPressure = (humidity * saturationVaporPressure) / 100.0;
        final double logVaporPressure = Math.log(actualVaporPressure);
        return (-430.22 + 237.7 * logVaporPressure) / (-1.0 * logVaporPressure + 19.08);
    }

    private static double computeHumidity(final double temperature, final double dewPoint) {
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
