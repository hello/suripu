package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAccountDAOImpl implements AccountDAO{
    final AtomicLong currentId = new AtomicLong();
    final Map<Long, Account> store = Maps.newHashMap();
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryAccountDAOImpl.class);


    public Optional<Account> getById(final Long id) {
        if(!store.containsKey(id)) {
            return Optional.absent();
        }


        final Account account = store.get(id);
        return Optional.of(account);
    }

    @Override
    public Optional<Account> getByExternalId(UUID externalId) {
        return Optional.absent();
    }

    @Override
    public Optional<Account> getByEmail(String email) {
        return null;
    }

    @Override
    public List<Account> getRecent(final Integer limit) {
        return Collections.emptyList();
    }


    public Account register(final Registration registration) {
        long id = currentId.incrementAndGet();
        final UUID externalId = UUID.randomUUID();
        final Account account = Account.fromRegistration(registration, id, externalId);
        LOGGER.debug("{}", account);
        store.put(id, account);
        return account;
    }


    public Optional<Account> exists(final String email, final String password) {
        for(Map.Entry<Long, Account> entry : store.entrySet()) {
            if (entry.getValue().email.equals(email) && entry.getValue().password.equals(password)) {
                return Optional.of(entry.getValue());
            }
        }

        LOGGER.warn("No user found for email = {} and password = {}", email, password);
        return Optional.absent();
    }

    @Override
    public Optional<Account> update(Account account, Long accountId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Boolean updatePassword(Long accountId, PasswordUpdate passwordUpdate) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean updatePasswordFromResetEmail(Long accountId, String encryptedPassword, final String state) {
        return Boolean.FALSE;
    }

    @Override
    public Optional<Account> updateEmail(Account account) {
        return Optional.absent();
    }

    @Override
    public void delete(String email) {

    }

    @Override
    public List<Account> getByNamePartial(String namePartial) {return Lists.newArrayList();}

    @Override
    public List<Account> getByEmailPartial(String emailPartial) {return Lists.newArrayList();}

}
