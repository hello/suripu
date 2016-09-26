package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import org.joda.time.DateTime;

public interface PairingDAO {
    Optional<String> senseId(long accountId, DateTime start, DateTime stop);
    Optional<String> pillId(long accountId, DateTime start, DateTime stop);
}
