package com.hello.suripu.core.models;

import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by kingshy on 10/27/14.
 */
public class Response {
    public static class ResponseCreationException extends RuntimeException {
        public ResponseCreationException(String message) {
            super(message);
        }
    }


    final public Long id;
    final public long accountId;
    final public Integer questionId;
    final public String response;
    final public Optional<Integer> responseId;
    final public Optional<Boolean> skip;
    final public DateTime created;
    final public Long accountQuestionId;
    final public Optional<Question.FREQUENCY> questionFreq;
    final public DateTime askTime; // ask time in user local-utc-ts


    public Response(final Long id, final Long accountId, final Integer questionId,
                    final String response, final Optional<Integer> responseId, final Optional<Boolean> skip,
                    final DateTime created, final Long accountQuestionId,
                    final Optional<Question.FREQUENCY> frequency, final DateTime askTime) {
        this.id = id;
        this.accountId = accountId;
        this.questionId = questionId;
        this.response = response;
        this.responseId = responseId;
        this.skip = skip;
        this.created = created;
        this.accountQuestionId = accountQuestionId;
        this.questionFreq = frequency;
        this.askTime = askTime;
    }

    public static class Builder {
        private Long id;
        private long accountId;
        private Integer questionId;
        private String response;
        private Optional<Integer> responseId;
        private Optional<Boolean> skip;
        private DateTime created;
        private Long accountQuestionId;
        private Optional<Question.FREQUENCY> questionFreq;
        private DateTime askTime; // ask time in user local-utc-ts

        public Builder() {
            this.id = 0L;
            this.accountId = 0L;
            this.questionId = 0;
            this.response = "";
            this.responseId = Optional.absent();
            this.skip = Optional.absent();
            this.created = DateTime.now(DateTimeZone.UTC);
            this.accountQuestionId = 0L;
            this.questionFreq = Optional.absent();
            this.askTime = DateTime.now(DateTimeZone.UTC);
        }

        public Builder withId(final Long id) {
            this.id = id;
            return this;
        }

        public Builder withAccountId(final Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withQuestionId(final Integer questionId) {
            this.questionId = questionId;
            return this;
        }

        public Builder withResponse(final String response) {
            this.response = response;
            return this;
        }

        public Builder withResponseId(final int responseId) {
            this.responseId = Optional.fromNullable(responseId);
            return this;
        }

        public Builder withSkip(final Boolean skip) {
            this.skip = Optional.fromNullable(skip);
            return this;
        }

        public Builder withCreated(final DateTime created) {
            this.created = created;
            return this;
        }

        public Builder withAccountQuestionId(final Long accountQuestionId) {
            this.accountQuestionId = accountQuestionId;
            return this;
        }

        public Builder withQuestionFreq(final String frequency) {
            if (frequency != null) {
                this.questionFreq = Optional.fromNullable(Question.FREQUENCY.valueOf(frequency.toUpperCase()));
            }
            return this;
        }

        public Builder withAskTime(final DateTime askTime) {
                this.askTime = askTime;
            return this;
        }

        public Response build() throws ResponseCreationException {
            return new Response(id, accountId, questionId, response, responseId, skip, created, accountQuestionId, questionFreq, askTime);
        }
    }
}
