package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;

import java.util.List;
import java.util.UUID;

public interface AccountReadDAO {
    Optional<Account> getById(final Long id);
    Optional<Account> getByExternalId(final UUID externalId);
    Optional<Account> getByEmail(final String email);
    List<Account> getRecent(final Integer limit);
    Optional<Account> exists(final String email, final String password);
    List<Account> getByNamePartial(String namePartial);
    List<Account> getByEmailPartial(String emailPartial);
}
