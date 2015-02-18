package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.AccountQuestionResponses;
import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Created by kingshy on 10/24/14.
 */
public class QuestionProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionProcessor.class);

    private static final int MAX_SKIPS_ALLOWED = 8;
    private static final int NEW_ACCOUNT_AGE = 1; // less than 1 day
    private static final int NEW_USER_NUM_Q = 3; // no. of on-boarding questions
    private static final int OLD_ACCOUNT_AGE = 7; // older accounts are more than this many days

    private final QuestionResponseDAO questionResponseDAO;
    private final int checkSkipsNum;

    private final ListMultimap<Question.FREQUENCY, Integer> availableQuestionIds = ArrayListMultimap.create();
    private final Map<Integer, Question> questionIdMap = new HashMap<>();
    private final Set<Integer> baseQuestionIds = new HashSet<>();

    public static class Builder {
        private QuestionResponseDAO questionResponseDAO;
        private int checkSkipsNum;
        private ListMultimap<Question.FREQUENCY, Integer> availableQuestionIds;
        private Map<Integer, Question> questionIdMap;
        private Set<Integer> baseQuestionIds;

        public Builder withQuestionResponseDAO(final QuestionResponseDAO questionResponseDAO) {
            this.questionResponseDAO = questionResponseDAO;
            return this;
        }

        public Builder withCheckSkipsNum(final int checkSkipsNum) {
            this.checkSkipsNum = checkSkipsNum;
            return this;
        }

        public Builder withQuestions(final QuestionResponseDAO questionResponseDAO) {
            this.availableQuestionIds = ArrayListMultimap.create();
            this.questionIdMap = new HashMap<>();
            this.baseQuestionIds = new HashSet<>();

            final ImmutableList<Question> allQuestions = questionResponseDAO.getAllQuestions();
            for (final Question question : allQuestions) {
                if (question.dependency != 0 || question.askTime == Question.ASK_TIME.AFTERNOON || question.askTime == Question.ASK_TIME.EVENING) {
                    // TODO: implement dependency and asktime
                    // don't show these questions till dependency and asktime is implemented
                    continue;
                }
                this.availableQuestionIds.put(question.frequency, question.id);
                this.questionIdMap.put(question.id, question);
                if (question.frequency == Question.FREQUENCY.ONE_TIME) {
                    baseQuestionIds.add(question.id);
                }
            }

            return this;
        }

        public QuestionProcessor build() {
            return new QuestionProcessor(this.questionResponseDAO, this.checkSkipsNum,
                    this.availableQuestionIds, this.questionIdMap, this.baseQuestionIds);
        }
    }

    public QuestionProcessor(final QuestionResponseDAO questionResponseDAO, final int checkSkipsNum,
                             final ListMultimap<Question.FREQUENCY, Integer> availableQuestionIds,
                             final Map<Integer, Question> questionIdMap,
                             final Set<Integer> baseQuestionIds) {
        this.questionResponseDAO = questionResponseDAO;
        this.checkSkipsNum = checkSkipsNum;
        this.availableQuestionIds.putAll(availableQuestionIds);
        this.questionIdMap.putAll(questionIdMap);
        this.baseQuestionIds.addAll(baseQuestionIds);
    }
    /**
     * Get a list of questions for the user, or pre-generate one
     */
    public List<Question> getQuestions(final Long accountId, final int accountAgeInDays, final DateTime today, final Integer numQuestions, final Boolean checkPause) {

        // brand new user - get on-boarding questions
        if (accountAgeInDays < NEW_ACCOUNT_AGE) {
            return this.getOnBoardingQuestions(accountId, today);
        }

        // check if user has skipped too many questions in the past.
        if (checkPause) {
            final boolean pauseQuestion = this.pauseQuestions(accountId, today);
            if (pauseQuestion) {
                LOGGER.debug("Pause questions for user {}", accountId);
                return Collections.emptyList();
            }
        } else {
            this.resetNextAsk(accountId, today);
        }

        // check if we have already generated a list of questions
        // and if the user has answered any
        final Map<Integer, Question> preGeneratedQuestions = new HashMap<>();

        // grab user question and response status for today if this is not a "get-more questions" request
        final DateTime expiration = today.plusDays(1);
        final ImmutableList<AccountQuestionResponses> questionResponseList = this.questionResponseDAO.getQuestionsResponsesByDate(accountId, expiration);

        // check if we have generated any questions for this user TODAY
        int answered = 0;
        if (questionResponseList.size() != 0) {
            // check number of today's question the user has answered
            for (final AccountQuestionResponses question : questionResponseList) {
                if (question.responded) {
                    answered++;
                    continue;
                }

                // add this unanswered question to list
                final Integer qid = question.questionId;
                if (!preGeneratedQuestions.containsKey(qid)) {
                    final Long accountQId = question.id;
                    preGeneratedQuestions.put(qid, Question.withAskTimeAccountQId(this.questionIdMap.get(qid), accountQId, today));
                }
            }

            if (checkPause && answered >= numQuestions) {
                // user has answered today's quota
                LOGGER.debug("User has answered all questions for today {}", accountId);
                return Collections.emptyList();
            }
        }

        if ((preGeneratedQuestions.size() + answered) >= numQuestions) {
            return new ArrayList<>(preGeneratedQuestions.values());
        }

        // get additional questions if needed
        final Integer getMoreNum = numQuestions - preGeneratedQuestions.size();
        List<Question> questions;

        if (accountAgeInDays < OLD_ACCOUNT_AGE) {
            questions = this.getNewbieQuestions(accountId, today, getMoreNum, preGeneratedQuestions.keySet());
        } else {
            questions = this.getOldieQuestions(accountId, today, getMoreNum, preGeneratedQuestions.keySet());
        }

        if (!preGeneratedQuestions.isEmpty()) {
            questions.addAll(new ArrayList<>(preGeneratedQuestions.values()));
        }

        return questions;
    }

    /**
     * Save response to a question
     */
    public boolean saveResponse(final Long accountId, final int questionId, final Long accountQuestionId, final List<Choice> choices) {

        //check if choice is a valid one before saving to DB
        final Question responseToQuestion = this.questionIdMap.get(questionId);
        int saved = 0;

        for (final Choice choice : choices) {
            if (responseToQuestion.choiceList.contains(choice)) {
                try {
                    this.questionResponseDAO.insertResponse(accountId, questionId, accountQuestionId, choice.id, responseToQuestion.frequency.toSQLString());
                    saved++;
                } catch (UnableToExecuteStatementException exception) {
                    LOGGER.warn("Fail to insert response {} to question {}", choice.id, accountQuestionId);
                }
            } else {
                LOGGER.warn("Account {} response to {} is not a valid choice: {}", accountId, accountQuestionId, choice);
            }
        }

        if (saved != choices.size()) {
            LOGGER.warn("Not all responses saved. Account {}, question {}, {}/{}", accountId, accountQuestionId, saved, choices.size());
            return false;
        }
        return true;
    }

    /**
     * Record a skipped question, set the next ask-time for this user
     */
    public void skipQuestion(final Long accountId, final Integer questionId, final Long accountQuestionId, final int tzOffsetMillis) {

        // save skip if this is a question we've asked
        final Question responseToQuestion = this.questionIdMap.get(questionId);
        final Long insertId = this.questionResponseDAO.insertSkippedQuestion(accountId, questionId, accountQuestionId, responseToQuestion.frequency.toSQLString());

        // determine next time to ask
        if (insertId != null && insertId > 0L) {
            this.setNextAskDate(accountId, tzOffsetMillis);
        }
    }

    public int setNextAskDate (final Long accountId, final int tzOffsetMillis) {
        // find out how many the user has skipped
        final List<Long> skippedIds = this.getConsecutiveSkipsCount(accountId, tzOffsetMillis);

        // set a delay for next ask time, max days to skip is 55 days
        final int skipCount = Math.min(skippedIds.size(), MAX_SKIPS_ALLOWED);
        if (skipCount < 2) {
            return 0;
        }

        final int skipDays = (int) Math.exp((double) skipCount / 2.0);

        LOGGER.debug("User has skipped {} consecutive questions, pause for {} days", skipCount, skipDays);

        final DateTime nextAskDate = DateTime.now(DateTimeZone.UTC).plusMillis(tzOffsetMillis).withTimeAtStartOfDay().plusDays(skipDays);
        this.questionResponseDAO.setNextAskTime(accountId, nextAskDate);

        return skipDays;
    }

    private List<Long> getConsecutiveSkipsCount(final Long accountId, final int tzOffsetMillis) {
        // get last N questions, and check for consecutive skips
        final List<Long> skippedIds = new ArrayList<>();

        final DateTime today = DateTime.now(DateTimeZone.UTC).plusMillis(tzOffsetMillis).withTimeAtStartOfDay();

        final List<Response> responses = this.questionResponseDAO.getLastFewResponses(accountId, this.checkSkipsNum);
        for (final Response response : responses) {
            if (response.skip == null && response.askTime.isEqual(today)) {
                continue;
            }

            Boolean skip = null;
            if (response.skip != null) {
                skip = response.skip.get();
            }

            if (skip != null && skip == false) {
                // not a skip, bolt
                break;
            }
            skippedIds.add(response.accountQuestionId);
        }
        return skippedIds;
    }

    private List<Question> getOnBoardingQuestions(Long accountId, DateTime today) {

        // check if user has already responded to any onboarding questions
        final Set<Integer> answeredIds = new HashSet<>(this.questionResponseDAO.getAnsweredOnboardingQuestions(accountId));

        final List<Question> onboardingQs = new ArrayList<>();
        if (!answeredIds.contains(1)) {
            onboardingQs.add(questionIdMap.get(1));
        }
        if (!answeredIds.contains(2)) {
            onboardingQs.add(questionIdMap.get(2));
        }
        if (!answeredIds.contains(3)) {
            onboardingQs.add(questionIdMap.get(3));
        }

        // None of questions has been answered, insert into DB
        if (onboardingQs.size() == NEW_USER_NUM_Q) {
            try {
                final DateTime expiration = today.plusDays(1);
                this.questionResponseDAO.insertAccountOnBoardingQuestions(accountId, today, expiration); // save to DB
            } catch (UnableToExecuteStatementException exception) {
                final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
                if (matcher.find()) {
                    LOGGER.debug("Onboarding questions already saved");
                }
            }
        }

        return onboardingQs;
    }

    /**
     * Get questions for accounts less than 2 weeks old
     */
    private List<Question> getNewbieQuestions(final Long accountId, final DateTime today, final Integer numQuestions, final Set<Integer> seenIds) {

        final List<Question> questions = new ArrayList<>();

        // from DB, get answered base-questions and other answered questions from the past week
        final Set<Integer> addedIds = this.getUserAnsweredQuestionIds(accountId, today, true);

        // add questions that has already been selected
        addedIds.addAll(seenIds);

        // always include the ONE daily calibration question, most important Q has lower id
        final Integer questionId = this.availableQuestionIds.get(Question.FREQUENCY.DAILY).get(0);
        if (!addedIds.contains(questionId)) {
            addedIds.add(questionId);
            final Long savedID = this.saveGeneratedQuestion(accountId, questionId, today);
            if (savedID > 0L) {
                questions.add(Question.withAskTimeAccountQId(this.questionIdMap.get(questionId), savedID, today));
            }
        }

        // pick some base question, check if we already got responses from all
        final Boolean answeredAllBaseQs = addedIds.containsAll(this.baseQuestionIds);
        if (!answeredAllBaseQs) {
            // randomly pick some base questions
            final List<Question> someQs = this.randomlySelectFromQuestionPool(accountId, addedIds, Question.FREQUENCY.ONE_TIME, today, numQuestions - 1);
            if (someQs.size() > 0) {
                questions.addAll(someQs);
            }
        }

        // pull from ongoing questions pool if we don't have enough
        if (questions.size() < numQuestions) {
            final int additionQuestions = numQuestions - questions.size();
            final List<Question> moreQs = this.randomlySelectFromQuestionPool(accountId, addedIds, Question.FREQUENCY.OCCASIONALLY, today, additionQuestions);
            if (moreQs.size() > 0) {
                questions.addAll(moreQs);
            }
        }

        return questions;
    }

    /** TODO: more logic to be implemented
     - determine ask frequency based on no. of responses from user in the first two weeks
     0 - once a week
     1 - 4 responses (25%) every 4 days
     5 - 9 (60%) every 3 days
     10 - 13 (80%) every 2 days
     14 - 17 (100%) every other day
     - base questions:
     - take weekdays/weekends into account
     - do not repeat base/ongoing questions within 2 ask days
     */
    private List<Question> getOldieQuestions(final Long accountId, final DateTime today, final Integer numQuestions, final Set<Integer> seenIds) {

        final List<Question> questions = new ArrayList<>();

        // from DB, get answered base-questions and other answered questions from the past week
        final Set<Integer> addedIds = this.getUserAnsweredQuestionIds(accountId, today, false);

        // add questions that has already been selected
        addedIds.addAll(seenIds);

        // always choose ONE random daily-question
        List<Question> dailyQs = this.randomlySelectFromQuestionPool(accountId, seenIds, Question.FREQUENCY.DAILY, today, 1);
        if (dailyQs.size() > 0 && !addedIds.contains(dailyQs.get(0).id)) {
            addedIds.add(dailyQs.get(0).id);
            questions.add(dailyQs.get(0));
        }


        // first dib for base-question, randomly choose ONE
        if (questions.size() < numQuestions) {
            final Boolean answeredAll = addedIds.containsAll(this.baseQuestionIds);
            if (!answeredAll) {
                final List<Question> selectedBaseQs = this.randomlySelectFromQuestionPool(accountId, addedIds, Question.FREQUENCY.ONE_TIME, today, 1);
                if (selectedBaseQs.size() > 0) {
                    addedIds.add(selectedBaseQs.get(0).id);
                    questions.add(selectedBaseQs.get(0));
                }

            }
        }

        // pick from ongoing-questions if we need more
        if (questions.size() < numQuestions) {
            final int numToGet = numQuestions - questions.size();
            final List<Question> moreQs= this.randomlySelectFromQuestionPool(accountId, addedIds, Question.FREQUENCY.OCCASIONALLY, today, numToGet);
            if (moreQs.size() > 0) {
                questions.addAll(moreQs);
            }
        }

        return questions;
    }

    private List<Question> randomlySelectFromQuestionPool(final long accountId, final Set<Integer> seenIds,
                                                          final Question.FREQUENCY questionType,
                                                          final DateTime today, final int numQuestions) {

        final List<Question> questions = new ArrayList<>();

        final Set<Integer> addedIds = new HashSet<>();
        addedIds.addAll(seenIds);

        final List<Integer> questionsPool = new ArrayList<>();
        questionsPool.addAll(this.availableQuestionIds.get(questionType));
        questionsPool.removeAll(seenIds);

        int poolSize = questionsPool.size();

        if (poolSize == 0) {
            return questions;
        }

        final int originalPoolSize = poolSize;

        int loop = 0;

        Random rnd = new Random();

        while (questions.size() < numQuestions) {
            final int qid = rnd.nextInt(poolSize); // next random question
            final Question question = this.questionIdMap.get(questionsPool.get(qid));

            if (!addedIds.contains(question.id)) {
                addedIds.add(question.id);
                final Long savedId = this.saveGeneratedQuestion(accountId, question.id, today);
                if (savedId > 0L) {
                    questions.add(Question.withAskTimeAccountQId(question, savedId, today));
                }
                questionsPool.remove(qid);
                poolSize--;
            }

            if (poolSize == 0) {
                break;
            }

            loop++;
            if (loop >= originalPoolSize) {
                break;
            }
        }
        return questions;
    }

    private Long saveGeneratedQuestion(final Long accountId, final Integer id, final DateTime today) {
        try {
            // insert into DB for later retrieval, TODO: make this batch Insert?
            final DateTime expiration = today.plusDays(1);
            return this.questionResponseDAO.insertAccountQuestion(accountId, id, today, expiration);

        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if (matcher.find()) {
                LOGGER.debug("Question already exist");
            }
        }
        return 0L;
    }

    /**
     * Get ids of base questions answered, and recently answered questions (one week)
     */
    private Set<Integer> getUserAnsweredQuestionIds (final Long accountId, final DateTime today, final Boolean newbie) {

        final DateTime oneWeekAgo = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay().minusDays(7);

//        final List<Integer> baseIds = this.questionResponseDAO.getBaseAndRecentAnsweredQuestionIds(accountId, Question.FREQUENCY.ONE_TIME.toSQLString(), oneWeekAgo);
//        final Set<Integer> uniqueIds = new HashSet<>(baseIds);

        final Integer newbieQuestionId = this.availableQuestionIds.get(Question.FREQUENCY.DAILY).get(0);

        final ImmutableList<Response> recentResponses = this.questionResponseDAO.getBaseAndRecentResponses(accountId, Question.FREQUENCY.ONE_TIME.toSQLString(), oneWeekAgo);

        final Set<Integer> uniqueIds = new HashSet<>();
        for (final Response response : recentResponses) {
            if (newbie && response.questionId == newbieQuestionId) {
                // check that we haven't asked this daily question for this user yet
                if (response.askTime.isBefore(today)) {
                    continue;
                }
            }
            uniqueIds.add(response.questionId);
        }

        LOGGER.debug("User {} has seen {} base questions", accountId, recentResponses.size());
        return uniqueIds;
    }

    /**
     * Get questions that were generated earlier and saved to DB, and have not been responded
     */
    private Map<Integer, Question> getPreGeneratedQuestions(final Long accountId, final DateTime today) {
        final DateTime expiration = today.plusDays(1);
        final ImmutableList<AccountQuestion> questionIds = this.questionResponseDAO.getAccountQuestions(accountId, expiration);
        if (questionIds == null || questionIds.size() == 0) {
            return Collections.emptyMap();
        }

        final Map<Integer, Question> questions = new HashMap<>();

        for (final AccountQuestion question : questionIds) {
            final Integer qid = question.questionId;
            if (!questions.containsKey(qid)) {
                final Long accountQId = question.id;
                questions.put(qid, Question.withAskTimeAccountQId(this.questionIdMap.get(qid), accountQId, today));
            }
        }

        return questions;
    }

    /**
     * Check if we need to pause questions for this account
     */
    private boolean pauseQuestions(Long accountId, DateTime today) {
        final Optional<Timestamp> nextAskTimestamp = this.questionResponseDAO.getNextAskTime(accountId);
        if (nextAskTimestamp.isPresent()){
            final DateTime nextAskDatetime = new DateTime(nextAskTimestamp.get(), DateTimeZone.UTC);
            if (nextAskDatetime.isAfter(today)) {
                return true;
            }
        }
        return false;
    }

    private void resetNextAsk(Long accountId, DateTime today) {
        // set next ask into the past.
        final DateTime nextAskDate = today.minusDays(1);
        this.questionResponseDAO.setNextAskTime(accountId, nextAskDate);
    }

}
