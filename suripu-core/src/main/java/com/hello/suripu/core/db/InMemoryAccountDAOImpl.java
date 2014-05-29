package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAccountDAOImpl implements AccountDAO{
    final AtomicLong currentId = new AtomicLong();
    final Map<Long, Account> store = new HashMap<Long, Account>();
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAccountDAOImpl.class);


    public Optional<Account> getById(final Long id) {
        if(!store.containsKey(id)) {
            return Optional.absent();
        }


        final Account account = store.get(id);
        return Optional.of(account);
    }

    @Override
    public Optional<Account> getByEmail(String email) {
        return null;
    }


    public Account register(final Registration registration) {
        long id = currentId.incrementAndGet();
        final Account account = Account.fromRegistration(registration, id);
        LOGGER.debug("{}", account);
        store.put(id, account);
        return account;
    }


    public Optional<Account> exists(final String email, final String password) {
        for(Map.Entry<Long, Account> entry : store.entrySet()) {
            if (entry.getValue().email.equals(email) && entry.getValue().passwordHash.equals(password)) {
                return Optional.of(entry.getValue());
            }
        }

        LOGGER.warn("No user found for email = {} and password = {}", email, password);
        return Optional.absent();
    }
}
