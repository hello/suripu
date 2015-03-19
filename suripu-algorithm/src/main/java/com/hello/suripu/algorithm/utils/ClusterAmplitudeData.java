package com.hello.suripu.algorithm.utils;

import com.google.common.base.Objects;
import com.hello.suripu.algorithm.core.AmplitudeData;

/**
 * Created by pangwu on 3/19/15.
 */
public class ClusterAmplitudeData extends AmplitudeData {
    private boolean isInCluster;
    private ClusterAmplitudeData(long timestamp, double amplitude, int offsetMillis, final boolean isInCluster) {
        super(timestamp, amplitude, offsetMillis);
        this.isInCluster = isInCluster;
    }

    public static ClusterAmplitudeData create(final AmplitudeData data, final boolean isInCluster){
        return new ClusterAmplitudeData(data.timestamp, data.amplitude, data.offsetMillis, isInCluster);
    }

    public boolean isInCluster(){
        return this.isInCluster;
    }

    public void setInCluster(final boolean isInCluster){
        this.isInCluster = isInCluster;
    }

    @Override
    public boolean equals(final Object other){
        final ClusterAmplitudeData clusterAmplitudeData = (ClusterAmplitudeData)other;
        if(clusterAmplitudeData == null){
            return false;
        }
        return Objects.equal(this.timestamp, clusterAmplitudeData.timestamp) &&
                Objects.equal(this.amplitude, clusterAmplitudeData.amplitude) &&
                Objects.equal(this.offsetMillis, clusterAmplitudeData.offsetMillis) &&
                Objects.equal(this.isInCluster(), clusterAmplitudeData.isInCluster());
    }
}
