package com.hello.suripu.core.ota;

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
  
}
