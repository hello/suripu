package com.hello.suripu.coredw.db;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.mappers.AccountMapper;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Gender;
import com.hello.suripu.core.models.PasswordUpdate;
import com.hello.suripu.core.models.Registration;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccountDAOTest {

    private DBI    dbi;
    private Handle handle;
    private AccountDAO accountDAO;

    @Before
    public void setUp() throws Exception
    {
        final String createTableQuery = "CREATE TABLE accounts (\n" +
                "    id SERIAL PRIMARY KEY,\n" +
                "    name VARCHAR (100),\n" +
                "    username VARCHAR (100),\n" +
                "    email VARCHAR (255),\n" +
                "    password_hash CHAR (60),\n" +
                "    created TIMESTAMP,\n" +
                "    height SMALLINT,\n" +
                "    weight SMALLINT,\n" +
                ");\n" +
                "CREATE UNIQUE INDEX uniq_email on accounts(email);" +
                "ALTER TABLE accounts ALTER COLUMN weight SET DATA TYPE INTEGER;\n" +

                "ALTER TABLE accounts ADD COLUMN tz_offset INTEGER;\n" +
                "ALTER TABLE accounts ADD COLUMN last_modified BIGINT;\n" +
                "ALTER TABLE accounts ADD COLUMN dob TIMESTAMP; " +
                "ALTER TABLE accounts ADD COLUMN gender VARCHAR(50); " +
                "ALTER TABLE accounts ADD COLUMN firstname VARCHAR(255); " +
                "ALTER TABLE accounts ADD COLUMN lastname VARCHAR(255); ";

        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new AccountMapper());
        dbi.registerArgumentFactory(new JodaArgumentFactory());
        dbi.registerContainerFactory(new OptionalContainerFactory());
        handle = dbi.open();

        handle.execute(createTableQuery);

        accountDAO = dbi.onDemand(AccountDAOImpl.class);
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table accounts");
        handle.close();
    }

    private Registration newRegistration(final String email, final String password) {
        return new Registration("Test registration",  "test firstname", "test lastname", email, password, 123,
                Gender.OTHER, 123, 321, DateTime.now(), 10, 0.0, 0.0);
    }

    private Registration newRegistrationWithoutLastname(final String email, final String password) {
        return new Registration("Test registration",  "test firstname", null, email, password, 123,
                Gender.OTHER, 123, 321, DateTime.now(), 10, 0.0, 0.0);
    }

    @Test
    public void testRegister() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Account registered = accountDAO.register(registration);
        assertThat(registered.email, equalTo(registration.email));
    }

    @Test
    public void testRegisterMixedcasedEmail() {
        final Registration registration = newRegistration("TeSt@tEsT.CoM", "test");
        final Account registered = accountDAO.register(Registration.secureAndNormalize(registration));
        assertThat(registered.email, equalTo(registration.email.toLowerCase()));
    }

    @Test
    public void testRegisterAndGet() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Account registered = accountDAO.register(registration);
        assertThat(registered.email, equalTo(registration.email));
        final Optional<Account> optional = accountDAO.getByEmail(registration.email);
        assertThat(optional.isPresent(), is(true));
    }

    @Test
    public void testRegisterAndGetMixedcaseEmail() {
        final Registration registration = newRegistration("test@TEST.com", "test");
        final Account registered = accountDAO.register(Registration.secureAndNormalize(registration));
        assertThat(registered.email, equalTo(registration.email.toLowerCase()));
        final Optional<Account> optional = accountDAO.getByEmail(registration.email.toLowerCase());
        assertThat(optional.isPresent(), is(true));
    }

    @Test
    public void testLogin() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Registration encryptedRegistration = Registration.secureAndNormalize(registration);
        final Account registered = accountDAO.register(encryptedRegistration);
        assertThat(registered.email, equalTo(registration.email));
        final Optional<Account> optional = accountDAO.exists(registration.email, registration.password);
        assertThat(optional.isPresent(), is(true));
    }

    @Test
    public void testLoginMixedcaseEmail() {
        final Registration registration = newRegistration("TesT@TesT.com", "test");
        final Registration encryptedRegistration = Registration.secureAndNormalize(registration);
        final Account registered = accountDAO.register(encryptedRegistration);
        assertThat(registered.email, equalTo(registration.email.toLowerCase()));
        final Optional<Account> optional = accountDAO.exists(registration.email.toLowerCase(), registration.password);
        assertThat(optional.isPresent(), is(true));
    }

    @Test
    public void testRegisterAndGetMissingAccount() {
        final Registration registration = newRegistration("test@test.com", "test");
        accountDAO.register(registration);
        final Optional<Account> optional = accountDAO.getByEmail("random");
        assertThat(optional.isPresent(), is(false));
    }

    @Test(expected = UnableToExecuteStatementException.class)
    public void testRegisterExistingAccount() {
        final Registration registration = newRegistration("test@test.com", "test");
        accountDAO.register(registration);
        accountDAO.register(registration);
    }

    @Test
    public void updateExistingEmail() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Account account = accountDAO.register(registration);
        final Account updatedEmailAccount = new Account.Builder(account)
                .withEmail("new@test.com").build();
        final Optional<Account> optional = accountDAO.updateEmail(updatedEmailAccount);
        assertThat(optional.isPresent(), is(true));
    }

    @Test
    public void updateExistingEmailWithMixedcaseInput() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Account account = accountDAO.register(registration);

        final String newEmail = "New@test.com";
        final Account updatedEmailAccount = new Account.Builder(account)
                .withEmail(newEmail).build();
        final Account normalizedUpdatedEmail = Account.normalizeWithId(updatedEmailAccount, updatedEmailAccount.id.get());
        final Optional<Account> optional = accountDAO.updateEmail(normalizedUpdatedEmail);
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get().email, equalTo(newEmail.toLowerCase()));
    }

    @Test
    public void updateNonExistingEmail() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Account account = accountDAO.register(registration);
        final Account updatedEmailAccount = new Account.Builder(account)
                .withId(0L)
                .withEmail("new@test.com").build();
        final Optional<Account> optional = accountDAO.updateEmail(updatedEmailAccount);
        assertThat(optional.isPresent(), is(false));
    }

    @Test
    public void updatePassword() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Registration encryptedRegistration = Registration.secureAndNormalize(registration);
        final Account account = accountDAO.register(encryptedRegistration);
        final PasswordUpdate passwordUpdate = new PasswordUpdate(registration.password, "test2");
        final Boolean updated = accountDAO.updatePassword(account.id.get(), passwordUpdate);
        assertThat(updated, is(true));
    }

    @Test
    public void updatePasswordWithIncorrectCurrentPassword() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Registration encryptedRegistration = Registration.secureAndNormalize(registration);
        final Account account = accountDAO.register(encryptedRegistration);
        final PasswordUpdate passwordUpdate = new PasswordUpdate("wrong", "test2");
        final Boolean updated = accountDAO.updatePassword(account.id.get(), passwordUpdate);
        assertThat(updated, is(false));
    }

    @Test
    public void updateAccountRaceCondition() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Account account = accountDAO.register(registration);
        final Account updatedAccount = new Account.Builder(account).withLastModified(DateTime.now().plusMillis(1).getMillis()).build();
        final Optional<Account> updated = accountDAO.update(updatedAccount, account.id.get());
        assertThat(updated.isPresent(), is(false));
    }

    @Test
    public void updateAccount() {
        final Registration registration = newRegistration("test@test.com", "test");
        final Account account = accountDAO.register(registration);
        final Account updatedAccount = new Account.Builder(account).withName("New Name").build();
        final Optional<Account> updated = accountDAO.update(updatedAccount, account.id.get());
        assertThat(updated.isPresent(), is(true));
        final Optional<Account> fromDBOptional = accountDAO.getById(updated.get().id.get());
        assertThat(fromDBOptional.isPresent(), is(true));
        assertThat(fromDBOptional.get().name, equalTo(updatedAccount.name));
    }

    @Test
    public void updateAccountNoLastname() {
        final Registration registration = newRegistrationWithoutLastname("test@test.com", "test");
        final Account account = accountDAO.register(registration);
        final Account updatedAccount = new Account.Builder(account).withName("New Name").build();
        final Optional<Account> updated = accountDAO.update(updatedAccount, account.id.get());
        assertThat(updated.isPresent(), is(true));
        final Optional<Account> fromDBOptional = accountDAO.getById(updated.get().id.get());
        assertThat(fromDBOptional.isPresent(), is(true));
        assertThat(fromDBOptional.get().name, equalTo(updatedAccount.name));
        assertThat(fromDBOptional.get().firstname, equalTo(updatedAccount.firstname));
        assertThat(updated.get().lastname.isPresent(), is(false));
    }

    @Test
    public void getAccount() {
        final Registration registration = newRegistrationWithoutLastname("test@test.com", "test");
        final Account account = accountDAO.register(registration);
        final Optional<Account> fromDBOptional = accountDAO.getById(account.id.get());
        assertThat(fromDBOptional.isPresent(), is(true));
        assertThat(fromDBOptional.get().lastname.isPresent(), is(false));
        assertThat(fromDBOptional.get().firstname.isEmpty(), is(false));
    }
}
