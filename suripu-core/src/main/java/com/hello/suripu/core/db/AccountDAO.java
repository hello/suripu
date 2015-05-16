package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;

import java.util.List;

public interface AccountDAO {

    Optional<Account> getById(final Long id);
    Optional<Account> getByEmail(final String email);
    List<Account> getRecent(final Integer limit);
    Account register(Registration registration);
    Optional<Account> exists(final String email, final String password);
    Optional<Account> update(final Account account, Long accountId);
    Boolean updatePassword(final Long accountId, final PasswordUpdate passwordUpdate);
    Boolean updatePasswordFromResetEmail(final Long accountId, final String encryptedPassword, final String state);
    Optional<Account> updateEmail(final Account account);
    void delete(final String email);
    List<Account> getByNamePartial(String namePartial);
    List<Account> getByEmailPartial(String emailPartial);
}
