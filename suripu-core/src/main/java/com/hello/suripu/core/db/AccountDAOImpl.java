package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.binders.BindAccount;
import com.hello.suripu.core.db.binders.BindRegistration;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Registration;
import org.mindrot.jbcrypt.BCrypt;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

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

    @SqlUpdate("INSERT INTO accounts (name, email, password_hash, dob, height, weight, tz_offset, created) VALUES(:name, :email, :password, :dob, :height, :weight, :tz_offset, :created)")
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
            LOGGER.warn("exists: Password should never be empty.");
            return Optional.absent();
        }

        LOGGER.debug("exists: Checking if account exists for email = {} with password length = {}", email, password.length());

        // TODO : check why we can get null return value here
        final Optional<Account> accountOptional = getByEmail(email);

        if(accountOptional == null) {
            LOGGER.warn("exists: !!! Account optional should never be NULL. Please investigate this issue !!!");
            return Optional.absent();
        }

        if(!accountOptional.isPresent()) {
            LOGGER.warn("exists: Account wasn't found for email = {} and password = {}...", email, password.substring(0, 1));
            return Optional.absent();
        }


        final String passwordFromDB = accountOptional.get().password;
        if(!BCrypt.checkpw(password, passwordFromDB)) {
            LOGGER.warn("exists: Passwords don't match");
            // TODO: Add metrics here
            return Optional.absent();
        }
        return accountOptional;
    }

    @SqlUpdate("UPDATE accounts SET name=:name, gender=:gender, dob=:dob, height=:height, weight=:weight, tz_offset=:tz_offset WHERE id=:account_id;")
    protected abstract Integer updateAccount(@BindAccount Account account, @Bind("account_id") Long accountId);


    public Optional<Account> update(final Account account, final Long accountId) {
        try {
            int updated = updateAccount(account, accountId);
            LOGGER.debug("Update: {} row updated for account_id = {}", updated, accountId);
            final Optional<Account> accountFromDB = getById(accountId);
            return accountFromDB;
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());

            if(matcher.find()) {
                LOGGER.warn("Update: Account with email {} already exists.", account.email);
            }

            LOGGER.warn("Update account failed: {}", exception.getMessage());
        }

        return Optional.absent();
    }
}
