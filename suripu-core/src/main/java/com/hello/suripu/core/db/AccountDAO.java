package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Registration;

public interface AccountDAO {

    Optional<Account> getById(final Long id);
    Optional<Account> getByEmail(final String email);
    Account register(Registration registration);
    Optional<Account> exists(final String email, final String password);
    boolean update(final Account account, Long accountId);

}
