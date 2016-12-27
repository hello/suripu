package com.hello.suripu.core.algorithmintegration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.core.db.DefaultModelEnsembleDAO;
import com.hello.suripu.core.db.FeatureExtractionModelsDAO;
import com.hello.suripu.core.db.OnlineHmmModelsDAO;
import com.hello.suripu.core.db.SleepHmmDAO;
import com.hello.suripu.core.util.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Created by benjo on 1/20/16.
 *
 * not really a factory, because it doesn't return a clone.  It's just a map for now.
 */
public class AlgorithmFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlgorithmFactory.class);


    private final ImmutableMap<AlgorithmType,TimelineAlgorithm> algorithmMap;

    public static AlgorithmFactory create(final SleepHmmDAO sleepHmmDAO, final OnlineHmmModelsDAO priorsDAO, final DefaultModelEnsembleDAO defaultModelEnsembleDAO, final FeatureExtractionModelsDAO featureExtractionModelsDAO, final Map<AlgorithmType,NeuralNetEndpoint> neuralNetEndpoints,final AlgorithmConfiguration algConfig, final Optional<UUID> uuid) {
        final Map<AlgorithmType,TimelineAlgorithm> algorithmMap = Maps.newHashMap();

        LOGGER.info("action=creating-timeline-algorithm type={}",AlgorithmType.VOTING);
        algorithmMap.put(AlgorithmType.VOTING,new VotingAlgorithm(uuid));

        LOGGER.info("action=creating-timeline-algorithm type={}",AlgorithmType.ONLINE_HMM);
        algorithmMap.put(AlgorithmType.ONLINE_HMM,new OnlineHmmAlgorithm(priorsDAO,defaultModelEnsembleDAO,featureExtractionModelsDAO,uuid));

        LOGGER.info("action=creating-timeline-algorithm type={}",AlgorithmType.HMM);
        algorithmMap.put(AlgorithmType.HMM,new YeOldeHmmAlgorithm(sleepHmmDAO,uuid));

        if (neuralNetEndpoints.containsKey(AlgorithmType.NEURAL_NET)) {
            LOGGER.info("action=creating-timeline-algorithm type={}",AlgorithmType.NEURAL_NET);
            algorithmMap.put(AlgorithmType.NEURAL_NET, new NeuralNetAlgorithm(neuralNetEndpoints.get(AlgorithmType.NEURAL_NET),algConfig));
        }

        if (neuralNetEndpoints.containsKey(AlgorithmType.NEURAL_NET_FOUR_EVENT)) {
            LOGGER.info("action=creating-timeline-algorithm type={}",AlgorithmType.NEURAL_NET_FOUR_EVENT);
            algorithmMap.put(AlgorithmType.NEURAL_NET_FOUR_EVENT, new NeuralNetFourEventAlgorithm(neuralNetEndpoints.get(AlgorithmType.NEURAL_NET_FOUR_EVENT),algConfig));
        }

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

        final TimelineAlgorithm algorithm = algorithmMap.get(alg);

        if (algorithm == null) {
            LOGGER.warn("action=get-algorithm-failed algorithm={}",alg.name());
            return Optional.absent();
        }

        return Optional.of(algorithm);
    }



}
