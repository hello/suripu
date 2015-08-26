package com.hello.suripu.app.v2;

import com.hello.suripu.core.store.StoreFeedback;
import com.hello.suripu.core.store.StoreFeedbackDAO;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;

import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/v2/store/feedback")
public class StoreFeedbackResource {
    private final StoreFeedbackDAO storeFeedbackDAO;

    public StoreFeedbackResource(final StoreFeedbackDAO storeFeedbackDAO) {
        this.storeFeedbackDAO = storeFeedbackDAO;
    }

    @POST
    public Response post(@Scope(OAuthScope.STORE_FEEDBACK) final AccessToken accessToken,
                         @Valid StoreFeedback storeFeedback) {

        storeFeedbackDAO.save(StoreFeedback.forAccountId(storeFeedback, accessToken.accountId));
        return Response.status(Response.Status.ACCEPTED).entity("").build();
    }
}
