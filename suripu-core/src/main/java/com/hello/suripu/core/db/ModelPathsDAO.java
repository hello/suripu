package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.BayesNetHmmModelResult;
import org.joda.time.DateTime;

/**
 * Created by benjo on 7/5/15.
 */
public interface ModelPathsDAO {
    Optional<BayesNetHmmModelResult> getResultByAccountAndDay(Long accountId,DateTime dateLocalUTCMidnight);

    boolean setResultByAccountAndDay(Long accountId, DateTime dateLocalUTCMidnight, final BayesNetHmmModelResult modelResult);
}
