package com.hello.suripu.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.suripu.core.util.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccountTest {

    final ObjectMapper objectMapper = new ObjectMapper();

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void testValidAccountDeserialization() throws IOException {
        final File jsonFile = new File(FileUtils.getResourceFilePath("fixtures/account/valid_account.json"));
        final Account account = objectMapper.readValue(jsonFile, Account.class);
        assertThat(account.email, is("email9877@email.com"));
        assertThat(account.gender, is(Gender.OTHER));
    }


    @Test
    public void testAccountWithExternalId() throws IOException {
        final File jsonFile = new File(FileUtils.getResourceFilePath("fixtures/account/valid_account_with_id.json"));
        final Account account = objectMapper.readValue(jsonFile, Account.class);
        assertThat(account.id.isPresent(), is(false));
        assertThat(account.externalId.isPresent(), is(false));
    }

    @Test
    public void getAgeInDays() {
        final DateTime fixedNow = new DateTime(2015, 9, 17, 11, 48, 0, DateTimeZone.UTC);
        DateTimeUtils.setCurrentMillisFixed(fixedNow.getMillis());

        final Account.Builder builder = new Account.Builder()
                .withId(42L)
                .withEmail("john.everyman@theinter.net");

        final Account createdToday = builder.withCreated(fixedNow).build();
        assertThat(createdToday.getAgeInDays(), is(equalTo(0)));

        final Account createdYesterday = builder.withCreated(fixedNow.minusDays(1)).build();
        assertThat(createdYesterday.getAgeInDays(), is(equalTo(1)));

        final Account createdLastWeek = builder.withCreated(fixedNow.minusWeeks(1)).build();
        assertThat(createdLastWeek.getAgeInDays(), is(equalTo(7)));

        final Account createdLastMonth = builder.withCreated(fixedNow.minusMonths(1)).build();
        assertThat(createdLastMonth.getAgeInDays(), is(equalTo(31)));
    }
}
