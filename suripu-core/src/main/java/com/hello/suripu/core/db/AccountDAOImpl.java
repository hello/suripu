package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.Registration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class AccountDAOImpl implements AccountDAO {
    final AtomicLong currentId = new AtomicLong();
    final Map<Long, Account> store = new HashMap<Long, Account>();

    @Override
    public Optional<Account> getById(final Long id) {
        if(!store.containsKey(id)) {
            return Optional.absent();
        }


        final Account account = store.get(id);
        return Optional.of(account);
    }

    @Override
    public Account register(final Registration registration) {
        long id = currentId.incrementAndGet();
        final Account account = Account.fromRegistration(registration,id);
        store.put(id, account);
        return account;
    }
}
