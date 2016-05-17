package com.hello.suripu.core.util;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.junit.Test;

import static com.hello.suripu.core.util.QuestionSurveyUtils.getSurveyXQuestion;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by jyfan on 5/16/16.
 */

public class QuestionSurveyUtilsTest {

        private final long ACCOUNT_QID_FILLER = 99L;
        private final int DEPENDENCY_FILLER = 0;
        private final int PARENT_ID_FILLER = 0;
        private final DateTime DATE_TIME_FILLER_NOW = DateTime.now(DateTimeZone.UTC);
        private final long ID_FILLER = 0;
        private final String ENGLISH_STR = "EN";
        private final String RESPONSE_STRING_FILLER = "I feel great";

        private ImmutableList<Question> getMockQuestions() {
//        final List<Question> questions = new ArrayList<>();
            final List<Question> SurveyOneQuestions = new ArrayList<>();

            final List<Integer> dependency_response_null = Lists.newArrayList();
            final List<Integer> dependency_response_level2 = Lists.newArrayList(4,5);
            final List<Integer> dependency_response_level3 = Lists.newArrayList(8,9);

            final List<Choice> choices1 = new ArrayList<>();
            choices1.add(new Choice(1, "Yes", 1));
            choices1.add(new Choice(2, "No", 1));
            choices1.add(new Choice(3, "Maybe so", 1));
            final Question question1 = new Question(1, ACCOUNT_QID_FILLER,
                    "Want a random fake onboarding question?", ENGLISH_STR,
                    Question.Type.CHOICE,
                    Question.FREQUENCY.OCCASIONALLY,
                    Question.ASK_TIME.ANYTIME,
                    DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices1, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                    QuestionCategory.ONBOARDING, dependency_response_null);
//        questions.add(question1);

            final List<Choice> choices2 = new ArrayList<>();
            choices2.add(new Choice(4, "Proceed", 2));
            choices2.add(new Choice(5, "Proceed", 2));
            choices2.add(new Choice(6, "No", 2));
            choices2.add(new Choice(7, "No", 2));
            final Question question2 = new Question(2, ACCOUNT_QID_FILLER,
                    "This is level 1, can we proceed to the level 2 question?", ENGLISH_STR,
                    Question.Type.CHOICE,
                    Question.FREQUENCY.ONE_TIME,
                    Question.ASK_TIME.ANYTIME,
                    DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices2, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                    QuestionCategory.SURVEY_ONE, dependency_response_null);
//        questions.add(question2);
            SurveyOneQuestions.add(question2);

            final List<Choice> choices3 = new ArrayList<>();
            choices3.add(new Choice(8, "Proceed", 2));
            choices3.add(new Choice(9, "Proceed", 2));
            choices3.add(new Choice(10, "No", 2));
            choices3.add(new Choice(11, "No", 2));
            final Question question3 = new Question(3, ACCOUNT_QID_FILLER,
                    "This is the 2nd survey question, can we proceed to the level 3 questions?", ENGLISH_STR,
                    Question.Type.CHOICE,
                    Question.FREQUENCY.ONE_TIME,
                    Question.ASK_TIME.ANYTIME,
                    DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices3, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                    QuestionCategory.SURVEY_ONE, dependency_response_level2);
//        questions.add(question3);
            SurveyOneQuestions.add(question3);

            final List<Choice> choices4 = new ArrayList<>();
            choices4.add(new Choice(12, "Yes", 2));
            choices4.add(new Choice(13, "No", 2));
            final Question question4 = new Question(4, ACCOUNT_QID_FILLER,
                    "This is a level 3 question?", ENGLISH_STR,
                    Question.Type.CHOICE,
                    Question.FREQUENCY.ONE_TIME,
                    Question.ASK_TIME.ANYTIME,
                    DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices4, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                    QuestionCategory.SURVEY_ONE, dependency_response_level3);
//        questions.add(question4);
            SurveyOneQuestions.add(question4);

            final List<Choice> choices5 = new ArrayList<>();
            choices5.add(new Choice(14, "Yes", 2));
            choices5.add(new Choice(15, "No", 2));
            final Question question5 = new Question(5, ACCOUNT_QID_FILLER,
                    "This is a level 3 question", ENGLISH_STR,
                    Question.Type.CHOICE,
                    Question.FREQUENCY.ONE_TIME,
                    Question.ASK_TIME.ANYTIME,
                    DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices5, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                    QuestionCategory.SURVEY_ONE, dependency_response_level3);
//        questions.add(question5);
            SurveyOneQuestions.add(question5);

            final List<Choice> choices6 = new ArrayList<>();
            choices6.add(new Choice(16, "Yes", 2));
            choices6.add(new Choice(17, "No", 2));
            final Question question6 = new Question(6, ACCOUNT_QID_FILLER,
                    "This is a level 3 question?", ENGLISH_STR,
                    Question.Type.CHOICE,
                    Question.FREQUENCY.ONE_TIME,
                    Question.ASK_TIME.ANYTIME,
                    DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices6, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                    QuestionCategory.SURVEY_ONE, dependency_response_level3);
//        questions.add(question6);
            SurveyOneQuestions.add(question6);

            return ImmutableList.copyOf(SurveyOneQuestions);
        }

