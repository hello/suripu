package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.Response;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jyfan on 5/12/16.
 */
public class QuestionSurveyProcessorTest {

    private final long FAKE_USER_ID_0 = 9990L;
    private final long FAKE_USER_ID_1 = 9991L;
    private final long FAKE_USER_ID_2 = 9992L;
    private final long FAKE_USER_ID_3 = 9993L;
    private final long FAKE_USER_ID_4 = 9994L;
    private final long FAKE_USER_ID_5 = 9995L;

    private final long ACCOUNT_QID_FILLER = 99L;
    private final int DEPENDENCY_FILLER = 0;
    private final int PARENT_ID_FILLER = 0;
    private final DateTime DATE_TIME_FILLER_NOW = DateTime.now(DateTimeZone.UTC);
    private final long ID_FILLER = 0;
    private final String ENGLISH_STR = "EN";
    private final String RESPONSE_STRING_FILLER = "I feel great";

    private ImmutableList<Question> getMockQuestions() {
        final List<Question> questions = new ArrayList<>();

        final List<Integer> dependency_response_null = Lists.newArrayList();
        final List<Integer> dependency_response_level2 = Lists.newArrayList(4, 5);
        final List<Integer> dependency_response_level3 = Lists.newArrayList(8, 9);

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
        questions.add(question1);

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
                QuestionCategory.SURVEY, dependency_response_null);
        questions.add(question2);

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
                QuestionCategory.SURVEY, dependency_response_level2);
        questions.add(question3);

        final List<Choice> choices4 = new ArrayList<>();
        choices4.add(new Choice(12, "Yes", 2));
        choices4.add(new Choice(13, "No", 2));
        final Question question4 = new Question(4, ACCOUNT_QID_FILLER,
                "This is a level 3 question?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices4, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.SURVEY, dependency_response_level3);
        questions.add(question4);

        final List<Choice> choices5 = new ArrayList<>();
        choices5.add(new Choice(14, "Yes", 2));
        choices5.add(new Choice(15, "No", 2));
        final Question question5 = new Question(5, ACCOUNT_QID_FILLER,
                "This is a level 3 question", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices5, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.SURVEY, dependency_response_level3);
        questions.add(question5);

        final List<Choice> choices6 = new ArrayList<>();
        choices6.add(new Choice(16, "Yes", 2));
        choices6.add(new Choice(17, "No", 2));
        final Question question6 = new Question(6, ACCOUNT_QID_FILLER,
                "This is a level 3 question?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices6, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.SURVEY, dependency_response_level3);
        questions.add(question6);

        return ImmutableList.copyOf(questions);
    }

    private ImmutableList<Response> getMockResponseUser0() {
        final List<Response> surveyResponses = new ArrayList<>();

        return ImmutableList.copyOf(surveyResponses);
    }

    private ImmutableList<Response> getMockResponseUser1() {
        final List<Response> surveyResponses = new ArrayList<>();
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(4), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

        return ImmutableList.copyOf(surveyResponses);
    }

    private ImmutableList<Response> getMockResponseUser2() {
        final List<Response> surveyResponses = Lists.newArrayList();
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(6), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

        return ImmutableList.copyOf(surveyResponses);
    }

    private ImmutableList<Response> getMockResponseUser3() {
        final List<Response> surveyResponses = Lists.newArrayList();
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(4), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 3, RESPONSE_STRING_FILLER, Optional.of(9), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

        return ImmutableList.copyOf(surveyResponses);
    }

    private ImmutableList<Response> getMockResponseUser4() {
        final List<Response> surveyResponses = Lists.newArrayList();
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(4), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 3, RESPONSE_STRING_FILLER, Optional.of(10), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

        return ImmutableList.copyOf(surveyResponses);
    }

    private ImmutableList<Response> getMockResponseUser5() {
        final List<Response> surveyResponses = Lists.newArrayList();
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 2, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 3, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 4, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 5, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 6, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));
        surveyResponses.add(new Response(ID_FILLER, ACCOUNT_QID_FILLER, 7, RESPONSE_STRING_FILLER, Optional.of(0), Optional.of(Boolean.FALSE), DATE_TIME_FILLER_NOW, ACCOUNT_QID_FILLER, Optional.of(Question.FREQUENCY.ONE_TIME), DATE_TIME_FILLER_NOW));

        return ImmutableList.copyOf(surveyResponses);
    }

    private QuestionSurveyProcessor setUp() {
        //Building
        QuestionResponseReadDAO mockQuestionResponseReadDAO = Mockito.mock(QuestionResponseReadDAO.class);
        QuestionResponseDAO mockQuestionResponseDAO = Mockito.mock(QuestionResponseDAO.class);

        //Mock survey question one
        final ImmutableList<Question> allQuestions = getMockQuestions();
        Mockito.when(mockQuestionResponseReadDAO.getAllQuestions()).thenReturn(allQuestions);

        //Mock user response
        final ImmutableList<Response> user0_response = getMockResponseUser0();
        final ImmutableList<Response> user1_response = getMockResponseUser1();
        final ImmutableList<Response> user2_response = getMockResponseUser2();
        final ImmutableList<Response> user3_response = getMockResponseUser3();
        final ImmutableList<Response> user4_response = getMockResponseUser4();
        final ImmutableList<Response> user5_response = getMockResponseUser5();
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_0, QuestionCategory.SURVEY.toString().toLowerCase())).thenReturn(user0_response);
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_1, QuestionCategory.SURVEY.toString().toLowerCase())).thenReturn(user1_response);
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_2, QuestionCategory.SURVEY.toString().toLowerCase())).thenReturn(user2_response);
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_3, QuestionCategory.SURVEY.toString().toLowerCase())).thenReturn(user3_response);
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_4, QuestionCategory.SURVEY.toString().toLowerCase())).thenReturn(user4_response);
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_5, QuestionCategory.SURVEY.toString().toLowerCase())).thenReturn(user5_response);

        final List<AccountQuestion> noQuestionsSaved = Lists.newArrayList();
        for (Question question : allQuestions) {
            Mockito.when(mockQuestionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(FAKE_USER_ID_0, question.id, DATE_TIME_FILLER_NOW)).thenReturn(ImmutableList.copyOf(noQuestionsSaved));
            Mockito.when(mockQuestionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(FAKE_USER_ID_1, question.id, DATE_TIME_FILLER_NOW)).thenReturn(ImmutableList.copyOf(noQuestionsSaved));
            Mockito.when(mockQuestionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(FAKE_USER_ID_2, question.id, DATE_TIME_FILLER_NOW)).thenReturn(ImmutableList.copyOf(noQuestionsSaved));
            Mockito.when(mockQuestionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(FAKE_USER_ID_3, question.id, DATE_TIME_FILLER_NOW)).thenReturn(ImmutableList.copyOf(noQuestionsSaved));
            Mockito.when(mockQuestionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(FAKE_USER_ID_4, question.id, DATE_TIME_FILLER_NOW)).thenReturn(ImmutableList.copyOf(noQuestionsSaved));
            Mockito.when(mockQuestionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(FAKE_USER_ID_5, question.id, DATE_TIME_FILLER_NOW)).thenReturn(ImmutableList.copyOf(noQuestionsSaved));
        }


        final QuestionSurveyProcessor questionSurveyProcessor = new QuestionSurveyProcessor.Builder()
                .withQuestionResponseDAO(mockQuestionResponseReadDAO, mockQuestionResponseDAO)
                .withQuestions(mockQuestionResponseReadDAO)
                .build();

        return questionSurveyProcessor;

        //Test user setup
    }

    /*
    Tests for spec'ed logic for serving survey one questions, dependant on user's previous responses. questionSurveyProcessor.getQuestions() should always serve only 1 question.
     */

    @Test
    public void test_getQuestions_user0() {
        final QuestionSurveyProcessor mockQuestionSurveyProcessor = setUp();
        //No survey questions have been asked yet. Should ask level 1 question
        final List<Question> servedQuestions = mockQuestionSurveyProcessor.getQuestions(FAKE_USER_ID_0, 2, DATE_TIME_FILLER_NOW);

        assertThat(servedQuestions.size(), is(1)); //Only one question from survey asked
        assertThat(servedQuestions.get(0).id, is(2)); //Ask level 1 question
    }

    @Test
    public void test_getQuestions_user1() {
        final QuestionSurveyProcessor mockQuestionSurveyProcessor = setUp();

        //Proceed-worthy response to level 1 survey question. Should ask level 2 question
        final List<Question> servedQuestions = mockQuestionSurveyProcessor.getQuestions(FAKE_USER_ID_1, 2, DATE_TIME_FILLER_NOW);

        assertThat(servedQuestions.size(), is(1)); //Only one question from survey asked
        assertThat(servedQuestions.get(0).id, is(3)); //Ask level 2 question
    }

    @Test
    public void test_getQuestions_user2() {
        final QuestionSurveyProcessor mockQuestionSurveyProcessor = setUp();

        //No Proceed-worthy response to level 1 survey question. Ask no questions
        final List<Question> servedQuestions = mockQuestionSurveyProcessor.getQuestions(FAKE_USER_ID_2, 2, DATE_TIME_FILLER_NOW);

        assertThat(servedQuestions.isEmpty(), is(Boolean.TRUE)); //No questions asked
    }

    @Test
    public void test_getQuestions_user3() {
        final QuestionSurveyProcessor mockQuestionSurveyProcessor = setUp();

        //Proceed-worthy response to level 1 survey question. Proceed-worthy response to level 2 survey question. Should ask level 3 question
        final List<Question> servedQuestions = mockQuestionSurveyProcessor.getQuestions(FAKE_USER_ID_3, 2, DATE_TIME_FILLER_NOW);

        assertThat(servedQuestions.size(), is(1)); //Only one question from survey asked
        assertThat(Collections.disjoint(Lists.newArrayList(servedQuestions.get(0).id), Lists.newArrayList(4,5,6)), is(Boolean.FALSE)); //Ask one of the level 3 question
    }

    @Test
    public void test_getQuestions_user4() {
        final QuestionSurveyProcessor mockQuestionSurveyProcessor = setUp();

        //Proceed-worthy response to level 1 survey question. No Proceed-worthy response to level 2 survey question. Ask no questions
        final List<Question> servedQuestions = mockQuestionSurveyProcessor.getQuestions(FAKE_USER_ID_4, 2, DATE_TIME_FILLER_NOW);

        assertThat(servedQuestions.isEmpty(), is(Boolean.TRUE)); //No questions asked
    }

    @Test
    public void test_getQuestions_user5() {
        final QuestionSurveyProcessor mockQuestionSurveyProcessor = setUp();

        //Answered all questions. Ask no more questions
        final List<Question> servedQuestions = mockQuestionSurveyProcessor.getQuestions(FAKE_USER_ID_5, 2, DATE_TIME_FILLER_NOW);

        assertThat(servedQuestions.isEmpty(), is(Boolean.TRUE)); //No questions asked
    }

}