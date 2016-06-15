package com.hello.suripu.coredw8.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"client_id", "response_type", "state", "scope", "internal_state"})
public class ClientAuthRequest {

  @JsonProperty("client_id")
  public final String clientId;

  @JsonProperty("response_type")
  public final String responseType;

  @JsonProperty("state")
  public final String state;

  @JsonProperty("scope")
  public final String scope;

  @JsonProperty("internal_state")
  public final Integer internalState;


  @JsonCreator
  public ClientAuthRequest(
      @JsonProperty("client_id") final String clientId,
      @JsonProperty("response_type") final String responseType,
      @JsonProperty("state") final String state,
      @JsonProperty("scope") final String scope,
      @JsonProperty("internal_state") final Integer internalState) {
    this.clientId = clientId;
    this.responseType = responseType;
    this.state = state;
    this.scope = scope;
    this.internalState = internalState;
  }

  public static class Builder {
    private String clientId;
    private String responseType;
    private String state;
    private String scope;
    private Integer internalState;

    public Builder() {
    }

    public Builder withClientId(final String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder withResponseType(final String responseType) {
      this.responseType = responseType;
      return this;
    }

    public Builder withState(final String state) {
      this.state = state;
      return this;
    }

    public Builder withScope(final String scope) {
      this.scope = scope;
      return this;
    }

    public Builder withInternalState(final Integer internalState) {
      this.internalState = internalState;
      return this;
    }


    public ClientAuthRequest build() {
      return new ClientAuthRequest(clientId, responseType, state, scope, internalState);
    }
  }

}
