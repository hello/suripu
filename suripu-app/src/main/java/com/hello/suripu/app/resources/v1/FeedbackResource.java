package com.hello.suripu.app.resources.v1;

import com.hello.suripu.core.db.FeedbackDAO;
import com.hello.suripu.core.models.SleepFeedback;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("/v1/feedback")
public class FeedbackResource {

    private final FeedbackDAO feedbackDAO;

    public FeedbackResource(final FeedbackDAO feedbackDAO) {
        this.feedbackDAO = feedbackDAO;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveFeedback(@Scope(OAuthScope.SLEEP_FEEDBACK) final AccessToken accessToken, final SleepFeedback feedback) {
        final SleepFeedback sleepFeedback = SleepFeedback.forAccount(feedback, accessToken.accountId);
        feedbackDAO.insert(sleepFeedback);
    }
}
