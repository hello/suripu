package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.support.SupportDAO;
import com.hello.suripu.core.support.SupportTopic;
import com.yammer.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/support")
public class SupportResource {

    private final SupportDAO supportDAO;

    public SupportResource(final SupportDAO supportDAO) {
        this.supportDAO = supportDAO;
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/topics")
    public List<SupportTopic> getTopics(@Scope(OAuthScope.SUPPORT) final AccessToken accessToken) {
        return supportDAO.getTopics();
    }
}
