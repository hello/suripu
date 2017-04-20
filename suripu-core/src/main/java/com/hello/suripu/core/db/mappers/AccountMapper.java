package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Account;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class AccountMapper implements ResultSetMapper<Account> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountMapper.class);
    @Override
    public Account map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Account.Builder builder = new Account.Builder();
        final String firstnameOrNull = r.getString("firstname");
        final String firstname = (firstnameOrNull == null || firstnameOrNull.isEmpty()) ? r.getString("name") : firstnameOrNull;

        builder.withName(r.getString("name"));
        builder.withEmail(r.getString("email"));
        builder.withHeight(r.getInt("height"));
        builder.withWeight(r.getInt("weight"));
        builder.withId(r.getLong("id"));
        builder.withTzOffsetMillis(r.getInt("tz_offset"));
        builder.withPassword(r.getString("password_hash"));
        builder.withGender(r.getString("gender"));
        builder.withGenderName(r.getString("gender_name"));


        builder.withFirstname(firstname);
        builder.withLastname(r.getString("lastname"));

        builder.withDOB(new DateTime(r.getTimestamp("dob"), DateTimeZone.UTC));
        builder.withLastModified(r.getLong("last_modified"));
        builder.withCreated(new DateTime(r.getTimestamp("created"), DateTimeZone.UTC));
        builder.withAccountVerified(Boolean.FALSE);
        builder.withExternalId(UUID.fromString(r.getString("external_id")));
        final Account account = builder.build();

        LOGGER.trace("last_modified from DB = {}", r.getLong("last_modified"));
        LOGGER.trace("Account from DB: {}", account);

        return account;
    }
}
