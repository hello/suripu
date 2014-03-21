package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;

public class AccountDAOImpl implements AccountDAO {
    @Override
    public Optional<Account> getById(Long id) {
        if(id != 1) {
            return Optional.absent();
        }

        final Account account = new Account("tim@sayhello.com");
        return Optional.of(account);
    }
}
