package com.hello.suripu.core.models;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jnorgan on 9/27/16.
 */
public class ValueRange {

  @JsonProperty("min")
  public final Integer min;

  @JsonProperty("max")
  public final Integer max;

  @JsonCreator
  public ValueRange(@JsonProperty("min")final Integer min,
                    @JsonProperty("max")final Integer max) {
    if(min > max) {
      throw new IllegalArgumentException("Min is greater than Max.");
    }

    if(min == null) {
      this.min = 0;
    } else {
      this.min = min;
    }

    if(max == null) {
      this.max = 0;
    } else {
      this.max = max;
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(ValueRange.class)
        .add("min", min)
        .add("max", max)
        .toString();
  }

  public static ValueRange createEmpty() {
    return new ValueRange(0, 0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValueRange that = (ValueRange) o;
    return Objects.equal(min, that.min) &&
        Objects.equal(max, that.max);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(min, max);
  }

  public Boolean hasRangeWithinRange(final ValueRange bounds) {
    return (this.min >= bounds.min) &&
        (this.min <= bounds.max) &&
        (this.max >= bounds.min) &&
        (this.max <= bounds.max);
  }

  @JsonIgnore
  public Boolean isEmpty() {
    return (this.min == 0) &&
        (this.max == 0);
  }
}
