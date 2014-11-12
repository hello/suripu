package com.hello.suripu.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.hello.suripu.core.util.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccountTest {

    final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testValidAccountDeserialization() throws IOException {
        final File jsonFile = new File(FileUtils.getResourceFilePath("fixtures/account/valid_account.json"));
        final Account account = objectMapper.readValue(jsonFile, Account.class);
        assertThat(account.email, is("email9877@email.com"));
        assertThat(account.gender, is(Gender.OTHER));
    }


    @Test(expected = InvalidFormatException.class)
    public void testAccountWithExternalId() throws IOException {
        final File jsonFile = new File(FileUtils.getResourceFilePath("fixtures/account/valid_account_with_id.json"));
        final Account account = objectMapper.readValue(jsonFile, Account.class);
    }
}
