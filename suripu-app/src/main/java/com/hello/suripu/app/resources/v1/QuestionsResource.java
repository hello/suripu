package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.oauth.AccessToken;
import com.hello.suripu.core.oauth.OAuthScope;
import com.hello.suripu.core.oauth.Scope;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

@Path("/v1/questions")
public class QuestionsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionsResource.class);

    private final AccountDAO accountDAO;

    public QuestionsResource(final AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

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

        // TODO: remove this once we hook up the database
        final DateTime today = DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/Los_Angeles")));
        LOGGER.debug("today = {}", today);
        if(!date.equals(today.toString("yyyy-MM-dd"))) {
            return Collections.EMPTY_LIST;
        }

        final Long questionId = 123L;
        final List<Choice> choices = new ArrayList<>();
        final Choice hot = new Choice(123456789L, "HOT", questionId);
        final Choice cold = new Choice(987654321L, "COLD", questionId);
        choices.add(hot);
        choices.add(cold);

        final String questionText = String.format("%s, do you sleep better when it is hot or cold?", accountOptional.get().name);
        final Question question = new Question(questionId, questionText, Question.Type.CHOICE, choices);
        final List<Question> questions = new ArrayList<>();
        questions.add(question);
        return questions;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveAnswer(@Scope(OAuthScope.QUESTIONS_WRITE) final AccessToken accessToken, @Valid final Choice choice) {
        LOGGER.debug("Saving answer for account id = {}", accessToken.accountId);
        LOGGER.debug("Choice was = {}", choice.id);
    }

}
