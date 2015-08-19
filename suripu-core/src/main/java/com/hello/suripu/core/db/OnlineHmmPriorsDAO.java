package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.OnlineHmmPriors;

/**
 * Created by benjo on 8/18/15.
 */
public interface OnlineHmmPriorsDAO {
    Optional<OnlineHmmPriors> getModelPriorsByAccountId(final Long accountId);
    boolean updateModelPriors(final Long accountId, final OnlineHmmPriors priors);
}
