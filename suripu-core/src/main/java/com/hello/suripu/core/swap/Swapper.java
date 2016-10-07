package com.hello.suripu.core.swap;

import com.google.common.base.Optional;

public interface Swapper {
    Result swap(Intent intent);
    void create(Intent intent);
    Optional<Intent> query(String senseId);
    Optional<Intent> query(String senseId, int minutesAgo);
    IntentResult eligible(Long accountId, String senseId);
}
