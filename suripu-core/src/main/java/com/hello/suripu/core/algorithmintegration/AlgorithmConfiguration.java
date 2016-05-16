package com.hello.suripu.core.algorithmintegration;

/**
 * Created by benjo on 5/16/16.
 */
public interface AlgorithmConfiguration {

    //if time > stop_minute && time < start_minute, then light is set to zero
    int getArtificalLightStartMinuteOfDay();
    int getArtificalLightStopMinuteOfDay();
}
