package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.hello.suripu.algorithm.hmm.HiddenMarkovModel;
import com.hello.suripu.core.models.AllSensorSampleList;
import com.hello.suripu.core.models.Sample;
import com.hello.suripu.core.models.Sensor;
import com.hello.suripu.core.models.TrackerMotion;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by benjo on 2/22/15.
 */
public class HmmUtils {
    final static private int NUM_DATA_DIMENSIONS = 3;
    final static private int LIGHT_INDEX = 0;
    final static private int MOT_COUNT_INDEX = 1;
    final static private int ENERGY_INDEX = 2;
    final static private int SOUND_INDEX = 3;

    static private void maxInBin(double[][] data, long t,double value, final int idx, final long t0, final int numMinutesInWindow) {
        final int tIdx = (int)(t - t0) / 1000 / 60 / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            double v1 = data[idx][tIdx];
            double v2 = value;

            if (v1 < v2) {
                v1 = v2;
            }

            data[idx][tIdx] = v1;
        }

    }

    static private void addToBin(double[][] data, long t,double value, final int idx,final long t0,final int numMinutesInWindow) {
        final int tIdx = (int)(t - t0) / 1000 / 60 / numMinutesInWindow;

        if (tIdx >= 0 && tIdx < data[0].length) {
            data[idx][tIdx] += value;
        }
    }


    static public class BinnedData {
        double [][] data;
        long t0;
        int numMinutesInWindow;
    }
    static public void getSleepEventsFromHMM(final BinnedData data) {
        HiddenMarkovModel hmm = HiddenMarkovModel.createPoissonOnlyModel(load up model!);

        int [] path = hmm.getViterbiPath(data.data);

        //figure out sleep
    }

    static public Optional<BinnedData> getBinnedSensorData(AllSensorSampleList sensors, List<TrackerMotion> pillData, final int numMinutesInWindow) {
        List<Sample> light = sensors.get(Sensor.LIGHT);

        if (light == Collections.EMPTY_LIST || light.isEmpty()) {
            return Optional.absent();
        }

        //get start and end of window
        long t0 = light.get(0).dateTime;
        long tf = light.get(light.size()-1).dateTime;

        int dataLength =(int) (tf-t0) / 1000 / 60 / numMinutesInWindow;

        double [][] data = new double[NUM_DATA_DIMENSIONS][dataLength];

        //zero out data
        for (int i = 0; i < NUM_DATA_DIMENSIONS; i++) {
            Arrays.fill(data[i], 0.0);
        }

        //start filling in the sensor data.  Pick the max of the 5 minute bins for light
        //compute log of light
        Iterator<Sample> it1 = light.iterator();
        while (it1.hasNext()) {
            Sample sample = it1.next();
            double value = sample.value;
            if (value < 0) {
                value = 0.0;
            }

            //TODO transform this back to raw counts before taking log
            value = Math.log(value + 1.0);

            maxInBin(data,sample.dateTime,value,LIGHT_INDEX,t0,numMinutesInWindow);
        }

        //max of "energy"
        //add counts to bin
        Iterator<TrackerMotion> it2 = pillData.iterator();
        while (it2.hasNext()) {
            TrackerMotion m = it2.next();

            double value = m.value;

            if (value < 0) {
                value = 0;
            }

            value = Math.log(value/2000 + 1);

            addToBin(data,m.timestamp,1.0,MOT_COUNT_INDEX,t0,numMinutesInWindow);
            maxInBin(data,m.timestamp,value,ENERGY_INDEX,t0,numMinutesInWindow);

        }

        BinnedData res = new BinnedData();
        res.data = data;
        res.numMinutesInWindow = numMinutesInWindow;
        res.t0 = t0;

        return Optional.of(res);
    }

}
