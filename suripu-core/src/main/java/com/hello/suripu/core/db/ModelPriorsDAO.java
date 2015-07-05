package com.hello.suripu.core.db;

import com.hello.suripu.core.models.BayesNetHmmModelPrior;

import java.util.List;

/**
 * Created by benjo on 7/5/15.
 */
public interface ModelPriorsDAO {

    public List<BayesNetHmmModelPrior> getModelPriorsByAccountId(Long accountId, List<String> modelNames);

    public boolean updateModelPriorsByAccountId(Long accountId, List<BayesNetHmmModelPrior> priors);
}
