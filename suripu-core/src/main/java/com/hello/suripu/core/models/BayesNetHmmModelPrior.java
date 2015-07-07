package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hello.suripu.api.datascience.BetaBinomialProtos;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by benjo on 7/5/15.
 */
public class BayesNetHmmModelPrior {

    public final String modelId;

    public final String outputId;

    public final List<BetaDistributionModel> priors;

    public BayesNetHmmModelPrior(String modelId, String outputId, List<BetaDistributionModel> priors) {
        this.modelId = modelId;
        this.outputId = outputId;
        this.priors = priors;
    }

    public void increment(final double alpha, final double beta, int state) {
        priors.get(state).alpha += alpha;
        priors.get(state).alpha += beta;
    }


    public static List<BayesNetHmmModelPrior> createListFromProtbuf(final byte [] serializedProtobuf) {

        final List<BayesNetHmmModelPrior> priors = Lists.newArrayList();

        try {
            final BetaBinomialProtos.MultipleCondProbs everything = BetaBinomialProtos.MultipleCondProbs.parseFrom(serializedProtobuf);

            for (final BetaBinomialProtos.CondProbs condProbs : everything.getCondProbsList()) {
                final List<BetaDistributionModel> probs = Lists.newArrayList();
                for (final BetaBinomialProtos.BetaCondProb prob : condProbs.getProbsList()) {
                    probs.add(new BetaDistributionModel(prob.getAlpha(),prob.getBeta()));
                }

                priors.add(new BayesNetHmmModelPrior(condProbs.getModelId(),condProbs.getOutputId(),probs));
            }

        }
        catch (InvalidProtocolBufferException exception) {
            return ImmutableList.copyOf(Collections.EMPTY_LIST);
        }

        return priors;
    }


    public static byte [] listToProtobuf(final List<BayesNetHmmModelPrior> priors) {

        final List<BetaBinomialProtos.CondProbs> condProbs = Lists.newArrayList();

        for (final BayesNetHmmModelPrior prior : priors) {
            final List<BetaBinomialProtos.BetaCondProb> probs = Lists.newArrayList();
            for (final BetaDistributionModel bdm : prior.priors) {
                probs.add(BetaBinomialProtos.BetaCondProb.newBuilder().setAlpha(bdm.alpha).setBeta(bdm.beta).build());
            }

            condProbs.add(BetaBinomialProtos.CondProbs.newBuilder().addAllProbs(probs).setModelId(prior.modelId).setOutputId(prior.outputId).build());
        }

        return BetaBinomialProtos.MultipleCondProbs.newBuilder().addAllCondProbs(condProbs).build().toByteArray();
    }

    /*
     * returns a map of MultipleCondProbs where the key is the model id
     */
    public static Map<String,byte []> getProtobufsByModelId(final List<BayesNetHmmModelPrior> priors) {

        final Map<String,List<BetaBinomialProtos.CondProbs>> dataByModelId = Maps.newHashMap();
        final Map<String,byte[]> serializedDataByModelId = Maps.newHashMap();

        for (final BayesNetHmmModelPrior prior : priors) {
            final List<BetaBinomialProtos.BetaCondProb> probs = Lists.newArrayList();
            for (final BetaDistributionModel bdm : prior.priors) {
                probs.add(BetaBinomialProtos.BetaCondProb.newBuilder().setAlpha(bdm.alpha).setBeta(bdm.beta).build());
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
