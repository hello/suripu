package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.datascience.OnlineHmmProtos;
import com.hello.suripu.api.datascience.OnlineHmmProtos.*;

import java.util.Map;

/**
 * Created by benjo on 8/19/15.
 */
public class OnlineHmmScratchPad {

    public OnlineHmmScratchPad(Map<String, OnlineHmmModelParams> paramsByOutputId, long lastUpdateTimeUtc) {
        this.paramsByOutputId = paramsByOutputId;
        this.lastUpdateTimeUtc = lastUpdateTimeUtc;
    }

    public final Map<String,OnlineHmmModelParams> paramsByOutputId;
    public final long lastUpdateTimeUtc;

    public boolean isEmpty() {
        return paramsByOutputId.isEmpty();
    }

    public static OnlineHmmScratchPad createEmpty() {
        return new OnlineHmmScratchPad(Maps.<String, OnlineHmmModelParams>newHashMap(),0);
    }

    public static Optional<OnlineHmmScratchPad> createFromProtobuf(final byte [] data) {

        try {
            final AlphabetHmmScratchPad protobuf = AlphabetHmmScratchPad.parseFrom(data);
            final Map<String,OnlineHmmModelParams> paramsByOutputId = Maps.newHashMap();

            for (final AlphabetHmmPrior modelDelta : protobuf.getModelDeltasList()) {

                String outputId = "unknown";
                switch (modelDelta.getOutputId()) {

                    case SLEEP:
                        outputId = OnlineHmmData.OUTPUT_MODEL_SLEEP;
                        break;

                    case BED:
                        outputId = OnlineHmmData.OUTPUT_MODEL_BED;
                        break;
                }

                final Optional<OnlineHmmModelParams> modelParamsOptional = OnlineHmmPriors.protobufToParams(modelDelta);

                if (!modelParamsOptional.isPresent()) {
                    //TODO log error
                    continue;
                }

                paramsByOutputId.put(outputId,modelParamsOptional.get());
            }

            long lastUpdateTimeUtc = 0;
            if (protobuf.hasLastDateUpdatedUtc()) {
                lastUpdateTimeUtc = protobuf.getLastDateUpdatedUtc();
            }


            return Optional.of(new OnlineHmmScratchPad(paramsByOutputId,lastUpdateTimeUtc));


        } catch (InvalidProtocolBufferException e) {
            //TODO log this as error
            return Optional.absent();
        }
    }

    public byte [] serializeToProtobuf() {

        final AlphabetHmmScratchPad.Builder builder = AlphabetHmmScratchPad.newBuilder();

        for (final String key : paramsByOutputId.keySet()) {
            final AlphabetHmmPrior protobuf = OnlineHmmPriors.protobufFromParams(paramsByOutputId.get(key));
            builder.addModelDeltas(protobuf);
        }

        builder.setLastDateUpdatedUtc(lastUpdateTimeUtc);


        return  builder.build().toByteArray();
    }

}
