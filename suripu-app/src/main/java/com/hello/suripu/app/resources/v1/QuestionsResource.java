package com.hello.suripu.app.resources.v1;

import com.google.common.base.Optional;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.TimeZoneHistory;
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
    private static final long DAY_IN_MILLIS = 86400000L;
    private static final int DEFAULT_NUM_QUESTIONS = 2;
    private static final int DEFAULT_NUM_MORE_QUESTIONS = 4;

    private final AccountDAO accountDAO;
    private final TimeZoneHistoryDAODynamoDB tzHistoryDAO;
    private final QuestionProcessor questionProcessor;

    public QuestionsResource(final AccountDAO accountDAO, final QuestionResponseDAO questionResponseDAO, final TimeZoneHistoryDAODynamoDB tzHistoryDAO, final int checkSkipsNum) {
        this.accountDAO = accountDAO;
        this.tzHistoryDAO = tzHistoryDAO;
        this.questionProcessor = new QuestionProcessor(questionResponseDAO, checkSkipsNum);
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

        int timeZoneOffset = - 26200000;
        final Optional<TimeZoneHistory> tzHistory = this.tzHistoryDAO.getCurrentTimeZone(accessToken.accountId);
        if (tzHistory.isPresent()) {
            timeZoneOffset = tzHistory.get().offsetMillis;
        }

        final DateTime today = DateTime.now(DateTimeZone.UTC).plusMillis(timeZoneOffset).withTimeAtStartOfDay();
        LOGGER.debug("today = {}", today);
        if(date != null && !date.equals(today.toString("yyyy-MM-dd"))) {
            return Collections.EMPTY_LIST;
        }

        // get question
        final int accountAgeInDays =  (int) ((DateTime.now(DateTimeZone.UTC).getMillis() - accountOptional.get().created.getMillis()) / DAY_IN_MILLIS);

        return this.questionProcessor.getQuestions(accessToken.accountId, accountAgeInDays, today, DEFAULT_NUM_QUESTIONS, true);
    }

    @Timed
    @GET
    @Path("/more")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Question> getMoreQuestions(
            @Scope(OAuthScope.QUESTIONS_READ) final AccessToken accessToken) {

        // user asked for more questions
        LOGGER.debug("Returning list of questions for account id = {}", accessToken.accountId);
        final Optional<Account> accountOptional = accountDAO.getById(accessToken.accountId);
        if(!accountOptional.isPresent()) {
            throw new WebApplicationException(404);
        }

        int timeZoneOffset = - 26200000;
        final Optional<TimeZoneHistory> tzHistory = this.tzHistoryDAO.getCurrentTimeZone(accessToken.accountId);
        if (tzHistory.isPresent()) {
            timeZoneOffset = tzHistory.get().offsetMillis;
        }

        final DateTime today = DateTime.now(DateTimeZone.UTC).plusMillis(timeZoneOffset).withTimeAtStartOfDay();
        LOGGER.debug("More questions for today = {}", today);

        // get question
        final int accountAgeInDays =  (int) ((DateTime.now(DateTimeZone.UTC).getMillis() - accountOptional.get().created.getMillis()) / DAY_IN_MILLIS);

        return this.questionProcessor.getQuestions(accessToken.accountId, accountAgeInDays, today, DEFAULT_NUM_MORE_QUESTIONS, false);
    }

    @Timed
    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveAnswers(@Scope(OAuthScope.QUESTIONS_READ) final AccessToken accessToken,
                           @QueryParam("account_question_id") final Long accountQuestionId,
                           @Valid final List<Choice> choice) {
        LOGGER.debug("Saving answer for account id = {}", accessToken.accountId);

        Optional<Integer> questionIdOptional = choice.get(0).questionId;
        Integer questionId = 0;
        if (questionIdOptional.isPresent()) {
            questionId = questionIdOptional.get();
        }

        this.questionProcessor.saveResponse(accessToken.accountId, questionId, accountQuestionId, choice);
    }

    @Timed
    @PUT
    @Path("/{question_id}/skip_this")
    @Consumes(MediaType.APPLICATION_JSON)
    public void skipQuestion(@Scope(OAuthScope.QUESTIONS_READ) final AccessToken accessToken,
                             @PathParam("question_id") final Integer questionId,
                             @QueryParam("account_question_id") final Long accountQuestionId) {
        LOGGER.debug("Skipping question {} for account id = {}", questionId, accessToken.accountId);

        final Optional<TimeZoneHistory> tzHistory = this.tzHistoryDAO.getCurrentTimeZone(accessToken.accountId);
        int timeZoneOffset = - 26200000;
        if (tzHistory.isPresent()) {
            timeZoneOffset = tzHistory.get().offsetMillis;
        }

        this.questionProcessor.skipQuestion(accessToken.accountId, questionId, accountQuestionId, timeZoneOffset);
    }

    // keeping these for backward compatibility
    @Timed
    @POST
    @Deprecated
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveAnswer(@Scope(OAuthScope.QUESTIONS_WRITE) final AccessToken accessToken, @Valid final Choice choice) {
        LOGGER.debug("Saving answer for account id = {}", accessToken.accountId);
        LOGGER.debug("Choice was = {}", choice.id);
    }

    @Timed
    @PUT
    @Deprecated
    @Path("/{question_id}/skip")
    @Consumes(MediaType.APPLICATION_JSON)
    public void skipQuestion(@Scope(OAuthScope.QUESTIONS_WRITE) final AccessToken accessToken,
                             @PathParam("question_id") final Long questionId) {
        LOGGER.debug("Skipping question {} for account id = {}", questionId, accessToken.accountId);
    }

}
