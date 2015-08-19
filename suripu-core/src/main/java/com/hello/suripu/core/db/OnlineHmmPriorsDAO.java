package com.hello.suripu.core.db;

import com.hello.suripu.core.models.OnlineHmmPriors;

/**
 * Created by benjo on 8/18/15.
 */
public interface OnlineHmmPriorsDAO {
    void getModelPriorsByAccountId(final Long accountId);
    void updateModelPriors(final Long accountId, final OnlineHmmPriors priors);
}
