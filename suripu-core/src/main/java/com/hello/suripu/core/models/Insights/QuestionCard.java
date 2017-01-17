package com.hello.suripu.core.models.Insights;

import org.joda.time.DateTime;

/**
 * Created by jyfan on 11/7/16.
 */
public class QuestionCard {
    public Long accountId;
    public Integer questionId;
    public DateTime startDate;
    public DateTime expireDate;

    private QuestionCard(final Long accountId,
                        final Integer questionId,
                        final DateTime startDate,
                        final DateTime expireDate) {
        this.accountId = accountId;
        this.questionId = questionId;
        this.startDate = startDate;
        this.expireDate = expireDate;
    }

    public static QuestionCard createQuestionCard(final Long accountId,
                                                 final Integer questionId,
                                                 final DateTime startDate,
                                                 final DateTime expireDate) {
        return new QuestionCard(accountId, questionId, startDate, expireDate);
    }
}