        @Test
        public void testGetSurveyOneQuestion0() {
            final List<Question> SurveyOneQuestions = getMockQuestions();

            //No survey questions have been asked yet. Should ask level 1 question
            final List<Response> surveyOneResponses = Lists.newArrayList();

            final List<Question> questionServed = getSurveyXQuestion(surveyOneResponses, SurveyOneQuestions);

            assertThat(questionServed.size(), is(1)); //Only one question from survey asked
            assertThat(questionServed.get(0).id, is(2)); //Ask level 1 question
        }

        @Test
        public void testGetSurveyOneQuestion() {
            final List<Question> SurveyOneQuestions = getMockQuestions();

            //Proceed-worthy response to level 1 survey question. Should ask level 2 question
            final List<Response> surveyOneResponses = Lists.newArrayList();
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(4), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

            final List<Question> questionServed = getSurveyXQuestion(surveyOneResponses, SurveyOneQuestions);

            assertThat(questionServed.size(), is(1)); //Only one question from survey asked
            assertThat(questionServed.get(0).id, is(3)); //Ask level 2 question
        }

        @Test
        public void testGetSurveyOneQuestion2() {
            final List<Question> SurveyOneQuestions = getMockQuestions();

            //No Proceed-worthy response to level 1 survey question. Ask no questions
            final List<Response> surveyOneResponses = Lists.newArrayList();
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(6), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

            final List<Question> questionServed = getSurveyXQuestion(surveyOneResponses, SurveyOneQuestions);

            assertThat(questionServed.isEmpty(), is(Boolean.TRUE)); //No questions asked
        }

        @Test
        public void testGetSurveyOneQuestion3() {
            final List<Question> SurveyOneQuestions = getMockQuestions();

            //Proceed-worthy response to level 1 survey question. Proceed-worthy response to level 2 survey question. Should ask level 3 question
            final List<Response> surveyOneResponses = Lists.newArrayList();
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(4), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 3, RESPONSE_STRING_FILLER, Optional.of(9), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

            final List<Question> questionServed = getSurveyXQuestion(surveyOneResponses, SurveyOneQuestions);

            assertThat(questionServed.size(), is(3)); //There are 3 level 3 questions in the mock
            assertThat(Collections.disjoint(Lists.newArrayList(questionServed.get(0).id), Lists.newArrayList(4,5,6)), is(Boolean.FALSE)); //Ask one of the level 3 question
            assertThat(Collections.disjoint(Lists.newArrayList(questionServed.get(1).id), Lists.newArrayList(4,5,6)), is(Boolean.FALSE)); //Ask one of the level 3 question
            assertThat(Collections.disjoint(Lists.newArrayList(questionServed.get(2).id), Lists.newArrayList(4,5,6)), is(Boolean.FALSE)); //Ask one of the level 3 question
        }

        @Test
        public void testGetSurveyOneQuestion4() {
            final List<Question> SurveyOneQuestions = getMockQuestions();

            //Proceed-worthy response to level 1 survey question. No Proceed-worthy response to level 2 survey question. Ask no questions
            final List<Response> surveyOneResponses = Lists.newArrayList();
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(4), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 3, RESPONSE_STRING_FILLER, Optional.of(10), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

            final List<Question> questionServed = getSurveyXQuestion(surveyOneResponses, SurveyOneQuestions);

            assertThat(questionServed.isEmpty(), is(Boolean.TRUE)); //No questions asked
        }

        @Test
        public void testGetSurveyOneQuestion5() {
            final List<Question> SurveyOneQuestions = getMockQuestions();

            //Answered all questions. Ask no more questions
            final List<Response> surveyOneResponses = Lists.newArrayList();
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 3, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 4, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 5, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 6, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
            surveyOneResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 7, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

            final List<Question> questionServed = getSurveyXQuestion(surveyOneResponses, SurveyOneQuestions);

            assertThat(questionServed.isEmpty(), is(Boolean.TRUE)); //No questions asked
        }

}
