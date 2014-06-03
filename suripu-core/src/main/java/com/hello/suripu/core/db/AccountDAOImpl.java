package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Registration;
import com.hello.suripu.core.db.binders.BindRegistration;
import com.hello.suripu.core.db.mappers.AccountMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.skife.jdbi.v2.sqlobject.*;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@RegisterMapper(AccountMapper.class)
public abstract class AccountDAOImpl implements AccountDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDAOImpl.class);

    @SqlQuery("SELECT * FROM accounts WHERE id = :id LIMIT 1;")
    @SingleValueResult(Account.class)
    public abstract Optional<Account> getById(@Bind("id") final Long id);

    @SqlQuery("SELECT * FROM accounts WHERE email = :email LIMIT 1;")
    @SingleValueResult(Account.class)
    public abstract Optional<Account> getByEmail(@Bind("email") final String email);

    @SqlUpdate("INSERT INTO accounts (firstname, lastname, email, password_hash, age, height, weight, tz, created) VALUES(:firstname, :lastname, :email, :password, :age, :height, :weight, :tz, :created)")
    @GetGeneratedKeys
    public abstract long insertAccount(@BindRegistration Registration registration);


    public Account register(final Registration registration) {
        long id = insertAccount(registration);
        return Account.fromRegistration(registration, id);
    }

    public Optional<Account> exists(final String email, final String password) {

        checkNotNull(email, "Email can not be null");
        checkNotNull(password, "Password can not be null");

        if(password.length() == 0) {
            LOGGER.warn("Password should never be empty.");
            return Optional.absent();
        }

        LOGGER.debug("Checking if account exists for email = {} with password length = {}", email, password.length());

        // TODO : check why we can get null return value here
        final Optional<Account> accountOptional = getByEmail(email);

        if(accountOptional == null) {
            LOGGER.warn("!!! Account optional should never be NULL. Please investigate this issue !!!");
            return Optional.absent();
        }

        if(!accountOptional.isPresent()) {
            LOGGER.warn("Account wasn't found for email = {} and password = {}...", email, password.substring(0, 1));
            return Optional.absent();
        }


        final String passwordFromDB = accountOptional.get().passwordHash;
        if(!BCrypt.checkpw(password, passwordFromDB)) {
            LOGGER.warn("Passwords don't match");
            // TODO: Add metrics here
            return Optional.absent();
        }
        return accountOptional;
    }
}
