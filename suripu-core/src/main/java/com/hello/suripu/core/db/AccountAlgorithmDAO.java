package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.AccountAlgorithm;
import org.joda.time.DateTime;

/**
 * Created by benjo on 11/17/15.
 */
public interface AccountAlgorithmDAO {

    Optional<AccountAlgorithm> getLatestAlgorithmForAccount(final Long accountId, final DateTime date);
}
