package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.List;

public class Question {

    public enum Type {
        CHOICE("choice"),
        CHECKBOX("checkbox"),
        TEXT("text");

        private String value;

        private Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
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

    }

    @JsonProperty("id")
    final public Integer id;

    @JsonProperty("text")
    final public String text;

    @JsonProperty("choices")
    final List<Choice> choiceList;

    @JsonProperty("ask_local_date")
    final DateTime askLocalDate;

    final public Type type;

    final public FREQUENCY frequency;

    final public ASK_TIME askTime;

    final int dependency;

    final int parentId;

    final String lang;

    public Question(final Integer id, final String text, final String lang,
                    final Type type, final FREQUENCY frequency, final ASK_TIME askTime, final int dependency, final int parentId,
                    final DateTime askLocalDate, final List<Choice> choiceList) {
        this.id = id;
        this.text = text;
        this.lang = lang;
        this.type = type;
        this.frequency = frequency;
        this.askTime = askTime;
        this.dependency = dependency;
        this.parentId = parentId;
        this.askLocalDate = askLocalDate;
        this.choiceList = choiceList;
    }

    public static Question withAskLocalTime(final Question question, final DateTime askLocalTime) {
        return new Question(question.id, question.text, question.lang,
                question.type, question.frequency, question.askTime,
                question.dependency, question.parentId, askLocalTime, question.choiceList);
    }

}
