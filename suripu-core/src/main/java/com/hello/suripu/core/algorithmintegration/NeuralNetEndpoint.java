package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;

/**
 * Created by benjo on 3/22/16.
 */
public interface NeuralNetEndpoint {
     Optional<NeuralNetAlgorithmOutput> getNetOutput(final String netId, final double [][] sensorData);
}
