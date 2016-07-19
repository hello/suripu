package com.hello.suripu.core.ota;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by jnorgan on 7/19/16.
 */
public class OTAStatus {

  @JsonProperty("status")
  public Status status;

  @JsonCreator
  public OTAStatus(final Status status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("status", status)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OTAStatus otaStatus = (OTAStatus) o;
    return Objects.equal(status, otaStatus.status);
  }

}
