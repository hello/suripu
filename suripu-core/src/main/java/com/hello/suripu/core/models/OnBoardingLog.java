package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hello.suripu.api.logging.LoggingProtos;
import com.hello.suripu.core.util.PairAction;
import com.hello.suripu.core.util.PairingResults;

/**
 * Created by pangwu on 5/7/15.
 */
public class OnBoardingLog {

    @JsonProperty("sense_id")
    public final String senseId;

    @JsonProperty("pill_id")
    public final Optional<String> pillIdOptional;

    @JsonProperty("account_id")
    public final Optional<Long> accountIdOptional;

    @JsonProperty("info")
    public final String info;

    @JsonProperty("result")
    public final PairingResults result;

    @JsonProperty("action")
    public final PairAction pairAction;

    @JsonProperty("ts_millis")
    public final long timestampMillis;

    @JsonProperty("ip")
    public final String ip;


    public OnBoardingLog(@JsonProperty("sense_id") final String senseId,
                         @JsonProperty("pill_id") final Optional<String> pillIdOptional,
                         @JsonProperty("account_id") final Optional<Long> accountIdOptional,
                         @JsonProperty("info") final String info,
                         @JsonProperty("result") final PairingResults result,
                         @JsonProperty("action") final PairAction pairAction,
                         @JsonProperty("ip") final String ip,
                         @JsonProperty("ts_millis") final long timestampMillis){
        this.senseId = senseId;
        this.accountIdOptional = accountIdOptional;
        this.pillIdOptional = pillIdOptional;
        this.info = info;
        this.result = result;
        this.pairAction = pairAction;
        this.timestampMillis = timestampMillis;
        this.ip = ip;
    }

    public static OnBoardingLog fromProtobuf(final LoggingProtos.RegistrationLog registrationLog){
        return new OnBoardingLog(registrationLog.getSenseId(),
                Optional.fromNullable(registrationLog.hasPillId() ? registrationLog.getPillId() : null),
                Optional.fromNullable(registrationLog.hasAccountId() ? registrationLog.getAccountId() : null),
                registrationLog.getInfo(),
                PairingResults.valueOf(registrationLog.getResult()),
                PairAction.valueOf(registrationLog.getAction()),
                registrationLog.getIpAddress(),
                registrationLog.getTimestamp());
    }
}
