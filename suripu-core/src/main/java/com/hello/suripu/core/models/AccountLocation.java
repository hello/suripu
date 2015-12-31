package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by kingshy on 12/30/15.
 */
public class AccountLocation {

    public final Long id;
    public final Long accountId;
    public final String ip;
    public final Double latitude;
    public final Double longitude;
    public final String city;
    public final String state;
    public final String countryCode;
    public final DateTime created;

    public AccountLocation(final Long id, final Long accountId,
                           final String ip, final Double latitude, final Double longitude,
                           final String city, final String state, final String countryCode,
                           final DateTime created) {
        this.id = id;
        this.accountId = accountId;
        this.ip = ip;
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.state = state;
        this.countryCode = countryCode;
        this.created = created;
    }
}
