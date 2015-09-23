package com.hello.suripu.service.registration;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.KeyStore;
import com.hello.suripu.core.oauth.ClientCredentials;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.stores.OAuthTokenStore;
import com.hello.suripu.service.SignedMessage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommonDeviceTest {

    private final OAuthTokenStore tokenStore = mock(OAuthTokenStore.class);
    private final KeyStore keyStore = mock(KeyStore.class);
    private final byte[] message = "hello".getBytes();
    private final byte[] key = "1234567891324657".getBytes();
    private final String senseId = "sense";

    @Test
    public void testMisingToken() {
        final String token = "abc";
        final ClientCredentials clientCredentials = new ClientCredentials(new OAuthScope[]{OAuthScope.AUTH}, token);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        when(tokenStore.getClientDetailsByToken(clientCredentials, now)).thenReturn(Optional.absent());
        final CommonDevice commonDevice = new CommonDevice(tokenStore, keyStore);
        final Optional<Long> accountIdOptional = commonDevice.getAccountIdFromTokenString(token, "", now);
        assertThat(accountIdOptional.isPresent(), is(false));
    }


    @Test
    public void testMissingKey() {
        final String senseId = "sense";
        when(keyStore.get(senseId)).thenReturn(Optional.<byte[]>absent());
        final CommonDevice commonDevice = new CommonDevice(tokenStore, keyStore);
        final boolean isValid  = commonDevice.validSignature(null, senseId);
        assertThat(isValid, is(false));
    }

    @Test
    public void testInvalidSignature() {
        final String senseId = "sense";

        final byte[] key = "1234567891324657".getBytes();
        when(keyStore.get(senseId)).thenReturn(Optional.of(key));

        final byte[] differentKey = "9876543219876543".getBytes();
        final byte[] message = "hello".getBytes();
        final Optional<byte[]> signedMessage = SignedMessage.sign(message, differentKey);

        final SignedMessage signedMessage1 = SignedMessage.parse(signedMessage.get());
        final CommonDevice commonDevice = new CommonDevice(tokenStore, keyStore);
        final boolean isValid  = commonDevice.validSignature(signedMessage1, senseId);
        assertThat(isValid, is(false));
    }

    @Test
    public void testValidSignature() {
        when(keyStore.get(senseId)).thenReturn(Optional.of(key));
        final byte[] signedMessage = SenseSigner.sign(message, key);

        final SignedMessage signedMessage1 = SignedMessage.parse(signedMessage);
        final CommonDevice commonDevice = new CommonDevice(tokenStore, keyStore);
        final boolean isValid  = commonDevice.validSignature(signedMessage1, senseId);
        assertThat(isValid, is(true));
    }
}
