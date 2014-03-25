package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

public class AccountDAOImpl implements AccountDAO {
    @Override
    public Optional<Account> getById(final Long id) {
        if(id != 1) {
            return Optional.absent();
        }

        final Account account = new Account("tim@sayhello.com", id, TimeZone.getTimeZone("America/Los_Angeles"));
        return Optional.of(account);
    }
}
