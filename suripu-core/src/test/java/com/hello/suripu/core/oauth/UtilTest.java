package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class UtilTest {

    private String bearer;

    @Before
    public void setUp() {
        bearer = "3a2bf678def149289ca50c90a9099e7a";
    }

    @Test
    public void testExtractValidBearer() {
        final String headerString = String.format("Bearer  %s", bearer);
        final String headerStringLowercase = String.format("Bearer  %s", bearer);

        final String[] headers = new String[]{headerString, headerStringLowercase};
        for(String header : headers) {
            final Optional<String> extractedBearer = Util.extractBearerToken(header);
            assertThat(extractedBearer.isPresent(), is(true));
            assertThat(bearer, equalTo(bearer));
        }
    }

    @Test
    public void testExtractInvalidBearer() {
        // Note the space before Bearer
        final String headerString = String.format(" Bearer %s", bearer);
        final Optional<String> extractedBearer = Util.extractBearerToken(headerString);
        assertThat(extractedBearer.isPresent(), is(false));
    }
}
