package com.hello.suripu.core.db;

import com.hello.suripu.core.models.BayesNetHmmModelPrior;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by benjo on 7/5/15.
 */
public interface ModelPriorsDAO {

    public List<BayesNetHmmModelPrior> getModelPriorsByAccountIdAndDate(final Long accountId,final DateTime dateLocalUTC);

    public boolean updateModelPriorsByAccountIdForDate(final Long accountId,final DateTime dateLocalUtc, final List<BayesNetHmmModelPrior> priors);
}
