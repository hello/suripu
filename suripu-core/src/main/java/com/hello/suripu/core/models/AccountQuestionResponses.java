package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by kingshy on 10/28/14.
 */
public class AccountQuestionResponses {
    final public Long id;
    final public long accountId;
    final public Integer questionId;
    final public DateTime askTime;
    final public Boolean responded;
    final public DateTime questionCreationDate; // UTC


    public AccountQuestionResponses(final Long id,
                                    final Long accountId,
                                    final Integer questionId,
                                    final DateTime askTime,
                                    final Boolean responded,
                                    final DateTime questionCreationDate) {
        this.id = id;
        this.accountId = accountId;
        this.questionId = questionId;
        this.askTime = askTime;
        this.responded = responded;
        this.questionCreationDate = questionCreationDate;
    }
}
