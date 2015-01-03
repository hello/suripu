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

    @JsonProperty("url")
    public final String url;


    @JsonCreator
    public AlarmSound(@JsonProperty("id") long id,
                      @JsonProperty("name") final String name){
        this(id, name, "");
    }

    public AlarmSound(long id,
                      final String name,
                      final String url){
        this.id = id;
        this.name = name;
        this.url = url;
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

        final AlarmSound sound = (AlarmSound) other;
        return Objects.equal(sound.id, this.id);
    }
}