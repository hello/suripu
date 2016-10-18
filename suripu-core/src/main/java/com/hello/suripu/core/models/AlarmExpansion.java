package com.hello.suripu.core.models;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AlarmExpansion {

    @JsonProperty("id")
    public final long id;

    @JsonProperty("enable")
    public final Boolean enable;

    @JsonCreator
    public AlarmExpansion(@JsonProperty("id") long id,
                          @JsonProperty("enable") final Boolean enable){
        this.id = id;
        this.enable = enable;
    }

    @Override
    public int hashCode(){
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(final Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final AlarmExpansion expansion = (AlarmExpansion) other;
        return Objects.equal(expansion.id, this.id);
    }
}