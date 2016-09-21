package com.hello.suripu.core.util.calibration;

import com.google.common.annotations.VisibleForTesting;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.util.DataUtils;

/**
 * Created by jyfan on 9/20/16.
 */
public class SenseOneFiveDataConversion {

    //TODO: replace all constants with updated values from manufacturer
    //RGB sensor constants
    final static float ATIME_MS = 16.8f; //set by fw
    final static float AGAIN_X = 64f; //set by fw

    final static float R_COEFF = 0.1949f; //set by sensor
    final static float G_COEFF = 1.0f;
    final static float B_COEFF = 0.293f;

    final static float DGF_WHITE = 798; //set by sensor & mesh material
    final static float DGF_BLACK = 101;

    final static float CPL_WHITE = DGF_WHITE / (ATIME_MS * AGAIN_X);
    final static float CPL_BLACK = DGF_BLACK / (ATIME_MS * AGAIN_X);

    final static float LUX_NEURAL_SCALE = 2f;

    final static float CT_COEFF = 4417.0f;
    final static float CT_OFFSET = 2000f;

    //UV sensor constants
    final static float UVI_COEFF = 1 / 5500.0f;
    final static float UVI_COR = 1.0f;

    //Temp sensor constants
    private static final int MAX_TEMP_CALIBRATION_FACTOR_IN_CELSIUS = 6;
    private static final int MAX_TEMP_CALIBRATION_TIME_MINUTES = 3 * 60;
    private static final int ONE_HOUR_MINUTES = 60;
    private static final int HEAT_RATE_MIN = 140 / ONE_HOUR_MINUTES;

    public static float convertRawRGBCToAmbientLight(final int rRaw, final int gRaw, final int bRaw, final int clear, final Device.Color color) {

        //Remove IR component
        final float ir = Math.max((rRaw + gRaw + bRaw - clear) / 2.0f, 0);
        final float rClean = Math.max(rRaw - ir, 0);
        final float gCLEAN = Math.max(gRaw - ir, 0);
        final float bClean = Math.max(bRaw - ir, 0);

        //Calc
        final float gPrimePrime = R_COEFF * rClean + G_COEFF * gCLEAN + B_COEFF * bClean;

        if (color == Device.Color.BLACK) {
            return gPrimePrime / CPL_BLACK;
        }

        final float lux = gPrimePrime / CPL_WHITE;
        return lux;
    }

    @Deprecated
    public static float approxClearToAmbientLight(final int clear) {
        //note this "direct" mapping of clear => lux is true by luck for white Sense. Not based on physics.
        return clear;
    }

    //Don't use me yet. Intended to work with convertRawRGBCToAmbientLight(), not approxClearToAmbientLight()
    public static float convertLuxToNeuralLux(final float lux) {
        return lux * LUX_NEURAL_SCALE;
    }

    public static float convertRawToColorTemp(final int rRaw, final int gRaw, final int bRaw, final int clear) {

        //Remove IR component
        final float ir = (rRaw + gRaw + bRaw - clear) / 2.0f;
        final float rClean = rRaw - ir;
        final float bClean = bRaw - ir;

        //Calc
        final float colorTemp = ( CT_COEFF * bClean ) / rClean + CT_OFFSET;
        return colorTemp;
    }

    public static float convertRawToUV(final int uvCounts) {

        final float uvIndex = UVI_COEFF * (uvCounts / UVI_COR);
        return uvIndex;
    }

    //returns units in μg/m^3
    public static float convertRawToVOC(final int vocRaw) {
        if (vocRaw <= 0) {
            return 0.0f;
        }

        return (float) vocRaw;
    }

    //returns units in ppm
    public static float convertRawToCO2(final int co2Raw) {
        if (co2Raw <= 0) {
            return 0.0f;
        }

        return (float) co2Raw;
    }

    public static float convertRawToMilliBar(final int pressureRaw) {
        return convertRawToPa(pressureRaw) / 100.0f;
    }

    private static float convertRawToPa(final int pressureRaw) {
        return pressureRaw / 256.0f;
    }

    public static float convertRawToCelsius(final int tempRaw, final int upTimeMinutes) {
        final float tempCalibrationCelsius = getTempCalibrationCelsius(upTimeMinutes);
        return DataUtils.dbIntToFloat(tempRaw) - tempCalibrationCelsius;
    }

    @VisibleForTesting
    public static float getTempCalibrationCelsius(final int uptimeMinutes) {
        if (uptimeMinutes > MAX_TEMP_CALIBRATION_TIME_MINUTES) {
            return MAX_TEMP_CALIBRATION_FACTOR_IN_CELSIUS;
        }
        final float calibration = (float) Math.log(HEAT_RATE_MIN * uptimeMinutes + 1);
        return calibration;
    }

    //units in % humidity
    public static float convertRawToHumidity(final int tempRaw, final int humidRaw, final int upTimeMinutes) {
        final double dewPoint = DataUtils.computeDewPoint((double) DataUtils.dbIntToFloat(tempRaw),  (double) DataUtils.dbIntToFloat(humidRaw));
        final float adjustedHumidity = (float) DataUtils.computeHumidity(convertRawToCelsius(tempRaw, upTimeMinutes), dewPoint);
        return adjustedHumidity;
    }
}
