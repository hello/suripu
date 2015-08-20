package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.datascience.OnlineHmmProtos.*;

import java.util.Map;

/**
 * Created by benjo on 8/19/15.
 */
public class OnlineHmmScratchPad {

    public OnlineHmmScratchPad(Map<String, OnlineHmmModelParams> paramsByOutputId, long lastUpdateTimeUtc, long createdTimeUtc) {
        this.paramsByOutputId = paramsByOutputId;
        this.lastUpdateTimeUtc = lastUpdateTimeUtc;
        this.createdTimeUtc = createdTimeUtc;
    }

    public final Map<String,OnlineHmmModelParams> paramsByOutputId;
    public final long lastUpdateTimeUtc;
    public final long createdTimeUtc;

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
            if (protobuf.hasLastUpdateTimeUtc()) {
                lastUpdateTimeUtc = protobuf.getLastUpdateTimeUtc();
            }

            long createdTimeUtc = 0;
            if (protobuf.hasDateCreated()) {
                createdTimeUtc = protobuf.getDateCreated();
            }


            return Optional.of(new OnlineHmmScratchPad(paramsByOutputId,lastUpdateTimeUtc,createdTimeUtc));


        } catch (InvalidProtocolBufferException e) {
            //TODO log this as error
            return Optional.absent();
        }
    }

    public byte [] serializeToProtobuf() {
        return  null;
    }

}
