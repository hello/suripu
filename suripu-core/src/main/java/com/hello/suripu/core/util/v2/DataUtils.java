package com.hello.suripu.core.util.v2;

import com.hello.suripu.core.sense.data.ExtraSensorData;

/**
 * Created by jyfan on 9/20/16.
 */
public class DataUtils {

    //TODO: replace all constants with udpated values from manufacturer

    //RGB sensor constants
    final static float R_COEFF = 0.1949f;
    final static float G_COEFF = 1.0f;
    final static float B_COEFF = 0.293f;

    final static float DF = 436.0f;
    final static float CPL_WHITE = 1.0f; //CPL = counts per lux TODO: need real value, which is calculated from DF (above) and GA
    final static float CPL_BLACK = 5.0f; //TODO: need real value, which is calculated from DF (above) and GA

    final static float CT_ATTEN = 1.0f; //TODO: pretty sure this is needed, related to CPL

    final static float LUX_NEURAL_SCALE = 300.0f; //TODO: recompute after previous constants set

    final static float CT_COEFF = 4417.0f;
    final static float CT_OFFSET = 1053f;

    //UV sensor constants

    //VOC CO2 sensor constants

    //Barometer sensor constants

    public static float convertRawToAmbientLight(final ExtraSensorData extraSensorData) {

        //Read data
        final float rRaw = parseRGB_R(extraSensorData.rgb());
        final float gRaw = parseRGB_G(extraSensorData.rgb());
        final float bRaw = parseRGB_B(extraSensorData.rgb());
        final float clear = extraSensorData.clear();

        //Remove IR component
        final float ir = (rRaw + gRaw + bRaw - clear) / 2.0f;
        final float rClean = rRaw - ir;
        final float gCLEAN = gRaw - ir;
        final float bClean = bRaw - ir;

        //Calc
        final float gPrimePrime = R_COEFF * rClean + G_COEFF * gCLEAN + B_COEFF * bClean;
        final float lux = gPrimePrime / CPL_WHITE;
        return lux;
    }

    //TODO: for scaling light values to possibly biased Sense 1.0 readings, to keep same neural net model
    //TODO: confirm with Benjo that other sensors do not need scaling
    public static float convertLuxToNeuralLux(final float lux) {
        return lux * LUX_NEURAL_SCALE;
    }

    public static float convertRawToColorTemp(final ExtraSensorData extraSensorData) {

        //Read data
        final float rRaw = parseRGB_R(extraSensorData.rgb());
        final float gRaw = parseRGB_G(extraSensorData.rgb());
        final float bRaw = parseRGB_B(extraSensorData.rgb());
        final float clear = extraSensorData.clear();

        //Remove IR component
        final float ir = (rRaw + gRaw + bRaw - clear) / 2.0f;
        final float rClean = rRaw - ir;
        final float bClean = bRaw - ir;

        //Calc
        final float colorTemp = ( CT_COEFF * bClean ) / rClean + CT_OFFSET;
        final float colorTempAttenuated = colorTemp * CT_ATTEN;

        return colorTempAttenuated;
    }

    public static float convertRawToUV(final ExtraSensorData extraSensorData) {
        return 0.0f;
    }

    public static float convertRawToVOC(final ExtraSensorData extraSensorData) {
        return 0.0f;
    }

    public static float convertRawToCO2(final ExtraSensorData extraSensorData) {
        return 0.0f;
    }

    public static float convertRawToPa(final ExtraSensorData extraSensorData) {
        return 0.0f;
    }

    //TODO: actually get RGB
    public static float parseRGB_R(final String rgb) {
        return 0.0f;
    }

    public static float parseRGB_G(final String rgb) {
        return 0.0f;
    }

    public static float parseRGB_B(final String rgb) {
        return 0.0f;
    }
}
