package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.Registration;

public interface AccountDAO {
    Optional<Account> getById(Long id);
    Account register(Registration registration);
}
