package com.hello.suripu.core.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.AccountQuestionResponses;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Response;

import java.util.Collections;
import java.util.List;

/**
 * Created by jyfan on 5/16/16.
 */
public class QuestionSurveyUtils {

    @VisibleForTesting
    /*
    Requirements for being available:
    1. User has not responded to this question before
    2. User has responded to other questions with response_ids in this question's dependency_response
     */
    public static List<Question> getSurveyXQuestion(final List<Response> surveyXResponses, final List<Question> surveyXQuestions) {
        if (surveyXResponses.size() == surveyXQuestions.size()) {
            return Lists.newArrayList();
        }

        final List<Integer> responded_question_ids = getRespondedQuestionIds(surveyXResponses);
        final List<Integer> responded_response_ids = getRespondedResponseIds(surveyXResponses);

        final List<Question> availableQuestions = Lists.newArrayList();
        for (final Question question : surveyXQuestions) {

            if (!Collections.disjoint(responded_question_ids, Lists.newArrayList(question.id))) {
                //User already responded to this question
                continue;
            }
            if (!question.dependencyResponse.isEmpty() && Collections.disjoint(responded_response_ids, question.dependencyResponse)) {
                //Question has dependency on another question response, which user does not fulfill
                continue;
            }

            availableQuestions.add(question);
        }

        return availableQuestions;
    }

    /*
    Requirements for being available:
    1. User has not responded to this question today (local date)
     */
    public static List<Question> getDailySurveyXQuestion(final List<AccountQuestionResponses> todaysQuestionResponses, final List<Question> surveyXQuestions) {

        final ImmutableList<AccountQuestionResponses> todaysQuestionResponsesImmutable = ImmutableList.copyOf(todaysQuestionResponses); // http://stackoverflow.com/questions/1998544/method-has-the-same-erasure-as-another-method-in-type
        final List<Integer> responded_question_ids = getRespondedQuestionIds(todaysQuestionResponsesImmutable);

        final List<Question> availableQuestions = Lists.newArrayList();
        for (final Question question : surveyXQuestions) {

            if (!Collections.disjoint(responded_question_ids, Lists.newArrayList(question.id))) {
                //User already responded to this question today
                continue;
            }

            availableQuestions.add(question);
        }

        return availableQuestions;
    }

    @VisibleForTesting
    public static List<Integer> getRespondedQuestionIds(final List<Response> responses) {
        final List<Integer> respondedQuestions = Lists.newArrayList();

        for (final Response response : responses) {
            respondedQuestions.add(response.questionId);
        }

        return respondedQuestions;
    }

    @VisibleForTesting
    public static List<Integer> getRespondedQuestionIds(final ImmutableList<AccountQuestionResponses> accountQuestionResponsesList) {
        final List<Integer> respondedQuestions = Lists.newArrayList();

        for (final AccountQuestionResponses accountQuestionResponses : accountQuestionResponsesList) {
            respondedQuestions.add(accountQuestionResponses.questionId);
        }

        return respondedQuestions;
    }

    @VisibleForTesting
    public static List<Integer> getRespondedResponseIds(final List<Response> responses) {
        final List<Integer> respondedResponses = Lists.newArrayList();

        for (final Response response : responses) {
            respondedResponses.add(response.responseId.get());
        }

        return respondedResponses;
    }

}
