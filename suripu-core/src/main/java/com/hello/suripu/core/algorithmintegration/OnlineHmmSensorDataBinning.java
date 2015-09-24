package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.hello.suripu.algorithm.hmm.Transition;
import com.hello.suripu.api.datascience.SleepHmmBayesNetProtos;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;
import com.hello.suripu.core.util.OnlineHmmMeasurementParameters;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *  takes raw sensor data and bins it into 5 minute chunks
 *  it also does some processing on the sensor data to come up with some new signals, like light/sound increases
 */
public class OnlineHmmSensorDataBinning {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineHmmSensorDataBinning.class);

    final static protected int MAX_NUMBER_OF_MEAUSUREMENTS = 100; //for sanity check
    final static protected int NUMBER_OF_MILLIS_IN_A_MINUTE = 60000;
    final static protected double LIGHT_INCREASE_THRESHOLD = 1.0;
    final static protected double LIGHT_DECREASE_THRESHOLD = -1.0;


    static public class BinnedData {
        public final double[][] data;
        public final int numMinutesInWindow;
        public final long t0;

        public BinnedData(double[][] data, int numMinutesInWindow,final long t0) {
            this.data = data;
            this.numMinutesInWindow = numMinutesInWindow;
            this.t0 = t0;
        }
    }

    static protected List<Sample> getTimeOfDayAsMeasurement(final List<Sample> light, final double startNaturalLightForbiddedenHour, final double stopNaturalLightForbiddenHour) {
        final List<Sample> minuteData = Lists.newArrayList();

        for (final Sample s : light) {
            //local UTC
            final DateTime t = new DateTime(s.dateTime + s.offsetMillis, DateTimeZone.UTC);
            final float hour = ((float)t.hourOfDay().get()) + t.minuteOfHour().get() / 60.0f;

            float value = 0.0f;
            if (hour > startNaturalLightForbiddedenHour || hour < stopNaturalLightForbiddenHour) {
                value = 1.0f;
            }


            minuteData.add(new Sample(s.dateTime,value,s.offsetMillis));
        }


        return minuteData;
    }


    /*
    *  CONVERT PILL DATA AND SENSOR DATA INTO 2-D ARRAY OF N MINUTE BINS
    *  where N is specified by the model
    *
    *
    *
    * */
    static public Optional<BinnedData> getBinnedSensorData(final AllSensorSampleList sensors, final List<TrackerMotion> pillData,final List<TrackerMotion> partnerPillData,final OnlineHmmMeasurementParameters params,
                                                           final long startTimeMillisInUTC, final long endTimeMillisInUTC,final int timezoneOffset) {

        final List<Sample> light = sensors.get(Sensor.LIGHT);
        final List<Sample> wave = sensors.get(Sensor.WAVE_COUNT);
        final List<Sample> sound = sensors.get(Sensor.SOUND_PEAK_DISTURBANCE);
        final List<Sample> soundCounts = sensors.get(Sensor.SOUND_NUM_DISTURBANCES);

        int max = 0;
        for (SleepHmmBayesNetProtos.MeasType mt : SleepHmmBayesNetProtos.MeasType.values()) {
            if (max < mt.getNumber()) {
                max = mt.getNumber();
            }
        }

        if (max > MAX_NUMBER_OF_MEAUSUREMENTS) {
            return  Optional.absent();
        }

        final int numberOfDataDimensions = max + 1;

        if (light == Collections.EMPTY_LIST || light.isEmpty()) {
            return Optional.absent();
        }

        /* ideally, this is from sundown to sunrise.... but we just say it's a time you're unlikely to be sleeping in natural daylight  */
        final List<Sample> naturalLightForbidden = getTimeOfDayAsMeasurement(light,params.natLightStartHour,params.natLightStopHour);

        final int numMinutesInWindow = params.numMinutesInMeasPeriod;

        //safeguard
        if (numMinutesInWindow <= 0) {
            return Optional.absent();
        }

        final int dataLength = (int) (endTimeMillisInUTC - startTimeMillisInUTC) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesInWindow;

        //allocate sensor data block
        final double[][] data = new double[numberOfDataDimensions][dataLength];

        //zero out data
        for (int i = 0; i < numberOfDataDimensions; i++) {
            Arrays.fill(data[i], 0.0);
        }

        //start filling in the sensor data.

        /////////////////////////////////////////////
        //LOG OF LIGHT, CONTINUOUS
        final Iterator<Sample> it1 = light.iterator();
        Sample prev = null;
        while (it1.hasNext()) {
            final Sample sample = it1.next();
            double value = sample.value - params.lightFloorLux;
            if (value < 0.0) {
                value = 0.0;
            }

            if (prev == null) {
                prev = sample;
            }

            double prevvalue = prev.value - params.lightFloorLux;

            if (prevvalue < 0.0) {
                prevvalue = 0.0;
            }

            //add so we can average later
            addToBin(data, sample.dateTime, value, SleepHmmBayesNetProtos.MeasType.LOG_LIGHT_VALUE, startTimeMillisInUTC, numMinutesInWindow);

            final double v2 = Math.log(value * params.lightPreMultiplier + 1.0) / Math.log(2.0);
            final double v1 = Math.log(prevvalue * params.lightPreMultiplier + 1.0) / Math.log(2.0);

            if (v2 - v1 > LIGHT_INCREASE_THRESHOLD) {
                maxInBin(data, sample.dateTime, 1.0, SleepHmmBayesNetProtos.MeasType.LIGHT_INCREASE_DISTURBANCE_VALUE, startTimeMillisInUTC, numMinutesInWindow);
            }

            if (v2 - v1 < LIGHT_DECREASE_THRESHOLD) {
                maxInBin(data, sample.dateTime, 1.0, SleepHmmBayesNetProtos.MeasType.LIGHT_DECREASE_DISTURBANCE_VALUE, startTimeMillisInUTC, numMinutesInWindow);
            }

            prev = sample;
        }

        //computing average light in bin, so divide by bin size
        for (int i = 0; i < data[SleepHmmBayesNetProtos.MeasType.LOG_LIGHT_VALUE].length; i++) {
            data[SleepHmmBayesNetProtos.MeasType.LOG_LIGHT_VALUE][i] /= numMinutesInWindow; //average

            //transform via log2(4.0 * x + 1.0)
            data[SleepHmmBayesNetProtos.MeasType.LOG_LIGHT_VALUE][i] =  Math.log(data[SleepHmmBayesNetProtos.MeasType.LOG_LIGHT_VALUE][i] * params.lightPreMultiplier + 1.0) / Math.log(2);
        }

        ///////////////////////////
        //PILL MOTION -- GET DISTURBANCES AND PILL ON DURATION
        final Iterator<TrackerMotion> it2 = pillData.iterator();
        while (it2.hasNext()) {
            final TrackerMotion m = it2.next();

            double value = m.value;

            //heartbeat value
            if (value == -1) {
                continue;
            }

            //if there's a disturbance, register it in the disturbance index
            if (value > params.pillMagnitudeForDisturbance) {
                maxInBin(data, m.timestamp, 1.0, SleepHmmBayesNetProtos.MeasType.PILL_MAGNITUDE_DISTURBANCE_VALUE, startTimeMillisInUTC, numMinutesInWindow);
            }

            addToBin(data, m.timestamp, m.onDurationInSeconds,  SleepHmmBayesNetProtos.MeasType.MOTION_DURATION_VALUE, startTimeMillisInUTC, numMinutesInWindow);

        }

        ///////////////////////
        //WAVES
        final Iterator<Sample> it3 = wave.iterator();
        while (it3.hasNext()) {
            final Sample sample = it3.next();
            double value = sample.value;

            //either wave happened or it didn't.. value can be 1.0 or 0.0
            if (value > 0.0 && params.useWavesForDisturbances) {
                maxInBin(data, sample.dateTime, 1.0, SleepHmmBayesNetProtos.MeasType.WAVE_DISTURBANCE_VALUE, startTimeMillisInUTC, numMinutesInWindow);
            }
        }


        //SOUND COUNTS
        final Iterator<Sample> it5 = soundCounts.iterator();
        prev = null;
        while (it5.hasNext()) {
            final Sample sample = it5.next();

            if (prev == null) {
                prev = sample;
            }

            //accumulate
            if (sample.value > 0.0) {
                addToBin(data, sample.dateTime, sample.value, SleepHmmBayesNetProtos.MeasType.LOG_SOUND_VALUE, startTimeMillisInUTC, numMinutesInWindow);
            }

            double logratio = Math.log(sample.value / (prev.value + 1.0)) / Math.log(2.0);

            if (logratio > 2.0 && sample.value > 10) {
                maxInBin(data, sample.dateTime, 1.0, SleepHmmBayesNetProtos.MeasType.SOUND_INCREASE_DISTURBANCE_VALUE, startTimeMillisInUTC, numMinutesInWindow);
            }

            prev = sample;

        }

        //transform via log2 (1.0 + x)
        for (int i = 0; i < data[SleepHmmBayesNetProtos.MeasType.LOG_SOUND_VALUE].length; i++) {
            double value = data[SleepHmmBayesNetProtos.MeasType.LOG_SOUND_VALUE][i];
            if (value >= 0.0) {
                data[SleepHmmBayesNetProtos.MeasType.LOG_SOUND_VALUE][i] =  Math.log(value + 1.0) / Math.log(2);
            }
        }


        //FORBIDDEN NATURAL LIGHT
        final Iterator<Sample> it6 = naturalLightForbidden.iterator();
        while (it6.hasNext()) {
            final Sample sample = it6.next();

            
            if (sample.value > 0.0) {
                maxInBin(data,sample.dateTime,1.0,SleepHmmBayesNetProtos.MeasType.NATURAL_LIGHT_VALUE,startTimeMillisInUTC,numMinutesInWindow);
            }
        }

        //output only artificial light
        for (int t = 0; t < data[0].length; t++) {
            if (data[SleepHmmBayesNetProtos.MeasType.NATURAL_LIGHT_VALUE][t] == 0.0) {
                //no light in the morning
                data[SleepHmmBayesNetProtos.MeasType.LOG_LIGHT_VALUE][t] = 0.0;
            }
            else {
                //at night before bed do not have any "lights on" events
                data[SleepHmmBayesNetProtos.MeasType.LIGHT_INCREASE_DISTURBANCE_VALUE][t] = 0.0;
            }

            data[SleepHmmBayesNetProtos.MeasType.NATURAL_LIGHT_VALUE][t] = 0.0;

        }


        ///////////////////////////
        //PARTNER PILL MOTION
        final Iterator<TrackerMotion> it7 = partnerPillData.iterator();
        while (it7.hasNext()) {
            final TrackerMotion m = it7.next();

            double value = m.value;

            //heartbeat value
            if (value == -1) {
                continue;
            }

            addToBin(data, m.timestamp, m.onDurationInSeconds,  SleepHmmBayesNetProtos.MeasType.PARTNER_MOTION_DURATION_VALUE, startTimeMillisInUTC, numMinutesInWindow);
        }


        final DateTime dateTimeBegin = new DateTime(startTimeMillisInUTC).withZone(DateTimeZone.forOffsetMillis(timezoneOffset));
        final DateTime dateTimeEnd = new DateTime(startTimeMillisInUTC + numMinutesInWindow * NUMBER_OF_MILLIS_IN_A_MINUTE * dataLength).withZone(DateTimeZone.forOffsetMillis(timezoneOffset));

        /*
        LOGGER.debug("t0UTC={},tf={}",dateTimeBegin.toLocalTime().toString(),dateTimeEnd.toLocalTime().toString());
        LOGGER.debug("light={}",getDoubleVectorAsString(data[SleepHmmBayesNetProtos.MeasType.LOG_LIGHT_VALUE]));
        LOGGER.debug("motion={}",getDoubleVectorAsString(data[SleepHmmBayesNetProtos.MeasType.MOTION_DURATION_VALUE]));
        LOGGER.debug("waves={}", getDoubleVectorAsString(data[SleepHmmBayesNetProtos.MeasType.PILL_MAGNITUDE_DISTURBANCE_VALUE]));
        LOGGER.debug("logsc={}", getDoubleVectorAsString(data[SleepHmmBayesNetProtos.MeasType.LOG_SOUND_VALUE]));
        LOGGER.debug("natlight={}", getDoubleVectorAsString(data[SleepHmmBayesNetProtos.MeasType.NATURAL_LIGHT_VALUE]));
        */

        //fill out the indices of the FORBIDDEN TRANSITIONS.  FORBIDDEN!


        return Optional.of(new BinnedData(data,numMinutesInWindow,startTimeMillisInUTC));
    }

    static public Multimap<Integer,Transition> getMotionForbiddenTransitions(final Collection<Transition> forbiddenTransitionList,final double [] motion) {
        final Multimap<Integer, Transition> forbiddenTransitions = ArrayListMultimap.create();

        if (forbiddenTransitionList == null) {
            return forbiddenTransitions;
        }

        for (int t = 0; t < motion.length - 1; t++) {
            //so the rule is, unless I have two consecutive bins of motion,
            //I can't transition from sleep to wake, or whatever the fuck is specified here
            if (!(motion[t] > 0.0 && motion[t + 1] > 0.0)) {

                for (final Transition transition : forbiddenTransitionList) {
                    forbiddenTransitions.put(t, transition);
                }
            }
        }

        return forbiddenTransitions;

    }

    static protected long getTimeFromBin(int bin, int binWidthMinutes, long t0) {
        long t = bin * binWidthMinutes;
        t *= NUMBER_OF_MILLIS_IN_A_MINUTE;
        t += t0;

        return t;
    }


    static protected void maxInBin(double[][] data, long t, double value, final int idx, final long t0, final int numMinutesInWindow) {
        final int tIdx = (int) (t - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            double v1 = data[idx][tIdx];
            double v2 = value;

            if (v1 < v2) {
                v1 = v2;
            }

            data[idx][tIdx] = v1;
        }

    }

    static protected void addToBin(double[][] data, long t, double value, final int idx, final long t0, final int numMinutesInWindow) {
        final int tIdx = (int) (t - t0) / NUMBER_OF_MILLIS_IN_A_MINUTE / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            data[idx][tIdx] += value;
        }
    }

    static protected String getPathAsString(final ImmutableList<Integer> path) {
        String pathString = "";
        boolean first = true;
        for (int alpha : path) {

            if (!first) {
                pathString += ",";
            }
            pathString += String.format("%d",alpha);
            first = false;
        }

        return pathString;
    }


    static public String getDoubleVectorAsString(final double [] vec) {
        String vecString = "[";
        boolean first = true;
        for (double f : vec) {

            if (!first) {
                vecString += ",";
            }
            vecString += String.format("%.1f",f);
            first = false;
        }

        vecString += "]";

        return vecString;
    }


}
