package com.hello.suripu.core.models;

/**
 * Created by jnorgan on 6/30/16.
 */
public enum AlarmSource {

  MOBILE_APP(0),
  VOICE_SERVICE(1),
  OTHER(2);

  private int value;

  private AlarmSource(int value) {
    this.value = value;
  }
}
