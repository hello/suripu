package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.AccountQuestionResponses;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;
import org.apache.commons.collections.map.HashedMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hello.suripu.core.processors.QuestionCoreProcessor.getAnomalyQuestions;
import static com.hello.suripu.core.processors.QuestionCoreProcessor.getDailyQuestions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by jyfan on 2/24/17.
 */
public class QuestionCoreProcessorTest {

    private final int FAKE_ACCOUNT_AGE_NEW = 0;
    private final int FAKE_ACCOUNT_AGE_OLD = 10;
    private final DateTime FAKE_LOCAL_DAY_0 = DateTime.now();

    private final long FAKE_INDEX_LONG = 0L;

    private final long FAKE_USER_ID_0 = 9990L;

    private final long ACCOUNT_QID_FILLER = 99L;
    private final int DEPENDENCY_FILLER = 0;
    private final int PARENT_ID_FILLER = 0;
    private final DateTime DATE_TIME_FILLER_NOW = DateTime.now(DateTimeZone.UTC);
    private final String ENGLISH_STR = "EN";

    private ImmutableList<Question> getMockQuestions() {
        final List<Question> questions = new ArrayList<>();

        final List<Integer> dependency_response_null = Lists.newArrayList();

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
                "daily question type I?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices2, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.DAILY, dependency_response_null);
        questions.add(question2);

        final List<Choice> choices3 = new ArrayList<>();
        choices3.add(new Choice(8, "Proceed", 2));
        choices3.add(new Choice(9, "Proceed", 2));
        choices3.add(new Choice(10, "No", 2));
        choices3.add(new Choice(11, "No", 2));
        final Question question3 = new Question(3, ACCOUNT_QID_FILLER,
                "daily question type II?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices3, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.DAILY, dependency_response_null);
        questions.add(question3);

        final List<Choice> choices4 = new ArrayList<>();
        choices4.add(new Choice(12, "Yes", 2));
        choices4.add(new Choice(13, "No", 2));
        final Question question4 = new Question(4, ACCOUNT_QID_FILLER,
                "question?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices4, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.DEMO, dependency_response_null);
        questions.add(question4);

        final List<Choice> choices5 = new ArrayList<>();
        choices5.add(new Choice(14, "Yes", 2));
        choices5.add(new Choice(15, "No", 2));
        final Question question5 = new Question(5, ACCOUNT_QID_FILLER,
                "anomaly question?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, choices4, AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.ANOMALY_LIGHT, dependency_response_null);
        questions.add(question5);

        return ImmutableList.copyOf(questions);
    }

    private Map<Integer, Question> getMockQuestionIdMapper() {

        final List<Question> allQuestions = getMockQuestions();

        final Map<Integer, Question> allQuestionIdMap = new HashedMap();
        for (Question question : allQuestions) {
            allQuestionIdMap.put(question.id, question);
        }

        return allQuestionIdMap;
    }

    private List<Question> getMockOnboardingQuestions() {
        final List<Question> allQuestions = getMockQuestions();
        final List<Question> onboardingQuestions = Lists.newArrayList();

        for (Question question : allQuestions) {
            if (question.category == QuestionCategory.ONBOARDING) {
                onboardingQuestions.add(question);
            }
        }

        return onboardingQuestions;
    }

    private Integer getMockAnomalyQid() {

        final List<Question> allQuestions = getMockQuestions();

        int anomalyQid = 0;
        for (Question question : allQuestions) {
            if (question.category == QuestionCategory.ANOMALY_LIGHT) {
                anomalyQid = question.id;
                break;
            }
        }

        return anomalyQid;
    }

    private List<Question> getMockDailyQuestions() {

        final List<Question> allQuestions = getMockQuestions();
        final List<Question> dailyQuestions = Lists.newArrayList();

        for (Question question : allQuestions) {
            if (question.category == QuestionCategory.DAILY) {
                dailyQuestions.add(question);
            }
        }

        return dailyQuestions;
    }

    private List<Question> getMockDemoQuestions() {
        final List<Question> allQuestions = getMockQuestions();
        final List<Question> demoQuestions = Lists.newArrayList();

        for (Question question : allQuestions) {
            if (question.category == QuestionCategory.DEMO) {
                demoQuestions.add(question);
            }
        }

        return demoQuestions;
    }


    private ImmutableList<Response> getMockNoOnboardingResponse() {
        final List<Response> noOnboardingResponses = new ArrayList<>();

        return ImmutableList.copyOf(noOnboardingResponses);
    }

    private ImmutableList<Response> getMockNoDemoResponse() {
        final List<Response> noDemoResponses = new ArrayList<>();

        return ImmutableList.copyOf(noDemoResponses);
    }

    private QuestionCoreProcessor setUp() {
        //Building
        QuestionResponseReadDAO mockQuestionResponseReadDAO = Mockito.mock(QuestionResponseReadDAO.class);
        QuestionResponseDAO mockQuestionResponseDAO = Mockito.mock(QuestionResponseDAO.class);

        //Mock survey question one
        final ImmutableList<Question> allQuestions = getMockQuestions();
        Mockito.when(mockQuestionResponseReadDAO.getAllQuestions()).thenReturn(allQuestions);

        //Mock user response 0
        final ImmutableList<Response> noOnboardingResponse = getMockNoOnboardingResponse();
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_0, QuestionCategory.ONBOARDING.toString().toLowerCase())).thenReturn(noOnboardingResponse);
        final ImmutableList<Response> noDemoResponse = getMockNoDemoResponse();
        Mockito.when(mockQuestionResponseReadDAO.getAccountResponseByQuestionCategoryStr(FAKE_USER_ID_0, QuestionCategory.DEMO.toString().toLowerCase())).thenReturn(noDemoResponse);

        final List<AccountQuestion> noQuestionsSaved = Lists.newArrayList();
        for (Question question : allQuestions) {
            Mockito.when(mockQuestionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(FAKE_USER_ID_0, question.id, DATE_TIME_FILLER_NOW)).thenReturn(ImmutableList.copyOf(noQuestionsSaved));
        }

        final QuestionCoreProcessor questionCoreProcessor = QuestionCoreProcessor.create(mockQuestionResponseReadDAO, mockQuestionResponseDAO);
        return questionCoreProcessor;

    }

    @Test
    public void test_initNoException() {
        QuestionResponseReadDAO mockQuestionResponseReadDAO = Mockito.mock(QuestionResponseReadDAO.class);
        QuestionResponseDAO mockQuestionResponseDAO = Mockito.mock(QuestionResponseDAO.class);

        final ImmutableList<Question> allQuestions = getMockQuestions();
        Mockito.when(mockQuestionResponseReadDAO.getAllQuestions()).thenReturn(allQuestions);

        final QuestionCoreProcessor questionCoreProcessor = QuestionCoreProcessor.create(mockQuestionResponseReadDAO, mockQuestionResponseDAO);
    }

    @Test
    public void test_getOnboarding_none() {

        final QuestionCoreProcessor mockQuestionCoreProcessor = setUp();
        final List<Question> availableQuestions = mockQuestionCoreProcessor.getOnboardingQuestions(FAKE_USER_ID_0, FAKE_ACCOUNT_AGE_OLD);

        assertThat(availableQuestions.size(), is(0));
    }

    @Test
    public void test_getOnboarding() {

        final QuestionCoreProcessor mockQuestionCoreProcessor = setUp();
        final List<Question> onboardingQuestions = getMockOnboardingQuestions();
        final List<Question> availableQuestions = mockQuestionCoreProcessor.getOnboardingQuestions(FAKE_USER_ID_0, FAKE_ACCOUNT_AGE_NEW);

        assertThat(availableQuestions.size(), is(onboardingQuestions.size()));
    }

    @Test
    public void test_getAnomalyQuestion_none1() {

        final Map<Integer, Question> allQuestionIdMap = getMockQuestionIdMapper();

        final List<AccountQuestionResponses> todayQuestionResponsesList_noAnomaly = Lists.newArrayList();

        List<Question> anomalyQuestions = getAnomalyQuestions(allQuestionIdMap, todayQuestionResponsesList_noAnomaly);

        assertThat(anomalyQuestions.size(), is(0));
    }

    @Test
    public void test_getAnomalyQuestion_none2() {
        final Map<Integer, Question> allQuestionIdMap = getMockQuestionIdMapper();
        final Integer anomalyQid = getMockAnomalyQid();

        final List<AccountQuestionResponses> todayQuestionResponsesList_anomaly = Lists.newArrayList();
        todayQuestionResponsesList_anomaly.add(new AccountQuestionResponses(FAKE_INDEX_LONG, FAKE_USER_ID_0, anomalyQid, FAKE_LOCAL_DAY_0, Boolean.TRUE, FAKE_LOCAL_DAY_0));

        List<Question> anomalyQuestions = getAnomalyQuestions(allQuestionIdMap, todayQuestionResponsesList_anomaly);

        assertThat(anomalyQuestions.size(), is(0));
    }

    @Test
    public void test_getAnomalyQuestion_yes() {
        final Map<Integer, Question> allQuestionIdMap = getMockQuestionIdMapper();
        final Integer anomalyQid = getMockAnomalyQid();

        final List<AccountQuestionResponses> todayQuestionResponsesList_anomaly = Lists.newArrayList();
        todayQuestionResponsesList_anomaly.add(new AccountQuestionResponses(FAKE_INDEX_LONG, FAKE_USER_ID_0, anomalyQid, FAKE_LOCAL_DAY_0, Boolean.FALSE, FAKE_LOCAL_DAY_0));

        List<Question> anomalyQuestions = getAnomalyQuestions(allQuestionIdMap, todayQuestionResponsesList_anomaly);

        assertThat(anomalyQuestions.size(), is(1));
    }

    @Test
    public void test_dailyQuestion_none2() {

        final Map<Integer, Question> allQuestionIdMap = getMockQuestionIdMapper();
        final List<Question> dailyQuestions = getMockDailyQuestions();

        final List<AccountQuestionResponses> todayQuestionResponsesList = Lists.newArrayList();
        todayQuestionResponsesList.add(new AccountQuestionResponses(FAKE_INDEX_LONG, FAKE_USER_ID_0, dailyQuestions.get(0).id, FAKE_LOCAL_DAY_0, Boolean.TRUE, FAKE_LOCAL_DAY_0));

        List<Question> availableQuestions = getDailyQuestions(allQuestionIdMap, dailyQuestions, todayQuestionResponsesList, FAKE_ACCOUNT_AGE_OLD);

        assertThat(availableQuestions.size(), is(0));
    }

    @Test
    public void test_dailyQuestion_type1() {

        final Map<Integer, Question> allQuestionIdMap = getMockQuestionIdMapper();
        final List<Question> dailyQuestions = getMockDailyQuestions();

        final List<AccountQuestionResponses> todayQuestionResponsesList = Lists.newArrayList();
        todayQuestionResponsesList.add(new AccountQuestionResponses(FAKE_INDEX_LONG, FAKE_USER_ID_0, dailyQuestions.get(0).id, FAKE_LOCAL_DAY_0, Boolean.FALSE, FAKE_LOCAL_DAY_0));

        List<Question> availableQuestions = getDailyQuestions(allQuestionIdMap, dailyQuestions, todayQuestionResponsesList, FAKE_ACCOUNT_AGE_OLD);

        assertThat(availableQuestions.size(), is(1));
        assertThat(availableQuestions.get(0).category, is(QuestionCategory.DAILY));
        assertThat(availableQuestions.get(0).id, is(dailyQuestions.get(0).id));
    }

    @Test
    public void test_dailyQuestion_type2() {

        final Map<Integer, Question> allQuestionIdMap = getMockQuestionIdMapper();
        final List<Question> dailyQuestions = getMockDailyQuestions();

        final List<AccountQuestionResponses> todayQuestionResponsesList = Lists.newArrayList();
        todayQuestionResponsesList.add(new AccountQuestionResponses(FAKE_INDEX_LONG, FAKE_USER_ID_0, dailyQuestions.get(0).id, FAKE_LOCAL_DAY_0, Boolean.FALSE, FAKE_LOCAL_DAY_0));

        List<Question> availableQuestions = getDailyQuestions(allQuestionIdMap, dailyQuestions, todayQuestionResponsesList, FAKE_ACCOUNT_AGE_OLD + 1);

        assertThat(availableQuestions.size(), is(1));
        assertThat(availableQuestions.get(0).category, is(QuestionCategory.DAILY));
        assertThat(availableQuestions.get(0).id, is(dailyQuestions.get(1).id));
    }

    @Test(expected=Exception.class)
    public void test_dailyQuestion_exception() {
        final Map<Integer, Question> allQuestionIdMap = getMockQuestionIdMapper();
        final List<Question> dailyQuestions = Lists.newArrayList(); //No daily questions loaded

        final List<AccountQuestionResponses> todayQuestionResponsesList = Lists.newArrayList();

        List<Question> availableQuestions = getDailyQuestions(allQuestionIdMap, dailyQuestions, todayQuestionResponsesList, FAKE_ACCOUNT_AGE_OLD);
    }

    @Test
    public void test_getDemoQuestions_none() {
        final QuestionCoreProcessor mockQuestionCoreProcessor = setUp();

        final List<Question> noOnboarding = Lists.newArrayList();
        final List<Question> noAnomaly = Lists.newArrayList();
        final List<Question> noDaily = Lists.newArrayList();

        final List<Question> availableQuestions = mockQuestionCoreProcessor.getDemoQuestions(FAKE_USER_ID_0, FAKE_ACCOUNT_AGE_NEW, noOnboarding, noAnomaly, noDaily);

        assertThat(availableQuestions.size(), is(0));
    }

    @Test
    public void test_getDemoQuestions_none2() {
        final QuestionCoreProcessor mockQuestionCoreProcessor = setUp();

        final List<Question> manyOnboarding = Lists.newArrayList(); //Add enough other question types over limit of num allowed
        final List<Question> allQuestions = getMockQuestions();
        for (final Question question : allQuestions) {
            manyOnboarding.add(question);
        }

        final List<Question> noAnomaly = Lists.newArrayList();
        final List<Question> noDaily = Lists.newArrayList();

        final List<Question> availableQuestions = mockQuestionCoreProcessor.getDemoQuestions(FAKE_USER_ID_0, FAKE_ACCOUNT_AGE_OLD, manyOnboarding, noAnomaly, noDaily);

        assertThat(availableQuestions.size(), is(0));

    }

    @Test
    public void test_getDemoQuestions() {
        final QuestionCoreProcessor mockQuestionCoreProcessor = setUp();
        final List<Question> demoQuestions = getMockDemoQuestions();

        final List<Question> noOnboarding = Lists.newArrayList();
        final List<Question> noAnomaly = Lists.newArrayList();
        final List<Question> noDaily = Lists.newArrayList();

        final List<Question> availableQuestions = mockQuestionCoreProcessor.getDemoQuestions(FAKE_USER_ID_0, FAKE_ACCOUNT_AGE_OLD, noOnboarding, noAnomaly, noDaily);

        assertThat(availableQuestions.size(), is(demoQuestions.size()));

    }

    @Test
    public void test_savedQuestionYes() {

        final Long accountQuestionId = 99L;
        final Integer questionId = 1;

        final ImmutableList<AccountQuestionResponses> todayQuestionResponseList = ImmutableList.copyOf(Lists.newArrayList(new AccountQuestionResponses(accountQuestionId, FAKE_USER_ID_0, questionId, FAKE_LOCAL_DAY_0, Boolean.FALSE, FAKE_LOCAL_DAY_0)));
        final Question question = new Question(questionId,
                ACCOUNT_QID_FILLER,
                "Random fake question?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, Lists.newArrayList(), AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.ONBOARDING, Lists.newArrayList());

        final Optional<Long> savedAccountQuestion = QuestionCoreProcessor.savedAccountQuestion(question, todayQuestionResponseList);

        assertThat(savedAccountQuestion.isPresent(), is(Boolean.TRUE));
        assertThat(savedAccountQuestion.get(), is(99L));

        }

    @Test
    public void test_savedQuestionNo() {

        final Long accountQuestionId = 99L;
        final Integer questionId = 1;

        final ImmutableList<AccountQuestionResponses> todayQuestionResponseList = ImmutableList.copyOf(Lists.newArrayList());
        final Question question = new Question(questionId,
                ACCOUNT_QID_FILLER,
                "Random fake question?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, Lists.newArrayList(), AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.ONBOARDING, Lists.newArrayList());

        final Optional<Long> savedAccountQuestion = QuestionCoreProcessor.savedAccountQuestion(question, todayQuestionResponseList);

        assertThat(savedAccountQuestion.isPresent(), is(Boolean.FALSE));

    }

    @Test
    public void test_savedQuestionNo2() {

        final Long accountQuestionId = 99L;
        final Integer questionId1 = 1;
        final Integer questionId2 = 2;

        final ImmutableList<AccountQuestionResponses> todayQuestionResponseList = ImmutableList.copyOf(Lists.newArrayList(new AccountQuestionResponses(accountQuestionId, FAKE_USER_ID_0, questionId1, FAKE_LOCAL_DAY_0, Boolean.FALSE, FAKE_LOCAL_DAY_0)));
        final Question question = new Question(questionId2,
                ACCOUNT_QID_FILLER,
                "Random fake question?", ENGLISH_STR,
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.ANYTIME,
                DEPENDENCY_FILLER, PARENT_ID_FILLER, DATE_TIME_FILLER_NOW, Lists.newArrayList(), AccountInfo.Type.NONE, DATE_TIME_FILLER_NOW,
                QuestionCategory.ONBOARDING, Lists.newArrayList());

        final Optional<Long> savedAccountQuestion = QuestionCoreProcessor.savedAccountQuestion(question, todayQuestionResponseList);

        assertThat(savedAccountQuestion.isPresent(), is(Boolean.FALSE));
    }

}