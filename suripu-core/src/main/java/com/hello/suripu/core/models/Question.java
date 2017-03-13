package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.hello.suripu.core.models.Questions.QuestionCategory;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.util.List;

public class Question {

    public enum Type {
        CHOICE("choice"),
        CHECKBOX("checkbox"),
        QUANTITY("quantity"),
        DURATION("duration"),
        TIME("time"),
        TEXT("text");

        private String value;

        private Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static Type fromString(final String text) {
            if (text != null) {
                for (final Type questionType : Type.values()) {
                    if (text.equalsIgnoreCase(questionType.toString())) {
                        return questionType;
                    }
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public enum FREQUENCY {
        ONE_TIME("one_time"),
        DAILY("daily"),
        OCCASIONALLY("occasionally"),
        TRIGGER("trigger");

        private String value;

        private FREQUENCY(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public String toSQLString() {return value.toLowerCase();}

        public static FREQUENCY fromString(final String text) {
            if (text != null) {
                for (final FREQUENCY frequency : FREQUENCY.values()) {
                    if (text.equalsIgnoreCase(frequency.toString())) {
                        return frequency;
                    }
                }
            }
            throw new IllegalArgumentException();
        }

    }

    public enum ASK_TIME {
        MORNING("morning"),
        AFTERNOON("afternoon"),
        EVENING("evening"),
        ANYTIME("anytime");

        private String value;

        private ASK_TIME(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        public static ASK_TIME fromString(final String text) {
            if (text != null) {
                for (final ASK_TIME askTime : ASK_TIME.values()) {
                    if (text.equalsIgnoreCase(askTime.toString())) {
                        return askTime;
                    }
                }
            }
            throw new IllegalArgumentException();
        }

    }

    @JsonProperty("id")
    final public Integer id;

    @JsonProperty("account_question_id")
    final public Long accountQuestionId;

    @JsonProperty("text")
    final public String text;

    @JsonProperty("choices")
    final public List<Choice> choiceList;

    @JsonProperty("ask_local_date")
    final public DateTime askLocalDate;

    @JsonProperty("type")
    final public Type type;

    @JsonIgnore
    final public FREQUENCY frequency;

    @JsonProperty("ask_time")
    final public ASK_TIME askTime;

    @JsonIgnore
    final public int dependency;

    @JsonIgnore
    final public int parentId;

    @JsonIgnore
    final public String lang;

    @JsonIgnore
    final public AccountInfo.Type accountInfo;

    @JsonIgnore
    final public DateTime accountCreationDate; // UTC

    @JsonIgnore
    final public QuestionCategory category;

    @JsonIgnore
    final public List<Integer> dependencyResponse;

    public Question(final Integer id, final Long accountQuestionId, final String text, final String lang,
                    final Type type, final FREQUENCY frequency, final ASK_TIME askTime,
                    final int dependency, final int parentId,
                    final DateTime askLocalDate, final List<Choice> choiceList,
                    final AccountInfo.Type accountInfo,
                    final DateTime accountCreationDate,
                    final QuestionCategory category,
                    @NotNull final List<Integer> dependencyResponse) {
        this.id = id;
        this.accountQuestionId = accountQuestionId;
        this.text = text;
        this.lang = lang;
        this.type = type;
        this.frequency = frequency;
        this.askTime = askTime;
        this.dependency = dependency;
        this.parentId = parentId;
        this.askLocalDate = askLocalDate;
        this.choiceList = choiceList;
        this.accountInfo = accountInfo;
        this.accountCreationDate = accountCreationDate;
        this.category = category;
        this.dependencyResponse = dependencyResponse;
    }

    public static Question withAskTimeAccountQId(final Question question,
                                                 final Long accountQuestionId,
                                                 final DateTime askLocalTime,
                                                 final DateTime created) {
        return new Question(question.id, accountQuestionId, question.text, question.lang,
                            question.type, question.frequency, question.askTime,
                            question.dependency, question.parentId, askLocalTime, question.choiceList, question.accountInfo,
                            created, question.category, question.dependencyResponse);
    }

    public static Question withAccountQId(final Question question, final Long accountQuestionId) {
        return new Question(
                question.id,
                accountQuestionId,
                question.text,
                question.lang,
                question.type,
                question.frequency,
                question.askTime,
                question.dependency,
                question.parentId,
                question.askLocalDate,
                question.choiceList,
                question.accountInfo,
                question.accountCreationDate,
                question.category,
                question.dependencyResponse);
    }

}
