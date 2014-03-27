package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.Account;
import com.hello.suripu.core.Registration;
import org.skife.jdbi.v2.sqlobject.*;

public abstract class AccountDAOImpl implements AccountDAO {

    @SqlQuery("SELECT * FROM accounts WHERE id = :id LIMIT 1;")
    public abstract Optional<Account> getById(@Bind("id") final Long id);

    @SqlQuery("SELECT * FROM accounts WHERE email = :email LIMIT 1;")
    public abstract Optional<Account> getByEmail(@Bind("email") final String email);

    @SqlUpdate("INSERT INTO accounts (firstname, lastname, email, password_hash, age, height, weight, tz, created) VALUES(:firstname, :lastname, :email, :password, :age, :height, :weight, :tz, :created)")
    @GetGeneratedKeys
    public abstract long insertAccount(@BindRegistration Registration registration);

    public Account register(final Registration registration) {
        long id = insertAccount(registration);
        return Account.fromRegistration(registration, id);
    }

    public Optional<Account> exists(final String email, final String password) {
        final Optional<Account> accountOptional = getByEmail(email);
        if(!accountOptional.isPresent()) {
            return Optional.absent();
        }



        // TODO: hash password and verify hash instead of real password
        final String passwordFromDB = accountOptional.get().password;
        if(!passwordFromDB.equals(password)) {
            // TODO: Add metrics here
            return Optional.absent();
        }
        return accountOptional;
    }
}
