package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.BayesNetHmmMultipleModelsPriors;
import com.hello.suripu.core.models.BayesNetHmmSingleModelPrior;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by benjo on 7/5/15.
 */
public interface BayesNetHmmModelPriorsDAO {

    public static final String CURRENT_RANGE_KEY = "current";

    public Optional<BayesNetHmmMultipleModelsPriors> getModelPriorsByAccountIdAndDate(final Long accountId,final DateTime dateLocalUTC);

    public boolean updateModelPriorsByAccountIdForDate(final Long accountId,final DateTime dateLocalUtc, final List<BayesNetHmmSingleModelPrior> priors);
}
