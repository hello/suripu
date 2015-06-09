package com.hello.suripu.admin.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.OnBoardingLog;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.util.JsonError;
import com.hello.suripu.core.util.PairingResults;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Created by pangwu on 5/7/15.
 */
@Path("/v1/onboarding_log")
public class OnBoardingLogResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(OnBoardingLogResource.class);

    private final AccountDAO accountDAO;
    private final OnBoardingLogDAO onBoardingLogDAO;

    public OnBoardingLogResource(final AccountDAO accountDAO, final OnBoardingLogDAO onBoardingLogDAO){
        this.accountDAO = accountDAO;
        this.onBoardingLogDAO = onBoardingLogDAO;
    }


    @GET
    @Path("/sense/{sense_id}/{count}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OnBoardingLog> getLogsBySenseId(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                                @PathParam("sense_id") final String senseId,
                                                @PathParam("count") final int count){
        try{
            final List<OnBoardingLog> logs = this.onBoardingLogDAO.getBySenseId(senseId, count);
            return logs;
        }catch (Exception ex){
            LOGGER.error("Get pairing log from sense {} failed: {}", senseId, ex.getMessage());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("/account/{email}/{count}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OnBoardingLog> getLogsByEmail(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                              @PathParam("email") final String email,
                                              @PathParam("count") final int count){
        final Optional<Account> accountOptional = accountDAO.getByEmail(email);
        if (!accountOptional.isPresent()) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .entity(new JsonError(Response.Status.NOT_FOUND.getStatusCode(), "Account not found")).build());
        }
        final long accountId = accountOptional.get().id.get();
        try{
            final List<OnBoardingLog> logs = this.onBoardingLogDAO.getByAccountId(accountId, count);
            return logs;
        }catch (Exception ex){
            LOGGER.error("Get pairing log from account {} failed: {}", accountId, ex.getMessage());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @GET
    @Path("/result")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OnBoardingLog> getLogsByResult(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                       @QueryParam("result") final PairingResults results,
                                       @QueryParam("start_millis") final long startMillis,
                                       @QueryParam("end_millis") final long endMillis){
        try{
            final List<OnBoardingLog> logs = this.onBoardingLogDAO.getByResult(results.toString(),
                    new DateTime(startMillis, DateTimeZone.UTC),
                    new DateTime(endMillis, DateTimeZone.UTC));
            return logs;
        }catch (Exception ex){
            LOGGER.error("Get pairing log for result {} from {} to {} failed: {}",
                    results,
                    startMillis,
                    endMillis,
                    ex.getMessage());
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}
