package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import com.hello.suripu.core.processors.QuestionProcessor;
import com.yammer.metrics.annotation.Timed;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/questions")
public class QuestionsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionsResource.class);

    private final AccountDAO accountDAO;
    private final QuestionResponseDAO questionResponseDAO;
    private final QuestionProcessor questionProcessor;

    public QuestionsResource(final AccountDAO accountDAO, final QuestionResponseDAO questionResponseDAO) {
        this.accountDAO = accountDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.questionProcessor = new QuestionProcessor(this.questionResponseDAO);
    }

    @Timed
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Question> getQuestions(
            @Scope(OAuthScope.QUESTIONS_READ) final AccessToken accessToken,
            @QueryParam("date") final String date) {

        LOGGER.debug("Returning list of questions for account id = {}", accessToken.accountId);
        final Optional<Account> accountOptional = accountDAO.getById(accessToken.accountId);
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(404);
        }

        // TODO: get user timezone, remove this once we hook up the database
        final int timeZoneOffset = - 26200000;
        final DateTime today = DateTime.now(DateTimeZone.UTC).plusMillis(timeZoneOffset).withTimeAtStartOfDay();
//        final DateTime today = DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))).withTimeAtStartOfDay();
        LOGGER.debug("today = {}", today);
        if(date != null && !date.equals(today.toString("yyyy-MM-dd"))) {
            return Collections.EMPTY_LIST;
        }

        // get question
        final int numToGet = 2;
        final List<Question> questions = this.questionProcessor.getQuestions(accessToken.accountId, today, numToGet);

        return questions;
    }

    @Timed
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveAnswer(@Scope(OAuthScope.QUESTIONS_WRITE) final AccessToken accessToken, @Valid final Choice choice) {
        LOGGER.debug("Saving answer for account id = {}", accessToken.accountId);
        LOGGER.debug("Choice was = {}", choice.id);

        Optional<Integer> questionIdOptional = choice.questionId;
        Integer questionId = 0;
        if (questionIdOptional.isPresent()) {
            questionId = questionIdOptional.get();
        }

        this.questionResponseDAO.insertResponse(accessToken.accountId, questionId, choice.id);
    }

    @Timed
    @PUT
    @Path("/{question_id}/skip")
    @Consumes(MediaType.APPLICATION_JSON)
    public void skipQuestion(@Scope(OAuthScope.QUESTIONS_WRITE) final AccessToken accessToken,
                             @PathParam("question_id") final Integer questionId) {
        LOGGER.debug("Skipping question {} for account id = {}", questionId, accessToken.accountId);
        this.questionResponseDAO.insertSkippedQuestion(accessToken.accountId, questionId);
    }

}
