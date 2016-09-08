package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;
import com.hello.suripu.core.util.QuestionSurveyUtils;
import com.hello.suripu.core.util.QuestionUtils;
import org.apache.commons.collections.ListUtils;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;

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
    public List<Question> getQuestions(final Long accountId, final int accountAgeInDays, final DateTime todayLocal, final List<Question> questionProcessorQuestions, final int timeZoneOffset) {

        final List<Question> surveyQuestions = getSurveyQuestions(accountId, todayLocal, timeZoneOffset);
        final List<Question> allQuestions = ListUtils.union(surveyQuestions, questionProcessorQuestions);

        final ImmutableList<Question> sortedQuestions = QuestionUtils.sortQuestionByCategory(allQuestions);
        return sortedQuestions;
    }

    /*
    Logic for picking questions
    */
    public List<Question> getSurveyQuestions(final Long accountId, final DateTime todayLocal, final int timeZoneOffset) {
        //Get available survey questions
        final List<Response> surveyResponses = questionResponseReadDAO.getAccountResponseByQuestionCategoryStr(accountId, QuestionCategory.SURVEY.toString().toLowerCase());
        final List<Question> availableQuestions = QuestionSurveyUtils.getSurveyXQuestion(surveyResponses, surveyQuestions);

        if (availableQuestions.isEmpty()) {
            return availableQuestions;
        }

        //If user already responded to a survey question today, do not serve another
        if (!surveyResponses.isEmpty() && surveyResponses.get(0).created.plusMillis(timeZoneOffset).withTimeAtStartOfDay().isEqual(todayLocal)) {
            return Lists.newArrayList();
        }

        //Outputs 1st available question
        final List<Question> outputSurveyQuestions = availableQuestions.subList(0, 1);
        final DateTime expiration = todayLocal.plusDays(1);
        return processQuestions(outputSurveyQuestions, accountId, todayLocal, expiration);

    }

    /*
    Saves question to accountQuestions if not already in database
    Return question with the accountQuestions Id from database
     */
    private List<Question> processQuestions(final List<Question> questions, final Long accountId, final DateTime todayLocal, final DateTime expireDate) {

        final List<Question> processedQuestions = Lists.newArrayList();

        //Check if database already has question with unique index on (account_id, question_id, created_local_utc_ts)
        final Optional<AccountQuestion> savedQuestion = getSavedAccountQuestionOptional(accountId, questions.get(0), todayLocal);
        if (savedQuestion.isPresent()) {
            final Long savedAccountQId = savedQuestion.get().id;
            final Question processedQuestion = Question.withAccountQId(questions.get(0), savedAccountQId);
            processedQuestions.add(processedQuestion);
        }
        else { //Save question and reform with unique index
            final Long accountQId = saveQuestion(accountId, questions.get(0), todayLocal, expireDate);
            final Question processedQuestion = Question.withAccountQId(questions.get(0), accountQId);
            processedQuestions.add(processedQuestion);
        }

        return processedQuestions;
    }

    /*
    Insert questions
     */
    private Long saveQuestion(final Long accountId, final Question question, final DateTime todayLocal, final DateTime expireDate) {
        try {
            LOGGER.debug("action=saving_question processor=question_survey account_id={} question_id={} today_local={} expire_date={}", accountId, question.id, todayLocal, expireDate);
            return this.questionResponseDAO.insertAccountQuestion(accountId, question.id, todayLocal, expireDate);

        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if (matcher.find()) {
                LOGGER.debug("action=not_saved_question reason=unable_to_execute_sql processor=question_survey account_id={} question_id={}", accountId, question.id);
            }
        }
        return 0L;
    }

    /*
    Get question from database. For purpose of 1. determine if question already exists in db and 2. if true, extract db's accountQuestions Id
     */
    private Optional<AccountQuestion> getSavedAccountQuestionOptional(final Long accountId, final Question question, final DateTime created) {
        final List<AccountQuestion> accountQuestions = questionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(accountId, question.id, created);
        if (accountQuestions.isEmpty()) {
            return Optional.absent();
        }
        return Optional.of(accountQuestions.get(0));
    }
}
