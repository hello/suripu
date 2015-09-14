package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.OnlineHmmData;
import com.hello.suripu.core.models.OnlineHmmPriors;
import com.hello.suripu.core.models.OnlineHmmScratchPad;
import org.joda.time.DateTime;

/**
 * Created by benjo on 8/18/15.
 */
public interface OnlineHmmModelsDAO {
    OnlineHmmData getModelDataByAccountId(final Long accountId,final DateTime date);
    boolean updateModelPriors(final Long accountId,final DateTime date, final OnlineHmmPriors priors);
    boolean updateModelPriorsAndZeroOutScratchpad(final Long accountId,final DateTime date, final OnlineHmmPriors priors);
    boolean updateScratchpad(final Long accountId,final DateTime date, final OnlineHmmScratchPad scratchPad);

}
