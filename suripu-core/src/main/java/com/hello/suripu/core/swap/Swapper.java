package com.hello.suripu.core.swap;

import com.google.common.base.Optional;

public interface Swapper {
    SwapResult swap(SwapIntent intent);
    void create(SwapIntent intent);
    Optional<SwapIntent> query(String senseId);
    Optional<SwapIntent> eligible(Long accountId, String senseId);
}
