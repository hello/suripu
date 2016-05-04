package com.hello.suripu.core.processors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by jyfan on 5/3/16.
 */
public class QuestionSurveyProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionSurveyProcessor.class);

    private final QuestionResponseReadDAO questionResponseReadDAO;
    private final QuestionResponseDAO questionResponseDAO;

    private final List<Question> surveyOneQuestions = Lists.newArrayList();
    private final List<Integer> surveyOneQuestionIds = Lists.newArrayList();

    public QuestionSurveyProcessor(final QuestionResponseReadDAO questionResponseReadDAO,
                                   final QuestionResponseDAO questionResponseDAO,
                                   final List<Question> surveyOneQuestions,
                                   final List<Integer> surveyOneQuestionIds) {
        this.questionResponseReadDAO = questionResponseReadDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.surveyOneQuestions.addAll(surveyOneQuestions);
        this.surveyOneQuestionIds.addAll(surveyOneQuestionIds);
    }

    /*
    Build processor
     */
    public static class Builder {
        private QuestionResponseReadDAO questionResponseReadDAO;
        private QuestionResponseDAO questionResponseDAO;
        private List<Question> surveyOneQuestions;
        private List<Integer> surveyOneQuestionIds;

        public Builder withQuestionResponseDAO(final QuestionResponseReadDAO questionResponseReadDAO,
                                               final QuestionResponseDAO questionResponseDAO) {
            this.questionResponseReadDAO = questionResponseReadDAO;
            this.questionResponseDAO = questionResponseDAO;
            return this;
        }

        public Builder withQuestions(final QuestionResponseReadDAO questionResponseReadDAO) {
            this.surveyOneQuestions = Lists.newArrayList();
            this.surveyOneQuestionIds = Lists.newArrayList();

            final List<Question> allQuestions = questionResponseReadDAO.getAllQuestions();
            for (final Question question : allQuestions) {
                if (question.category != QuestionCategory.SURVEY_ONE) {
                    continue;
                }
                this.surveyOneQuestions.add(question);
                this.surveyOneQuestionIds.add(question.id);
            }

            return this;
        }

        public QuestionSurveyProcessor build() {
            return new QuestionSurveyProcessor(this.questionResponseReadDAO, this.questionResponseDAO, this.surveyOneQuestions, this.surveyOneQuestionIds);
        }
    }

    /*
    Logic for picking questions
     */
    public List<Question> getQuestions(final Long accountId, final int accountAgeInDays, final DateTime today) { //TODO mock test
        final List<Response> surveyOneResponses = questionResponseReadDAO.getAccountResponseByQuestionIds(accountId, QuestionCategory.SURVEY_ONE.toString());

        final List<Question> questions = getSurveyOneQuestion(surveyOneResponses, surveyOneQuestions);

        //Returns and saves 1st available question.  TODO: randomize?
        if (!questions.isEmpty()) {
            final DateTime expiration = today.plusDays(1);
            saveQuestion(accountId, questions.subList(0, 1).get(0), today, expiration); //TODO: What's the purpose of saving? Also monitor after deploy
            return questions.subList(0, 1);
        }
        return questions;
    }

    @VisibleForTesting
    public static List<Question> getSurveyOneQuestion(final List<Response> surveyOneResponses, final List<Question> surveyOneQuestions) {
        if (surveyOneResponses.size() == surveyOneQuestions.size()) {
            LOGGER.debug("User has responded to all survey one questions");
            return Lists.newArrayList();
        }

        final List<Integer> responded_question_ids = getRespondedQuestionIds(surveyOneResponses);
        final List<Integer> responded_response_ids = getRespondedResponseIds(surveyOneResponses);

        final List<Question> availableQuestions = Lists.newArrayList();
        for (Question question : surveyOneQuestions) { //TODO: zip each question with corresponding choice ids for better efficiency

            if (!Collections.disjoint(responded_question_ids, Lists.newArrayList(question.id))) {
                //User already responded to this question
                continue;
            }
            if (!question.dependency_response.isEmpty() && Collections.disjoint(responded_response_ids, question.dependency_response) ) {
                //Question has dependency on another question response, which user does not fulfill
                continue;
            }

            availableQuestions.add(question);
        }

    return availableQuestions;
    }

    @VisibleForTesting
    public static List<Integer> getRespondedQuestionIds(final List<Response> responses) {
        final List<Integer> respondedQuestions = Lists.newArrayList();

        for (Response response : responses) {
            respondedQuestions.add(response.questionId);
        }

        return respondedQuestions;
    }

    @VisibleForTesting
    public static List<Integer> getRespondedResponseIds(final List<Response> responses) {
        final List<Integer> respondedResponses = Lists.newArrayList();

        for (Response response : responses) {
            respondedResponses.add(response.responseId.get());
        }

        return respondedResponses;
    }

    /*
    Insert questions
     */
    private void saveQuestion(final Long accountId, final Question question, final DateTime today, final DateTime expireDate) { //TODO test monitor
        LOGGER.debug("action=saved_question processor=question_survey account_id={} question_id={} today={} expireDate={}", accountId, question.id, today, expireDate);
        this.questionResponseDAO.insertAccountQuestion(accountId, question.id, today, expireDate);
    }

}
