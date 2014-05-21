package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;

/**
 * Created by pangwu on 4/18/14.
 */
public class OAuthScopeTest {

    @Test
    public void testFromInteger(){
        final int invalidInteger = 99;
        final Optional<OAuthScope> invalidOAuthScope = OAuthScope.fromInteger(invalidInteger);
        assertThat(invalidOAuthScope.isPresent(), is(false));


        final int validInteger = 0;
        final Optional<OAuthScope> validOAuthScope = OAuthScope.fromInteger(validInteger);
        assertThat(validOAuthScope.isPresent(), is(true));
        assertThat(validOAuthScope.get(), is(OAuthScope.USER_BASIC));
    }

    @Test
    public void testFromValidIntegerArray(){
        final Integer[] allValidInts = new Integer[]{OAuthScope.USER_BASIC.getValue(), OAuthScope.SCORE_READ.getValue()};
        final OAuthScope[] allValidScopes = OAuthScope.fromIntegerArray(allValidInts);

        ArrayList<OAuthScope> scopeList = new ArrayList<OAuthScope>();
        for(OAuthScope scope:allValidScopes){
            scopeList.add(scope);
        }
        assertThat(scopeList, contains(OAuthScope.USER_BASIC, OAuthScope.SCORE_READ));



    }

    @Test
    public void testFromInvalidIntegerArray(){
        final Integer[] allValidInts = new Integer[]{OAuthScope.USER_BASIC.getValue(), 99};
        final OAuthScope[] allInvalidScopes = OAuthScope.fromIntegerArray(allValidInts);

        ArrayList<OAuthScope> scopeList = new ArrayList<OAuthScope>();
        for(OAuthScope scope:allInvalidScopes){
            scopeList.add(scope);
        }
        assertThat(scopeList, contains(OAuthScope.USER_BASIC));

    }
}
