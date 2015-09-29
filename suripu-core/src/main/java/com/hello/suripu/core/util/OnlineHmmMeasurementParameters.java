package com.hello.suripu.core.util;

import com.hello.suripu.api.datascience.SleepHmmBayesNetProtos;

/**
 * Created by benjo on 6/24/15.
 */
public class OnlineHmmMeasurementParameters {
    public static Boolean DEFAULT_ENABLE_INTERVAL_SEARCH = false;
    public static Double DEFAULT_LIGHT_FLOOR_LUX = 0.0;
    public static Double DEFAULT_LIGHT_PRE_MULTIPLER = 1.0;
    public static Double DEFAULT_PILL_MAGNITUDE_FOR_DISTURBANCE = 15000.0;
    public static Double DEFAULT_NATURAL_LIGHT_FILTER_START_HOUR = 16.0;
    public static Double DEFAULT_NATURAL_LIGHT_FILTER_STOP_HOUR = 4.0;
    public static Boolean DEFAULT_USE_WAVES_AS_DISTURBANCES = true;
    public static Integer DEFAULT_NUM_MINUTES_IN_MEASUREMENT_PERIOD = 5;

    public static String CONDITIONAL_PROBABILITY_OF_SLEEP = "p_state_given_sleep";
    public static String CONDITIONAL_PROBABILITY_OF_BED = "p_state_given_bed";


    public final Boolean enableIntervalSearch;
    public final Double lightFloorLux;
    public final Double lightPreMultiplier;
    public final Double pillMagnitudeForDisturbance;
    public final Double natLightStartHour;
    public final Double natLightStopHour;
    public final Boolean useWavesForDisturbances;
    public final Integer numMinutesInMeasPeriod;

    public OnlineHmmMeasurementParameters(Boolean enableIntervalSearch, Double lightFloorLux, Double lightPreMultiplier, Double pillMagnitudeForDisturbance, Double natLightStartHour, Double natLightStopHour, Boolean useWavesForDisturbances, Integer numMinutesInMeasPeriod) {
        this.enableIntervalSearch = enableIntervalSearch;
        this.lightFloorLux = lightFloorLux;
        this.lightPreMultiplier = lightPreMultiplier;
        this.pillMagnitudeForDisturbance = pillMagnitudeForDisturbance;
        this.natLightStartHour = natLightStartHour;
        this.natLightStopHour = natLightStopHour;
        this.useWavesForDisturbances = useWavesForDisturbances;
        this.numMinutesInMeasPeriod = numMinutesInMeasPeriod;
    }

    static public OnlineHmmMeasurementParameters createFromProto(SleepHmmBayesNetProtos.MeasurementParams params) {
        Boolean enableIntervalSearch = DEFAULT_ENABLE_INTERVAL_SEARCH;
        if (params.hasEnableIntervalSearch()) {
            enableIntervalSearch = params.getEnableIntervalSearch();
        }

        Double lightFloorLux = DEFAULT_LIGHT_FLOOR_LUX;
        if (params.hasLightFloorLux()) {
            lightFloorLux = params.getLightFloorLux();
        }

        Double lightPreMultiplier = DEFAULT_LIGHT_PRE_MULTIPLER;
        if (params.hasLightPreMultiplier()) {
            lightPreMultiplier = DEFAULT_LIGHT_PRE_MULTIPLER;
        }

        Double pillMagnitudeForDisturbance = DEFAULT_PILL_MAGNITUDE_FOR_DISTURBANCE;
        if (params.hasMotionCountForDisturbances()) {
            pillMagnitudeForDisturbance = params.getMotionCountForDisturbances();
        }

        Double natLightStartHour = DEFAULT_NATURAL_LIGHT_FILTER_START_HOUR;
        if (params.hasNaturalLightFilterStartHour()) {
            natLightStartHour = params.getNaturalLightFilterStartHour();
        }

        Double natLightStopHour = DEFAULT_NATURAL_LIGHT_FILTER_STOP_HOUR;
        if (params.hasNaturalLightFilterStopHour()) {
            natLightStopHour = params.getNaturalLightFilterStopHour();
        }

        Boolean useWavesForDisturbances = DEFAULT_USE_WAVES_AS_DISTURBANCES;
        if (params.hasUseWavesForDisturbances()) {
            useWavesForDisturbances = params.getUseWavesForDisturbances();
        }

        Integer numMinutesInMeasPeriod = DEFAULT_NUM_MINUTES_IN_MEASUREMENT_PERIOD;
        if (params.hasNumMinutesInMeasPeriod()) {
            numMinutesInMeasPeriod = params.getNumMinutesInMeasPeriod();
        }


        return new OnlineHmmMeasurementParameters(enableIntervalSearch,lightFloorLux,lightPreMultiplier,pillMagnitudeForDisturbance,natLightStartHour,natLightStopHour,useWavesForDisturbances,numMinutesInMeasPeriod);
    }

}
