package com.hello.suripu.core.models;

import org.joda.time.DateTime;

/**
 * Created by kingshy on 10/27/14.
 */
public class Response {
    final public Long id;
    final public long accountId;
    final public Integer questionId;
    final public String response;
    final public Integer responseId;
    final public boolean skip;
    final public DateTime created;
    final public Long accountQuestionId;
    final public DateTime askTime; // ask time in user local-utc-ts


    public Response(final Long id, final Long accountId, final Integer questionId,
                    final String response, final Integer responseId, final boolean skip,
                    final DateTime created, final Long accountQuestionId, final DateTime askTime) {
        this.id = id;
        this.accountId = accountId;
        this.questionId = questionId;
        this.response = response;
        this.responseId = responseId;
        this.skip = skip;
        this.created = created;
        this.accountQuestionId = accountQuestionId;
        this.askTime = askTime;
    }
}
