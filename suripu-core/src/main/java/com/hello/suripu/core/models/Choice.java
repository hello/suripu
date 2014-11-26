package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Optional;

public class Choice {

    @JsonProperty("id")
    final public Integer id;

    @JsonProperty("text")
    final public String text;

    @JsonProperty("question_id")
    final public Optional<Integer> questionId;

    @JsonCreator
    public Choice(
            @JsonProperty("id") final Integer id,
            @JsonProperty("text") final String text,
            @JsonProperty("question_id") final Integer questionId) {
        this.id = id;
        this.text = text;
        this.questionId = Optional.fromNullable(questionId);
    }

    @Override
    public boolean equals(final Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final Choice convertedObject = (Choice) other;
        return Objects.equal(convertedObject.id, this.id) &&
                Objects.equal(convertedObject.text, this.text) &&
                Objects.equal(convertedObject.questionId, this.questionId) ;
    }
}
