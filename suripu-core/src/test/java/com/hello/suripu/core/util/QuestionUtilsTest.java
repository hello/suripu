package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jyfan on 5/24/16.
 */
public class QuestionUtilsTest {
    
    private final long ACCOUNT_QID_FILLER = 99L;
    private final int DEPENDENCY_FILLER = 0;
    private final int PARENT_ID_FILLER = 0;
    private final DateTime DATE_TIME_FILLER_NOW = DateTime.now(DateTimeZone.UTC);
    private final String ENGLISH_STR = "EN";
    final List<Integer> DEPENDENCY_RESPONSE_NULL = Lists.newArrayList();

    @Test
    public void testSort() {
        final List<Question> questions = new ArrayList<>();

        final List<Choice> choices1 = new ArrayList<>();
        choices1.add(new Choice(1, "Something", 1));
        choices1.add(new Choice(2, "Something", 1));
        final Question question1 = new Question(1, ACCOUNT_QID_FILLER,
                "Me first", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices1, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.ONBOARDING, DEPENDENCY_RESPONSE_NULL);

        final List<Choice> choices2 = new ArrayList<>();
        choices2.add(new Choice(3, "Something", 2));
        choices2.add(new Choice(4, "Something", 2));
        final Question question2 = new Question(2, ACCOUNT_QID_FILLER,
                "Me second", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices2, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.ANOMALY_LIGHT, DEPENDENCY_RESPONSE_NULL);


        final List<Choice> choices3 = new ArrayList<>();
        choices3.add(new Choice(5, "Something", 2));
        choices3.add(new Choice(6, "Something", 2));
        final Question question3 = new Question(3, ACCOUNT_QID_FILLER,
                "Me third", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices3, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.DAILY, DEPENDENCY_RESPONSE_NULL);


        final List<Choice> choices4 = new ArrayList<>();
        choices4.add(new Choice(7, "Something", 2));
        choices4.add(new Choice(8, "Something", 2));
        final Question question4 = new Question(4, ACCOUNT_QID_FILLER,
                "Me fourth", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices4, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.SURVEY, DEPENDENCY_RESPONSE_NULL);

        //Mix up order of questions
        questions.add(question4);
        questions.add(question2);
        questions.add(question1);
        questions.add(question3);

        //Sort questions
        final ImmutableList<Question> sortedList = QuestionUtils.sortQuestionByCategory(questions);

        assertThat(sortedList.get(0).category, is(QuestionCategory.ONBOARDING));
        assertThat(sortedList.get(1).category, is(QuestionCategory.ANOMALY_LIGHT));
        assertThat(sortedList.get(2).category, is(QuestionCategory.DAILY));
        assertThat(sortedList.get(3).category, is(QuestionCategory.SURVEY));
    }
    
}
