package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Question {

    public enum Type {
        CHOICE("choice"),
        YES_NO("yes_no");

        private String value;

        private Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @JsonProperty("id")
    final public Long id;

    @JsonProperty("text")
    final public String text;

    @JsonProperty("type")
    final public Type type;

    @JsonProperty("choices")
    final List<Choice> choiceList;

    public Question(final Long id, final String text, final Type type, final List<Choice> choiceList) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.choiceList = choiceList;
    }
}
