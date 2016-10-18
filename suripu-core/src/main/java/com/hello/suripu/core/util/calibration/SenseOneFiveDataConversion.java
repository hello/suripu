package com.hello.suripu.core.util.calibration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.util.DataUtils;

import java.util.Random;

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

    final static float CT_COEFF = 5500.0f;
    final static float CT_OFFSET = 1000f;

    //UV sensor constants
    final static float UVI_COEFF = 1 / 5500.0f;
    final static float UVI_COR = 1.0f;

    //Temp sensor constants
    private static final int TEMP_MAX_DERIV = 20;
    private static final int TEMP_ALPHA_ONE = -20;
    private static final int TEMP_ALPHA_TWO = 600;

    //Audio
    public static final float MIN_AUDIO_DB = 25.0f; //our reported noise floor (although I doubt we're actually going to go below this)
    public static final float MAX_AUDIO_DB = 160.f; //arbitrarily a very very very loud noise
    public static final float AUDIO_FLOAT_TO_INT_MULTIPLIER = 1000.0f; // 3 decimal places

    //TVOC and CO2 constants
    private static final int MAX_VOC = 3000;
    private static final int VOC_VARIANCE = 10;
    private static final int MAX_CO2 = 1800;
    private static final int CO2_VARIANCE = 10;

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

    public static float convertLuxToNeuralLux(final float lux) {
        return lux * LUX_NEURAL_SCALE;
    }

    //TODO: Constants may need to be adjusted for PVT units
    public static float convertLuxCountToLux(final int luxCount, final Device.Color color) {
        if (color == Device.Color.BLACK) {
            return (float) luxCount / 2.0f;
        }

        return (float) luxCount / 5.0f;
    }

    public static float convertRawToColorTemp(final int rRaw, final int gRaw, final int bRaw, final int clear) {

        //Remove IR component
        final float ir = (rRaw + gRaw + bRaw - clear) / 2.0f;
        final float rClean = rRaw - ir;
        final float bClean = bRaw - ir;

        //Calc
        if (rClean <= 0.0f) {
            return CT_OFFSET;
        }
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

    //Note: should smooth by 5 min buckets before presenting data to user
    public static float convertRawToCelsius(final int tempRaw, final Optional<Integer> tempRawLastMinOptional) {
        if (!tempRawLastMinOptional.isPresent()) {
            return DataUtils.dbIntToFloat(tempRaw - TEMP_ALPHA_TWO); //If T(last min) DNE, assume no change in temp: T(now) = T(last min)
        }

        final int tempCalibration = getTempCalibration(tempRaw, tempRawLastMinOptional.get());
        return DataUtils.dbIntToFloat(tempRaw - tempCalibration);
    }

    @VisibleForTesting
    public static int getTempCalibration(final int tempRaw, final int tempRawLastMin) {
        final int deriv = tempRaw - tempRawLastMin;

        final int deriv_cap = Math.max(Math.min(deriv, TEMP_MAX_DERIV), -TEMP_MAX_DERIV);

        final int calibration = deriv_cap * TEMP_ALPHA_ONE + TEMP_ALPHA_TWO;
        return calibration;
    }

    //units in % humidity
    public static float convertRawToHumidity(final int humidRaw) {
        return DataUtils.dbIntToFloat(humidRaw);
    }

    public static float convertRawAudioToDb(final int rawAudio) {
        //incoming number is in fixed point format Q10, so divide by 1024 to convert it to floating point
        float audioDB = ((float) rawAudio )/ 1024.0f;

        if (audioDB < MIN_AUDIO_DB) {
            return MIN_AUDIO_DB;
        }

        if (audioDB > MAX_AUDIO_DB) {
            return MAX_AUDIO_DB;
        }

        return audioDB;

    }

}
