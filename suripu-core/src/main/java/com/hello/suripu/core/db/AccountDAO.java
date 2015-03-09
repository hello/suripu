package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.util.List;

public interface AccountDAO {

    Optional<Account> getById(final Long id);
    Optional<Account> getByEmail(final String email);
    List<Account> getRecent();
    Account register(Registration registration);
    Optional<Account> exists(final String email, final String password);
    Optional<Account> update(final Account account, Long accountId);
    Boolean updatePassword(final Long accountId, final PasswordUpdate passwordUpdate);
    Boolean updatePasswordFromResetEmail(final Long accountId, final String encryptedPassword, final String state);
    Optional<Account> updateEmail(final Account account);
    void delete(final String email);

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM accounts WHERE name ILIKE '%'||:name||'%' ORDER BY id DESC LIMIT 50;")
    List<Account> getAccountsByNameHint(
            @Bind("name") final String name
    );

    @RegisterMapper(AccountMapper.class)
    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM accounts WHERE email ILIKE '%'||:email||'%' ORDER BY id DESC LIMIT 50;")
    List<Account> getAccountsByEmailHint(
            @Bind("email") final String email
    );
}
