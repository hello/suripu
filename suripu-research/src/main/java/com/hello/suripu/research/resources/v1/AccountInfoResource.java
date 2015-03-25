package com.hello.suripu.research.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.DeviceDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.research.models.PartnerAccounts;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by pangwu on 3/6/15.
 */
@Path("/v1/accounts")
public class AccountInfoResource {

    private final AccountDAO accountDAO;
    private final DeviceDAO deviceDAO;

    public AccountInfoResource(final AccountDAO accountDAO,
                               final DeviceDAO deviceDAO){
        this.accountDAO = accountDAO;
        this.deviceDAO = deviceDAO;
    }

    @GET
    @Path("/{email}")
    @Produces(MediaType.APPLICATION_JSON)
    public PartnerAccounts getAccountInfo(@Scope({OAuthScope.RESEARCH}) final AccessToken accessToken,
                                          @PathParam("email") final String email){
        final Optional<Account> accountOptional = this.accountDAO.getByEmail(email);
        if (!accountOptional.isPresent() || !accountOptional.get().id.isPresent()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        final Optional<Long> partnerAccountIdOptional = this.deviceDAO.getPartnerAccountId(accountOptional.get().id.get());
        if(!partnerAccountIdOptional.isPresent()){
            return new PartnerAccounts(email, "", accountOptional.get().id.get(), 0L);
        }

        final Optional<Account> partnerAccount = this.accountDAO.getById(partnerAccountIdOptional.get());
        if(!partnerAccount.isPresent() || !partnerAccount.get().id.isPresent()){
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        return new PartnerAccounts(email, partnerAccount.get().email, accountOptional.get().id.get(), partnerAccount.get().id.get());
    }
}
