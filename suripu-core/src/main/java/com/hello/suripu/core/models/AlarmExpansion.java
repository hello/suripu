package com.hello.suripu.core.models;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AlarmExpansion {

    @JsonProperty("id")
    public final long id;

    @JsonProperty("enabled")
    public final Boolean enabled;

    @JsonProperty("category")
    public final String category;

    @JsonProperty("service_name")
    public String serviceName;

    @JsonProperty("target_value")
    public Integer targetValue;

    @JsonCreator
    public AlarmExpansion(@JsonProperty("id") long id,
                          @JsonProperty("enabled") final Boolean enabled,
                          @JsonProperty("category") final String category,
                          @JsonProperty("service_name") final String serviceName,
                          @JsonProperty("target_value") final Integer targetValue){
        this.id = id;
        this.enabled = enabled;
        this.category = category;
        this.serviceName = serviceName;
        this.targetValue = targetValue;
    }

    @Override
    public int hashCode(){
        return Objects.hashCode(this.id, this.enabled, this.category, this.targetValue);
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
        return Objects.equal(expansion.id, this.id) &&
            Objects.equal(expansion.enabled, this.enabled) &&
            Objects.equal(expansion.category, this.category) &&
            Objects.equal(expansion.targetValue, this.targetValue);
    }
}