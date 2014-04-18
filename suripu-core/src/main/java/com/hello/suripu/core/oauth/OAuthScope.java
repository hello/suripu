package com.hello.suripu.core.oauth;

import com.google.common.base.Optional;

import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

public enum OAuthScope {

    USER_BASIC (0),
    USER_EXTENDED (1),
    SENSORS_BASIC(2),
    SENSORS_EXTENDED(3),
    SENSORS_WRITE(4),
    SCORE_READ(5),
    SLEEP_LABEL_BASIC(6),
    SLEEP_LABEL_WRITE(7);

    private int value;

    private OAuthScope(int value) {
        this.value = value;
    }


    public static Optional<OAuthScope> fromInteger(int value){
        try{
            return Optional.of(OAuthScope.values()[value]);
        }catch (ArrayIndexOutOfBoundsException ex){
            return Optional.absent();
        }
    }

    public static OAuthScope[] fromIntegerArray(Integer[] array){
        checkNotNull(array, "Cannot convert null to OAuthScope array.");

        final ArrayList<OAuthScope> scopeArrayList = new ArrayList<OAuthScope>();
        for(int i = 0; i < array.length; i ++) {
            final Optional<OAuthScope> oAuthScopeOptional = OAuthScope.fromInteger(array[i]);
            if(oAuthScopeOptional.isPresent()){
                scopeArrayList.add(oAuthScopeOptional.get());
            }
        }

        final OAuthScope[] scopeArray = scopeArrayList.toArray(new OAuthScope[0]);
        return scopeArray;
    }

    public int getValue() {
        return value;
    }
}
