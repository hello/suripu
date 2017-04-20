package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.binders.BindAccount;
import com.hello.suripu.core.db.binders.BindRegistration;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;
import com.hello.suripu.core.util.PasswordUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.mindrot.jbcrypt.BCrypt;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;

import static com.google.common.base.Preconditions.checkNotNull;

@RegisterMapper(AccountMapper.class)
public abstract class AccountDAOImpl implements AccountDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountDAOImpl.class);

    @SqlQuery("SELECT * FROM accounts WHERE id = :id LIMIT 1;")
    @SingleValueResult(Account.class)
    public abstract Optional<Account> getById(@Bind("id") final Long id);

    @SingleValueResult(Account.class)
    @SqlQuery("SELECT * FROM accounts WHERE external_id=:external_id")
    public abstract Optional<Account> getByExternalId(@Bind("external_id") UUID uuid);

    @SqlQuery("SELECT * FROM accounts WHERE email = :email LIMIT 1;")
    @SingleValueResult(Account.class)
    public abstract Optional<Account> getByEmail(@Bind("email") final String email);

    @SqlQuery("SELECT * FROM accounts order by id desc LIMIT :limit;")
    public abstract List<Account> getRecent(@Bind("limit") final Integer limit);

    @SqlQuery("SELECT * FROM accounts ORDER BY id DESC;")
    public abstract List<Account> getAll();

    @SqlQuery("INSERT INTO accounts (name, firstname, lastname, email, password_hash, dob, height, weight, tz_offset, created, last_modified) VALUES(:name, :firstname, :lastname, :email, :password, :dob, :height, :weight, :tz_offset, :created, :last_modified) RETURNING *")
    public abstract Account insertNewAccount(@BindRegistration Registration registration, @Bind("last_modified") Long lastModified);

    @SqlUpdate("UPDATE accounts SET password_hash = :new_password_hash WHERE password_hash = :current_password_hash AND id = :account_id;")
    public abstract int updatePassword(@Bind("new_password_hash") final String newPasswordHash, @Bind("current_password_hash") final String currentPasswordHash, @Bind("account_id") final Long accountId);

    @SqlUpdate("UPDATE accounts SET password_hash = :new_password_hash WHERE id = :account_id;")
    protected abstract int updatePasswordFromResetEmail(@Bind("new_password_hash") final String newPasswordHash, @Bind("account_id") final Long accountId);

    @SqlUpdate("UPDATE accounts SET email = :email, last_modified = :new_last_modified WHERE id = :account_id AND last_modified = :last_modified;")
    protected abstract int updateEmail(@Bind("email") final String email, @Bind("account_id") final Long accountId, @Bind("last_modified") final Long lastModified, @Bind("new_last_modified") final Long newLastModified);

    public Account register(final Registration registration) {
        final Account account = insertNewAccount(registration, registration.created.getMillis());
        return Account.fromRegistration(registration, account.id.get(), account.externalId.get());
    }


    public Optional<Account> updateEmail(final Account account) {
        return updateEmail(account, DateTime.now().getMillis());
    }

    private Optional<Account> updateEmail(final Account account, final Long lastModified) {
        try {
            int rows = updateEmail(account.email, account.id.get(), account.lastModified, lastModified);
            if (rows == 0) {
                return Optional.absent();
            }

            return getById(account.id.get());
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if (matcher.find()) {
                LOGGER.error("Account with email = {} already exists", account.email);
            }
            LOGGER.error("Unknown error: {}", exception.getMessage());
        }

        return Optional.absent();
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
            LOGGER.warn("exists: Account wasn't found for email = {} and password = {}...", email, PasswordUtil.obfuscate(password));
            return Optional.absent();
        }


        final String passwordHashFromDB = accountOptional.get().password;
        if(!BCrypt.checkpw(password, passwordHashFromDB)) {
            LOGGER.warn("exists: Passwords don't match");
            // TODO: Add metrics here
            return Optional.absent();
        }
        return accountOptional;
    }


    @SqlUpdate("UPDATE accounts SET name=:name, firstname=:firstname, lastname=:lastname, gender=:gender, gender_name=:gender_name, dob=:dob, height=:height, weight=:weight, " +
            "tz_offset=:tz_offset, last_modified= :new_last_modified WHERE id=:account_id AND last_modified=:last_modified;")
    protected abstract Integer updateAccount(@BindAccount Account account, @Bind("account_id") Long accountId, @Bind("new_last_modified") final Long lastModified);


    public Optional<Account> update(final Account account, final Long accountId) {
        LOGGER.debug("attempting update with Last modified = {}", account.lastModified);
        int updated = updateAccount(account, accountId, DateTime.now().getMillis());
        LOGGER.debug("Update: {} row updated for account_id = {}", updated, accountId);

        if(updated == 0) {
            LOGGER.warn("No row was updated for account_id = {} and last_modified = {}", accountId, account.lastModified);
            return Optional.absent();
        }

        final Optional<Account> accountFromDB = getById(accountId);
        return accountFromDB;
    }


    public Boolean updatePassword(final Long accountId, final PasswordUpdate passwordUpdate) {
        final Optional<Account> accountOptional = getById(accountId);
        if(!accountOptional.isPresent()) {
            LOGGER.warn("Account {} not found for password update");
            return Boolean.FALSE;
        }

        final String passwordHashFromDB = accountOptional.get().password;
        if(!BCrypt.checkpw(passwordUpdate.currentPassword, passwordHashFromDB)) {
            return Boolean.FALSE;
        }

        int updated = updatePassword(passwordUpdate.newPassword, passwordHashFromDB, accountId);
        LOGGER.warn("Updated {} rows during update password for user = {}", updated, accountId);
        return updated > 0;
    }

    public Boolean updatePasswordFromResetEmail(final Long accountId, final String password, final String state) {
        final Optional<Account> accountOptional = getById(accountId);
        if(!accountOptional.isPresent()) {
            LOGGER.warn("Account {} not found for password update");
            return Boolean.FALSE;
        }

        final String stateFromDB = DigestUtils.md5Hex(accountOptional.get().password);
        if(!stateFromDB.equals(state)) {
            LOGGER.error("State doesn't match for password update account = {} (expected = {}, received = {}", accountId, stateFromDB, state);
            return Boolean.FALSE;
        }
        int updated = updatePasswordFromResetEmail(password, accountId);
        LOGGER.warn("Updated {} rows during update password for user = {}", updated, accountId);
        return updated > 0;
    }

    @SqlUpdate("DELETE FROM accounts where email = :email;")
    public abstract void delete(@Bind("email") String email);

    @SqlQuery("SELECT * FROM accounts WHERE name ILIKE '%'||:name_partial||'%' ORDER BY id DESC LIMIT 50;")
    public abstract List<Account> getByNamePartial(
            @Bind("name_partial") final String namePartial
    );

    @SqlQuery("SELECT * FROM accounts WHERE email ILIKE '%'||:email_partial||'%' ORDER BY id DESC LIMIT 50;")
    public abstract List<Account> getByEmailPartial(
            @Bind("email_partial") final String emailPartial
    );
}
