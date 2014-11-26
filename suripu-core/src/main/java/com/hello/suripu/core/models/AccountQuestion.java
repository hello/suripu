package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by kingshy on 10/28/14.
 */
public class AccountQuestion {
    final public Long id;
    final public long accountId;
    final public Integer questionId;
    final public DateTime askTime;


    public AccountQuestion(Long id, long accountId, Integer questionId, DateTime askTime) {
        this.id = id;
        this.accountId = accountId;
        this.questionId = questionId;
        this.askTime = askTime;
    }
}
