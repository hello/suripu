package com.hello.suripu.core.processors;

import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kingshy on 9/19/14.
 */
public class QuestionProcessorTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(QuestionProcessorTest.class);

    private final static int CHECK_SKIP_NUM = 5;

    private final DateTime today = DateTime.now().withTimeAtStartOfDay();
    private final static String pillID = "10";
    private final static Long ACCOUNT_ID = 1L;

    private QuestionProcessor questionProcessor;

    @Before
    public void setUp() {
        final List<Question> questions = this.getQuestions();

        final QuestionResponseDAO questionResponseDAO = mock(QuestionResponseDAO.class);
        when(questionResponseDAO.getAllQuestions()).thenReturn(ImmutableList.<Question>of().copyOf(questions));
        questionProcessor = new QuestionProcessor(questionResponseDAO, CHECK_SKIP_NUM);
    }

    private List<Question> getQuestions() {
        final List<Question> questions = new ArrayList<>();

        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final int dependency = 0;
        final int parentId = 0;
        final long accountQId = 0L;

        List<Choice> choices = new ArrayList<>();
        int qid = 1;
        choices.add(new Choice(1, "Hot", qid));
        choices.add(new Choice(2, "Cold", qid));
        choices.add(new Choice(3, "No Effect", qid));
        questions.add(new Question(qid, accountQId,
                "Do you sleep hot or cold", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 2;
        choices.add(new Choice(4, "Snore", qid));
        choices.add(new Choice(5, "Sleep-talk", qid));
        choices.add(new Choice(6, "None", qid));
        questions.add(new Question(qid, accountQId,
                "Do you snore or talk in your sleep", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 3;
        choices.add(new Choice(7, "Yes", qid));
        choices.add(new Choice(8, "Somewhat", qid));
        choices.add(new Choice(9, "No", qid));
        questions.add(new Question(qid, accountQId,
                "Are you a light sleeper?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 4;
        choices.add(new Choice(10, "coffee", qid));
        choices.add(new Choice(11, "tea", qid));
        choices.add(new Choice(12, "red bull", qid));
        choices.add(new Choice(14, "others", qid));
        choices.add(new Choice(15, "none", qid));
        questions.add(new Question(qid, accountQId,
                "Do you drink caffeine?", "EN",
                Question.Type.CHECKBOX,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 5;
        choices.add(new Choice(15, "everyday", qid));
        choices.add(new Choice(16, ">4 times a week", qid));
        choices.add(new Choice(17, ">2 times a week", qid));
        choices.add(new Choice(18, "once a week", qid));
        choices.add(new Choice(19, "nope", qid));
        questions.add(new Question(qid, accountQId,
                "Do you workout?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.ONE_TIME,
                Question.ASK_TIME.ANYTIME,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 10000;
        choices.add(new Choice(23, "great", qid));
        choices.add(new Choice(24, "ok", qid));
        choices.add(new Choice(25, "poor", qid));
        questions.add(new Question(qid, accountQId,
                "How was your sleep?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.MORNING,
                dependency, parentId, now, choices));

        choices.clear();
        qid = 10002;
        choices.add(new Choice(30, "yes", qid));
        choices.add(new Choice(31, "no", qid));
        questions.add(new Question(qid, accountQId,
                "Did you workout today?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.DAILY,
                Question.ASK_TIME.EVENING,
                5, parentId, now, choices));

        choices.clear();
        qid = 10003;
        choices.add(new Choice(32, "great", qid));
        choices.add(new Choice(33, "normal", qid));
        choices.add(new Choice(33, "shitty", qid));
        questions.add(new Question(qid, accountQId,
                "How are you feeling?", "EN",
                Question.Type.CHOICE,
                Question.FREQUENCY.OCCASIONALLY,
                Question.ASK_TIME.AFTERNOON,
                dependency, parentId, now, choices));

        return questions;
    }

    @Test
    public void getOnBoardingQuestions() {
        final int accountAge = 0; // zero-days
        final List<Question> questions = this.questionProcessor.getQuestions(ACCOUNT_ID, accountAge, this.today, 2);

        LOGGER.debug("Questions {}", questions);
        assertThat(questions.size(), is(3));

        for (int i = 0; i < questions.size(); i++) {
            assertThat(questions.get(i).id, is(i+1));
        }
    }

}