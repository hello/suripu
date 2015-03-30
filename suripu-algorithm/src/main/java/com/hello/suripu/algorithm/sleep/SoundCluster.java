package com.hello.suripu.algorithm.sleep;

import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.utils.DataUtils;
import com.hello.suripu.algorithm.utils.NumericalUtils;
import com.hello.suripu.algorithm.utils.SoundFeatures;
import org.joda.time.DateTimeConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pangwu on 3/30/15.
 */
public class SoundCluster {
    public static List<Segment> getClusters(final List<AmplitudeData> rawSound){
        final List<AmplitudeData> alignedSound = DataUtils.fillMissingValues(DataUtils.dedupe(rawSound), DateTimeConstants.MILLIS_PER_MINUTE);
        final List<AmplitudeData> densityFeature = SoundFeatures.getDensityFeature(alignedSound, 20);
        final double densityMean = NumericalUtils.mean(densityFeature);
        final List<Segment> result = new ArrayList<>();

        long startMillis = 0;
        long endMillis = 0;
        int offsetMillis = 0;

        for(final AmplitudeData density:densityFeature){
            if(density.amplitude > densityMean){
                if(startMillis == 0){
                    startMillis = density.timestamp;
                }
                endMillis = density.timestamp;
                offsetMillis = density.offsetMillis;
            }else{
                if(startMillis > 0){
                    result.add(new Segment(startMillis, endMillis, offsetMillis));
                }
                startMillis = 0;
                endMillis = 0;
            }
        }
        if(startMillis > 0) {
            result.add(new Segment(startMillis, endMillis, offsetMillis));
        }

        return result;
    }
}
