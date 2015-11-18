package com.hello.suripu.core.models;

import com.hello.suripu.core.util.AlgorithmType;
import org.joda.time.DateTime;

/**
 * Created by benjo on 11/17/15.
 */
public class AccountAlgorithm {
    public final AlgorithmType algorithmType;

    public final Long accountId;

    public final DateTime dateOfSpecification;

    public AccountAlgorithm(AlgorithmType algorithmType, Long accountId, final DateTime dateOfSpecification) {
        this.algorithmType = algorithmType;
        this.accountId = accountId;
        this.dateOfSpecification = dateOfSpecification;
    }
}
