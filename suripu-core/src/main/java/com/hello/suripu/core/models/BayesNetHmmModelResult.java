package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.datascience.BetaBinomialProtos;

import java.util.List;

/**
 * Created by benjo on 7/5/15.
 */
public class BayesNetHmmModelResult {
    public final ImmutableList<BayesNetHmmModelPath> paths;

    public final Long startTimeUtc;

    public final Integer numMinutesPerPeriod;


    public BayesNetHmmModelResult(ImmutableList<BayesNetHmmModelPath> paths, Long startTimeUtc, Integer numMinutesPerPeriod) {
        this.paths = paths;
        this.startTimeUtc = startTimeUtc;
        this.numMinutesPerPeriod = numMinutesPerPeriod;
    }

    public byte [] toProtobuf() {
        //get paths
        final List<BetaBinomialProtos.ModelPath> modelPaths = Lists.newArrayList();
        for (final BayesNetHmmModelPath path : paths) {
            BetaBinomialProtos.ModelPath modelPath = BetaBinomialProtos.ModelPath.newBuilder()
                    .setModelId(path.modelId)
                    .setOutputId(path.outputId)
                    .addAllPath(path.path)
                    .build();

            modelPaths.add(modelPath);
        }

        return  BetaBinomialProtos.ModelResult.newBuilder()
                .addAllPaths(modelPaths)
                .setMeasPeriodMinutes(numMinutesPerPeriod)
                .setStartTimeUtc(startTimeUtc)
                .build().toByteArray();

    }



    public static Optional<BayesNetHmmModelResult> createFromProtobuf(final byte [] serializedProtobuf) {

        try {
            final BetaBinomialProtos.ModelResult modelResult = BetaBinomialProtos.ModelResult.parseFrom(serializedProtobuf);

            final List<BayesNetHmmModelPath> paths = Lists.newArrayList();

            for (BetaBinomialProtos.ModelPath path : modelResult.getPathsList()) {
                paths.add(new BayesNetHmmModelPath(ImmutableList.copyOf(path.getPathList()),path.getModelId(),path.getOutputId()));
            }

            return Optional.of(new BayesNetHmmModelResult(ImmutableList.copyOf(paths),modelResult.getStartTimeUtc(),modelResult.getMeasPeriodMinutes()));

        }
        catch (InvalidProtocolBufferException exception) {
            return Optional.absent();
        }
    }


}
