package com.hello.suripu.coredropwizard.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Created by jnorgan on 6/10/16.
 */
@JsonPropertyOrder({"account_id", "application_id", "logged_in", "created_at", "expires_in"})
public class AuthCookie {

  @JsonProperty("account_id")
  @JsonSerialize(using = ToStringSerializer.class)
  public final Long accountId;

  @JsonProperty("logged_in")
  public final Boolean loggedIn;

  @JsonProperty("created_at")
  public final DateTime createdAt;

  @JsonProperty("expires_in")
  public final Integer expiresIn;


  @JsonCreator
  public AuthCookie(
      @JsonProperty("account_id") final Long accountId,
      @JsonProperty("logged_in") final Boolean loggedIn,
      @JsonProperty("created_at") final DateTime createdAt,
      @JsonProperty("expires_in") final Integer expiresIn) {
    this.accountId = accountId;
    this.loggedIn = loggedIn;
    this.createdAt = createdAt;
    this.expiresIn = expiresIn;
  }

  public static class Builder {
    private Long accountId;
    private Boolean loggedIn;
    private DateTime createdAt;
    private Integer expiresIn;

    public Builder() {
      createdAt = DateTime.now(DateTimeZone.UTC);
    }

    public Builder withAccountId(final Long accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withLoggedIn(final Boolean loggedIn) {
      this.loggedIn = loggedIn;
      return this;
    }

    public Builder withExpiresIn(Integer expiresIn) {
      this.expiresIn = expiresIn;
      return this;
    }

    public Builder withCreatedAt(final DateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public AuthCookie build() {
      return new AuthCookie(accountId, loggedIn, createdAt, expiresIn);
    }
  }

}
