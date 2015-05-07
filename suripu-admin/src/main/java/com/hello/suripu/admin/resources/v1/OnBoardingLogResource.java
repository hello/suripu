package com.hello.suripu.admin.resources.v1;

import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.OnBoardingLogDAO;
import com.hello.suripu.core.models.OnBoardingLog;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
    @Path("/sense/{sense_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<OnBoardingLog> getLogs(@Scope(OAuthScope.ADMINISTRATION_READ) final AccessToken accessToken,
                                       @PathParam("sense_id") final String senseId){
        try{
            final List<OnBoardingLog> logs = this.onBoardingLogDAO.getBySenseId(senseId);
            return logs;
        }catch (Exception ex){
            LOGGER.error("Get pairing log from sense {} failed.", senseId);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }
}
