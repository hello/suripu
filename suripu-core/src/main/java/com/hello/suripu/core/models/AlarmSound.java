package com.hello.suripu.core.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

/**
 * Created by pangwu on 9/17/14.
 */
public class AlarmSound {

    @JsonProperty("id")
    public final long id;

    @JsonProperty("name")
    public final String name;


    @JsonCreator
    public AlarmSound(@JsonProperty("id") long id,
                      @JsonProperty("name") final String name){
        this.id = id;
        this.name = name;

    }

    @Override
    public int hashCode(){
        return (int)id;
    }

    @Override
    public boolean equals(final Object other){
        if (other == null){
            return false;
        }

        if (getClass() != other.getClass()){
            return false;
        }

        final AlarmSound sound = (AlarmSound) other;
        return Objects.equal(sound.id, this.id);
    }
}
