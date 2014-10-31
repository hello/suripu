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
    final public boolean skip;
    final public DateTime created;
    final public Long accountQuestionId;
    final public DateTime askTime; // ask time in user local-utc-ts


    public Response(Long id, long accountId, Integer questionId, String response, boolean skip, DateTime created, Long accountQuestionId, DateTime askTime) {
        this.id = id;
        this.accountId = accountId;
        this.questionId = questionId;
        this.response = response;
        this.skip = skip;
        this.created = created;
        this.accountQuestionId = accountQuestionId;
        this.askTime = askTime;
    }
}
