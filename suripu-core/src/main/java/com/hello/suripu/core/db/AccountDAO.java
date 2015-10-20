package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;

import java.util.List;

public interface AccountDAO extends AccountReadDAO {
    Account register(Registration registration);
    Optional<Account> update(final Account account, Long accountId);
    Boolean updatePassword(final Long accountId, final PasswordUpdate passwordUpdate);
    Boolean updatePasswordFromResetEmail(final Long accountId, final String encryptedPassword, final String state);
    Optional<Account> updateEmail(final Account account);
    void delete(final String email);
}
