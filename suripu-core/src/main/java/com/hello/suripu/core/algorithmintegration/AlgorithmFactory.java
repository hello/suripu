package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.core.configuration.NeuralNetServiceClientConfiguration;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.util.AlgorithmType;

import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 1/20/16.
 *
 * not really a factory, because it doesn't return a clone.  It's just a map for now.
 */
public class AlgorithmFactory {

    private final ImmutableMap<AlgorithmType,TimelineAlgorithm> algorithmMap;

    public static AlgorithmFactory create(final SleepHmmDAO sleepHmmDAO, final OnlineHmmModelsDAO priorsDAO, final DefaultModelEnsembleDAO defaultModelEnsembleDAO, final FeatureExtractionModelsDAO featureExtractionModelsDAO, final NeuralNetServiceClientConfiguration neuralNetDAO, final Optional<UUID> uuid) {
        final Map<AlgorithmType,TimelineAlgorithm> algorithmMap = Maps.newHashMap();

        algorithmMap.put(AlgorithmType.VOTING,new VotingAlgorithm(uuid));
        algorithmMap.put(AlgorithmType.ONLINE_HMM,new OnlineHmmAlgorithm(priorsDAO,defaultModelEnsembleDAO,featureExtractionModelsDAO,uuid));
        algorithmMap.put(AlgorithmType.HMM,new YeOldeHmmAlgorithm(sleepHmmDAO,uuid));
        algorithmMap.put(AlgorithmType.NEURAL_NET, new NeuralNetAlgorithm(neuralNetDAO));
        return new AlgorithmFactory(algorithmMap);
    }


    private AlgorithmFactory(final Map<AlgorithmType,TimelineAlgorithm> algorithmMap) {
        this.algorithmMap = ImmutableMap.copyOf(algorithmMap);
    }

    public AlgorithmFactory cloneWithNewUUID(final Optional<UUID> uuid) {
        final Map<AlgorithmType,TimelineAlgorithm> clonedMap = Maps.newHashMap();

        for (final Map.Entry<AlgorithmType,TimelineAlgorithm> entry : algorithmMap.entrySet()) {
            clonedMap.put(entry.getKey(),entry.getValue().cloneWithNewUUID(uuid));
        }

        return new AlgorithmFactory(clonedMap);

    }

    public Optional<TimelineAlgorithm> get(final AlgorithmType alg) {
        return Optional.fromNullable(algorithmMap.get(alg));
    }



}
