package com.hello.suripu.core.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Response;

import java.util.Collections;
import java.util.List;

/**
 * Created by jyfan on 5/16/16.
 */
public class QuestionSurveyUtils {

    @VisibleForTesting
    public static List<Question> getSurveyXQuestion(final List<Response> surveyXResponses, final List<Question> surveyXQuestions) {
        if (surveyXResponses.size() == surveyXQuestions.size()) {
            return Lists.newArrayList();
        }

        final List<Integer> responded_question_ids = getRespondedQuestionIds(surveyXResponses);
        final List<Integer> responded_response_ids = getRespondedResponseIds(surveyXResponses);

        final List<Question> availableQuestions = Lists.newArrayList();
        for (Question question : surveyXQuestions) {

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
}
