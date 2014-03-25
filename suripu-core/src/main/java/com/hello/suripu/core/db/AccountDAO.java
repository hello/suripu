package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;

public interface AccountDAO {
    Optional<Account> getById(Long id);
}
