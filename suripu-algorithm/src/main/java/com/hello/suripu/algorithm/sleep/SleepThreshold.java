package com.hello.suripu.algorithm.sleep;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AmplitudeData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by pangwu on 4/21/14.
 */
public class SleepThreshold {

    private double amplitudeThreshold = 0.173;

    public SleepThreshold(double amplitudeThreshold){
        this.amplitudeThreshold = amplitudeThreshold;
    }

    public double getValue(){
        return this.amplitudeThreshold;
    }


    public static ImmutableList<SleepThreshold> generateEqualBinThresholds(final List<AmplitudeData> data, final int binNumber){
        AmplitudeData max = null;
        AmplitudeData min = null;
        for(final AmplitudeData datum:data){
            if(max == null){
                max = datum;
            }

            if(min == null){
                min = datum;
            }

            if(datum.amplitude > max.amplitude){
                max = datum;
            }

            if(datum.amplitude < min.amplitude){
                min = datum;
            }
        }

        if(max == null || min == null){
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        double diff = max.amplitude - min.amplitude;
        double step = diff / (double)binNumber;

        final ArrayList<SleepThreshold> thresholds = new ArrayList<SleepThreshold>();

        SleepThreshold threshold = new SleepThreshold(min.amplitude + step);
        while (threshold.getValue() < max.amplitude){
            thresholds.add(threshold);
            threshold = new SleepThreshold(threshold.getValue() + step);
        }

        thresholds.add(new SleepThreshold(max.amplitude));
        return ImmutableList.copyOf(thresholds);
    }

}
