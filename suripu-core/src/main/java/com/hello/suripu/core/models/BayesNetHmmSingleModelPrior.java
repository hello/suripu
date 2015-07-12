package com.hello.suripu.core.models;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.algorithm.bayes.BetaBinomialBayesModel;
import com.hello.suripu.api.datascience.BetaBinomialProtos;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 7/5/15.
 */
public class BayesNetHmmSingleModelPrior {

    public final String modelId;

    public final String outputId;

    public final List<BetaBinomialBayesModel> priors;

    public BayesNetHmmSingleModelPrior(String modelId, String outputId, List<BetaBinomialBayesModel> priors) {
        this.modelId = modelId;
        this.outputId = outputId;
        this.priors = priors;
    }

    public static List<BayesNetHmmSingleModelPrior> createListFromProtbuf(final byte [] serializedProtobuf) {

        final List<BayesNetHmmSingleModelPrior> priors = Lists.newArrayList();

        try {
            final BetaBinomialProtos.MultipleCondProbs everything = BetaBinomialProtos.MultipleCondProbs.parseFrom(serializedProtobuf);

            for (final BetaBinomialProtos.CondProbs condProbs : everything.getCondProbsList()) {
                final List<BetaBinomialBayesModel> probs = Lists.newArrayList();
                for (final BetaBinomialProtos.BetaCondProb prob : condProbs.getProbsList()) {
                    probs.add(new BetaBinomialBayesModel(prob.getAlpha(),prob.getBeta()));
                }

                priors.add(new BayesNetHmmSingleModelPrior(condProbs.getModelId(),condProbs.getOutputId(),probs));
            }

        }
        catch (InvalidProtocolBufferException exception) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        return priors;
    }


    public static byte [] listToProtobuf(final List<BayesNetHmmSingleModelPrior> priors) {

        final List<BetaBinomialProtos.CondProbs> condProbs = Lists.newArrayList();

        for (final BayesNetHmmSingleModelPrior prior : priors) {
            final List<BetaBinomialProtos.BetaCondProb> probs = Lists.newArrayList();
            for (final BetaBinomialBayesModel bdm : prior.priors) {
                probs.add(BetaBinomialProtos.BetaCondProb.newBuilder().setAlpha(bdm.getAlpha()).setBeta(bdm.getBeta()).build());
            }

            condProbs.add(BetaBinomialProtos.CondProbs.newBuilder().addAllProbs(probs).setModelId(prior.modelId).setOutputId(prior.outputId).build());
        }

        return BetaBinomialProtos.MultipleCondProbs.newBuilder().addAllCondProbs(condProbs).build().toByteArray();
    }

    /*
     * returns a map of MultipleCondProbs where the key is the model id
     */
    public static Map<String,byte []> getProtobufsByModelId(final List<BayesNetHmmSingleModelPrior> priors) {

        final Map<String,List<BetaBinomialProtos.CondProbs>> dataByModelId = Maps.newHashMap();
        final Map<String,byte[]> serializedDataByModelId = Maps.newHashMap();

        for (final BayesNetHmmSingleModelPrior prior : priors) {
            final List<BetaBinomialProtos.BetaCondProb> probs = Lists.newArrayList();
            for (final BetaBinomialBayesModel bdm : prior.priors) {
                probs.add(BetaBinomialProtos.BetaCondProb.newBuilder().setAlpha(bdm.getAlpha()).setBeta(bdm.getBeta()).build());
            }

            if (dataByModelId.get(prior.modelId) == null) {
                dataByModelId.put(prior.modelId,Lists.<BetaBinomialProtos.CondProbs>newArrayList());
            }

            dataByModelId.get(prior.modelId).add(BetaBinomialProtos.CondProbs.newBuilder().addAllProbs(probs).setModelId(prior.modelId).setOutputId(prior.outputId).build());
        }

        //now go through and serialize to "MultipleCondProbs"
        for (final String key : dataByModelId.keySet()) {
            serializedDataByModelId.put(key, BetaBinomialProtos.MultipleCondProbs.newBuilder().addAllCondProbs(dataByModelId.get(key)).build().toByteArray());
        }

        return serializedDataByModelId;
    }
}
