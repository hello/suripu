package com.hello.suripu.research.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by pangwu on 3/6/15.
 */
public class PartnerAccounts {
    @JsonProperty("email")
    public final String masterEmail;

    @JsonProperty("account_id")
    public final Long masterAccountId;

    @JsonProperty("partner_email")
    public final String partnerEmail;

    @JsonProperty("partner_account_id")
    public final Long partnerAccountId;


    public PartnerAccounts(final String masterEmail, final String partnerEmail, final Long masterAccountId, final Long partnerAccountId){
        this.masterEmail = masterEmail;
        this.masterAccountId = masterAccountId;
        this.partnerEmail = partnerEmail;
        this.partnerAccountId = partnerAccountId;
    }
}
