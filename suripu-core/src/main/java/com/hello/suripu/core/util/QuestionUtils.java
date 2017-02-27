package com.hello.suripu.core.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;

import java.util.Collections;
import java.util.List;

/**
 * Created by jyfan on 5/24/16.
 */
public class QuestionUtils {

    @VisibleForTesting
    public static ImmutableList<Question> sortQuestionByCategory(final List<Question> questionList) {

        Function<Question, Integer> assignWeights = new Function<Question, Integer>() {
            public Integer apply(Question question) {
                if (question.category.equals(QuestionCategory.ONBOARDING)) {
                    return 0;
                } if (question.category.equals(QuestionCategory.ANOMALY_LIGHT)) {
                    return 1;
                } if (question.category.equals(QuestionCategory.DAILY)) {
                    return 2;
                } if (question.category.equals(QuestionCategory.GOAL)) {
                    return 3;
                } if (question.category.equals(QuestionCategory.SURVEY)) {
                    return 4;
                } else {
                    return 5;
                }
            }
        };

        ImmutableList<Question> sortedFiltered = ImmutableList.copyOf(
                Ordering.natural().onResultOf(assignWeights).sortedCopy(questionList));

        return sortedFiltered;
    }

    //TODO:
    public static List<Question> getAvailableQuestion(final List<Response> responses, final List<Question> possibleQuestions) {

        return Lists.newArrayList();
    }

}
