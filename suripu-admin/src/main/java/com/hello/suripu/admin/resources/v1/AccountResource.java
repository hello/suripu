package com.hello.suripu.admin.resources.v1;

import com.hello.suripu.core.models.Account;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/admin/v1/account")
public class AccountResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account getAccount() {

        return null;

    }
}
