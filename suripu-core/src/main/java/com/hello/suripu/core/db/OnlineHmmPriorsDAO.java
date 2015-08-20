package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;

/**
 * Created by benjo on 8/18/15.
 */
public interface OnlineHmmPriorsDAO {
    OnlineHmmData getModelDataByAccountId(final Long accountId);
    boolean updateModelPriors(final Long accountId, final OnlineHmmPriors priors);
    boolean updateScratchpad(final Long accountId, final OnlineHmmScratchPad scratchPad);

}
