package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.NeuralNetDAO;
import com.hello.suripu.core.models.timeline.v2.TimelineLog;

import java.util.UUID;

/**
 * Created by benjo on 2/23/16.
 */
public class NeuralNetAlgorithm implements TimelineAlgorithm {
    private final NeuralNetDAO neuralNetDAO;

    public NeuralNetAlgorithm(final NeuralNetDAO neuralNetDAO) {
        this.neuralNetDAO = neuralNetDAO;
    }

    @Override
    public Optional<TimelineAlgorithmResult> getTimelinePrediction(final OneDaysSensorData sensorData,final TimelineLog log,final long accountId,final boolean feedbackChanged) {
        return Optional.absent();
    }

    @Override
    public TimelineAlgorithm cloneWithNewUUID(Optional<UUID> uuid) {
        return new NeuralNetAlgorithm(neuralNetDAO);
    }
}
