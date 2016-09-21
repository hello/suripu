package com.hello.suripu.core.util.calibration;

/**
 * Created by jyfan on 9/20/16.
 */
public class SenseOneFiveCalibration {

    //TODO: replace all constants with udpated values from manufacturer

    //RGB sensor constants
    final static float ATIME_MS = 16.8f; //set by fw
    final static float AGAIN_X = 64f; //set by fw

    final static float R_COEFF = 0.1949f; //set by sensor
    final static float G_COEFF = 1.0f;  //set by sensor
    final static float B_COEFF = 0.293f; //set by sensor

    final static float DGF_WHITE = 380 * 10f; //set by sensor & mesh material
    final static float DGF_BLACK = 380 * 20f; //set by sensor & mesh material TODO: no idea what this is

    final static float CPL_WHITE = DGF_WHITE / (ATIME_MS * AGAIN_X); //CPL = counts per lux
    final static float CPL_BLACK = DGF_BLACK / (ATIME_MS * AGAIN_X);

    final static float CT_ATTEN = 1.0f; //TODO: pretty sure this is needed, related to CPL

    final static float LUX_NEURAL_SCALE = 300.0f; //TODO: recompute after previous constants set

    final static float CT_COEFF = 4417.0f;
    final static float CT_OFFSET = 1053f;

    //UV sensor constants

    //VOC CO2 sensor constants

    //Barometer sensor constants

    public static float convertRawToAmbientLight(final int rRaw, final int gRaw, final int bRaw, final int clear) {

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

    public static float convertRawToColorTemp(final int rRaw, final int gRaw, final int bRaw, final int clear) {

        //Remove IR component
        final float ir = (rRaw + gRaw + bRaw - clear) / 2.0f;
        final float rClean = rRaw - ir;
        final float bClean = bRaw - ir;

        //Calc
        final float colorTemp = ( CT_COEFF * bClean ) / rClean + CT_OFFSET;
        final float colorTempAttenuated = colorTemp * CT_ATTEN;

        return colorTempAttenuated;
    }

    public static float convertRawToUV() {
        return 0.0f;
    }

    public static float convertRawToVOC() {
        return 0.0f;
    }

    public static float convertRawToCO2() {
        return 0.0f;
    }

    public static float convertRawToPa() {
        return 0.0f;
    }
}
