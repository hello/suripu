package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;
import com.hello.suripu.core.util.QuestionSurveyUtils;
import com.hello.suripu.core.util.QuestionUtils;
import org.apache.commons.collections.ListUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jyfan on 5/3/16.
 */
public class QuestionSurveyProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionSurveyProcessor.class);

    private final QuestionResponseReadDAO questionResponseReadDAO;
    private final QuestionResponseDAO questionResponseDAO;

    private final List<Question> surveyQuestions;

    public QuestionSurveyProcessor(final QuestionResponseReadDAO questionResponseReadDAO,
                                   final QuestionResponseDAO questionResponseDAO,
                                   final List<Question> surveyQuestions) {
        this.questionResponseReadDAO = questionResponseReadDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.surveyQuestions = surveyQuestions;
    }

    /*
    Build processor
     */
    public static class Builder {
        private QuestionResponseReadDAO questionResponseReadDAO;
        private QuestionResponseDAO questionResponseDAO;
        private List<Question> surveyQuestions;

        public Builder withQuestionResponseDAO(final QuestionResponseReadDAO questionResponseReadDAO,
                                               final QuestionResponseDAO questionResponseDAO) {
            this.questionResponseReadDAO = questionResponseReadDAO;
            this.questionResponseDAO = questionResponseDAO;
            return this;
        }

        public Builder withQuestions(final QuestionResponseReadDAO questionResponseReadDAO) {
            this.surveyQuestions = Lists.newArrayList();

            final List<Question> allQuestions = questionResponseReadDAO.getAllQuestions();
            for (final Question question : allQuestions) {
                if (question.category != QuestionCategory.SURVEY) {
                    continue;
                }
                this.surveyQuestions.add(question);
            }

            return this;
        }

        public QuestionSurveyProcessor build() {
            checkNotNull(questionResponseReadDAO, "questionResponseRead can not be null");
            checkNotNull(questionResponseDAO, "questionResponse can not be null");
            checkNotNull(surveyQuestions, "surveyQuestions can not be null");

            return new QuestionSurveyProcessor(this.questionResponseReadDAO, this.questionResponseDAO, this.surveyQuestions);
        }
    }

    /*
    Pick question, combine with output of questionProcessor and sort by question priority
     */
    public List<Question> getQuestions(final Long accountId, final int accountAgeInDays, final DateTime today, final List<Question> questionProcessorQuestions, final int timeZoneOffset) {

        final List<Question> surveyQuestions = getSurveyQuestions(accountId, today, timeZoneOffset);
        final List<Question> allQuestions = ListUtils.union(surveyQuestions, questionProcessorQuestions);

        final ImmutableList<Question> sortedQuestions = QuestionUtils.sortQuestionByCategory(allQuestions);
        return sortedQuestions;
    }

    /*
    Logic for picking questions
    */
    public List<Question> getSurveyQuestions(final Long accountId, final DateTime today, final int timeZoneOffset) {
        //Get available survey questions
        final List<Response> surveyResponses = questionResponseReadDAO.getAccountResponseByQuestionCategoryStr(accountId, QuestionCategory.SURVEY.toString().toLowerCase());
        final List<Question> availableQuestions = QuestionSurveyUtils.getSurveyXQuestion(surveyResponses, surveyQuestions);

        if (availableQuestions.isEmpty()) {
            return availableQuestions;
        }

        //If user already responded to a survey question today, do not serve another
        if (!surveyResponses.isEmpty() && surveyResponses.get(0).created.plusMillis(timeZoneOffset).withTimeAtStartOfDay().isEqual(today)) {
            return Lists.newArrayList();
        }

        //Returns and saves 1st available question.
        final DateTime expiration = today.plusDays(1);

        //Check if database already has question with unique index on (account_id, question_id, created_local_utc_ts)
        final Boolean savedQuestion = savedAccountQuestion(accountId, availableQuestions.get(0), today);
        if (savedQuestion) {
            return availableQuestions.subList(0, 1);
        }

        saveQuestion(accountId, availableQuestions.subList(0, 1).get(0), today, expiration);
        return availableQuestions.subList(0, 1);
    }

    /*
    Insert questions
     */

    private void saveQuestion(final Long accountId, final Question question, final DateTime today, final DateTime expireDate) {
        LOGGER.debug("action=saved_question processor=question_survey account_id={} question_id={} today={} expire_date={}", accountId, question.id, today, expireDate);
        this.questionResponseDAO.insertAccountQuestion(accountId, question.id, today, expireDate);
    }

    private Boolean savedAccountQuestion(final Long accountId, final Question question, final DateTime created) {
        final List<AccountQuestion> questions = questionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(accountId, question.id, created);
        return !questions.isEmpty();
    }
}
