package com.hello.suripu.core.processors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hello.suripu.core.db.QuestionResponseDAO;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.db.util.MatcherPatternsDB;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.AccountQuestionResponses;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Questions.QuestionCategory;
import com.hello.suripu.core.models.Response;
import com.hello.suripu.core.util.QuestionUtils;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.exceptions.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by jyfan on 2/23/17.
 *
 * Function:
 * 1. Decides which questions should be surfaced each day. Logic is based only on the category parameter.
 * 2. Saves questions surfaced (exception is pre-populated questions e.g. anomaly which are saved even though they
 * may not be surfaced)
 *
 */
public class QuestionCoreProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuestionCoreProcessor.class);

    private final QuestionResponseReadDAO questionResponseReadDAO;
    private final QuestionResponseDAO questionResponseDAO;

    private Map<Integer, Question> allQuestionIdMap;

    private final List<Question> onboardingQuestions;
    private final List<Question> anomalyQuestions;
    private final List<Question> dailyQuestions;
    private final List<Question> demoQuestions;
    private final List<Question> surveyQuestions;

    private static final int NEW_ACCOUNT_AGE = 1; // less than 1 day
    private static final int MAX_SHOWN_Q = 5;

    public QuestionCoreProcessor(final QuestionResponseReadDAO questionResponseReadDAO,
                                 final QuestionResponseDAO questionResponseDAO,
                                 final Map<Integer, Question> allQuestionIdMap,
                                 final List<Question> onboardingQuestions,
                                 final List<Question> anomalyQuestions,
                                 final List<Question> dailyQuestions,
                                 final List<Question> demoQuestions,
                                 final List<Question> surveyQuestions) {
        this.questionResponseReadDAO = questionResponseReadDAO;
        this.questionResponseDAO = questionResponseDAO;
        this.allQuestionIdMap = allQuestionIdMap;
        this.onboardingQuestions = onboardingQuestions;
        this.anomalyQuestions = anomalyQuestions;
        this.dailyQuestions = dailyQuestions;
        this.demoQuestions = demoQuestions;
        this.surveyQuestions = surveyQuestions;
    }

    /*
    Build processor
     */
    public static class Builder {
        private QuestionResponseReadDAO questionResponseReadDAO;
        private QuestionResponseDAO questionResponseDAO;
        private Map<Integer, Question> allQuestionIdMap;
        private List<Question> onboardingQuestions;
        private List<Question> anomalyQuestions;
        private List<Question> dailyQuestions;
        private List<Question> demoQuestions;
        private List<Question> surveyQuestions;

        public Builder withQuestionResponseDAO(final QuestionResponseReadDAO questionResponseReadDAO,
                                               final QuestionResponseDAO questionResponseDAO) {
            this.questionResponseReadDAO = questionResponseReadDAO;
            this.questionResponseDAO = questionResponseDAO;
            return this;
        }

        public Builder withQuestions(final QuestionResponseReadDAO questionResponseReadDAO) {
            this.allQuestionIdMap = new HashMap<>();
            this.onboardingQuestions = Lists.newArrayList();
            this.anomalyQuestions = Lists.newArrayList();
            this.dailyQuestions = Lists.newArrayList();
            this.demoQuestions = Lists.newArrayList();
            this.surveyQuestions = Lists.newArrayList();

            final List<Question> allQuestions = questionResponseReadDAO.getAllQuestions();
            for (final Question question : allQuestions) {

                this.allQuestionIdMap.put(question.id, question);

                if (question.category == QuestionCategory.ONBOARDING) {
                    this.onboardingQuestions.add(question);
                } if (question.category == QuestionCategory.ANOMALY_LIGHT) {
                    this.anomalyQuestions.add(question);
                } if (question.category == QuestionCategory.DAILY) {
                    this.dailyQuestions.add(question);
                } if (question.category == QuestionCategory.DEMO) {
                    this.demoQuestions.add(question);
                } if (question.category == QuestionCategory.SURVEY) {
                    this.surveyQuestions.add(question);
                }
            }

            if (this.dailyQuestions.size() < 2) { //see getDailyQuestions() logic
                try {
                    throw new Exception("less than 2 daily questions avail: please check postgres");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return this;
        }

        public QuestionCoreProcessor build() {
            checkNotNull(questionResponseReadDAO, "questionResponseRead cannot be null");
            checkNotNull(questionResponseDAO, "questionResponse cannot be null");
            checkNotNull(allQuestionIdMap, "allQuestionIdMapp cannot be null");
            checkNotNull(onboardingQuestions, "onboardingQuestions cannot be null");
            checkNotNull(anomalyQuestions, "anomalyQuestions cannot be null");
            checkNotNull(dailyQuestions, "dailyQuestions cannot be null");
            checkNotNull(demoQuestions, "demoQuestions cannot be null");
            checkNotNull(surveyQuestions, "surveyQuestions cannot be null");

            return new QuestionCoreProcessor(this.questionResponseReadDAO,
                    this.questionResponseDAO,
                    this.allQuestionIdMap,
                    this.onboardingQuestions,
                    this.anomalyQuestions,
                    this.dailyQuestions,
                    this.demoQuestions,
                    this.surveyQuestions);
        }
    }

    /*
    Pick question, combine with output of questionProcessor and sort by question priority
     */
    public List<Question> getQuestions(final Long accountId, final int accountAgeInDays, final DateTime todayLocal) {

        final DateTime expiration = todayLocal.plusDays(1);
        final ImmutableList<AccountQuestionResponses> todayQuestionResponseList = this.questionResponseDAO.getQuestionsResponsesByDate(accountId, expiration); //Gets questions already asked to user & responses, if any

        final List<Question> onboardingQuestions = getOnboardingQuestions(accountId, accountAgeInDays);
        final List<Question> anomalyQuestions = getAnomalyQuestions(this.allQuestionIdMap, todayQuestionResponseList);
        final List<Question> dailyQuestions = getDailyQuestions(this.allQuestionIdMap, this.dailyQuestions, todayQuestionResponseList, accountAgeInDays);
        final List<Question> demoQuestions = getDemoQuestions(accountId, accountAgeInDays, onboardingQuestions, anomalyQuestions, dailyQuestions);

        final List<Question> allQuestions = Lists.newArrayList(onboardingQuestions);
        allQuestions.addAll(anomalyQuestions);
        allQuestions.addAll(dailyQuestions);
        allQuestions.addAll(demoQuestions);

        final ImmutableList<Question> sortedQuestions = QuestionUtils.sortQuestionByCategory(allQuestions);

        //Batch save
        final DateTime expireDate = todayLocal.plusDays(1);
        handleSaveQuestions(accountId, todayLocal, expireDate, sortedQuestions);

        return sortedQuestions;
    }

    /*
    Pick question types
     */
    @VisibleForTesting
    public List<Question> getOnboardingQuestions(final Long accountId, final int accountAgeInDays) {

        if (accountAgeInDays > NEW_ACCOUNT_AGE) {
            return Lists.newArrayList();
        }

        // decide available
        final List<Response> onboardingResponses = questionResponseReadDAO.getAccountResponseByQuestionCategoryStr(accountId, QuestionCategory.ONBOARDING.toString().toLowerCase());
        final List<Question> availableQuestions = QuestionUtils.getAvailableQuestion(onboardingResponses, onboardingQuestions);

        return availableQuestions;
    }

    @VisibleForTesting
    public static List<Question> getAnomalyQuestions(final Map<Integer, Question> allQuestionIdMap, final List<AccountQuestionResponses> todayQuestionResponsesList) {

        // anomaly questions are pre-inserted by the anomaly worker
        // check existence/ answered status
        for (AccountQuestionResponses accountQuestionResponses : todayQuestionResponsesList) {

            final Question questionTemplate = allQuestionIdMap.get(accountQuestionResponses.questionId);
            final Boolean answered = accountQuestionResponses.responded;

            if (questionTemplate.category.equals(QuestionCategory.ANOMALY_LIGHT) & !answered) {
                return Lists.newArrayList(questionTemplate);
            }
        }

        return Lists.newArrayList();
    }

    @VisibleForTesting
    public static List<Question> getDailyQuestions(final Map<Integer, Question> allQuestionIdMap,
                                                   final List<Question> dailyQuestions,
                                                   final List<AccountQuestionResponses> todayQuestionResponsesList,
                                                   final int accountAgeInDays) {

        // check answered status
        for (AccountQuestionResponses accountQuestionResponses : todayQuestionResponsesList) {

            final Question questionTemplate = allQuestionIdMap.get(accountQuestionResponses.questionId);
            final Boolean answered = accountQuestionResponses.responded;

            if (questionTemplate.category.equals(QuestionCategory.DAILY) & answered) { //already answered daily question
                return Lists.newArrayList();
            }
        }

        // decide randomish daily question
        final Boolean evenAccountAge = (accountAgeInDays % 2 == 0);
        if (evenAccountAge) {
            return Lists.newArrayList(dailyQuestions.get(0));
        }

        return Lists.newArrayList(dailyQuestions.get(1));
    }

    @VisibleForTesting
    public List<Question> getDemoQuestions(final Long accountId,
                                           final int accountAgeInDays,
                                           final List<Question> onboardingQuestions,
                                           final List<Question> anomalyQuestions,
                                           final List<Question> dailyQuestions) {

        if (accountAgeInDays <= NEW_ACCOUNT_AGE) {
            return Lists.newArrayList();
        }

        final int numQuestions = onboardingQuestions.size() + anomalyQuestions.size() + dailyQuestions.size();
        if (numQuestions >= MAX_SHOWN_Q) {
            return Lists.newArrayList();
        }

        // decide available
        final List<Response> demoResponses = questionResponseReadDAO.getAccountResponseByQuestionCategoryStr(accountId, QuestionCategory.DEMO.toString().toLowerCase());
        final List<Question> availableQuestions = QuestionUtils.getAvailableQuestion(demoResponses, demoQuestions);

        final int numDemoQuestions = Math.min(MAX_SHOWN_Q - numQuestions, availableQuestions.size());
        return availableQuestions.subList(0, numDemoQuestions);
    }

    /*
    Insert questions if it has not already been inserted
    */
    private Integer handleSaveQuestions(final Long accountId, final DateTime todayLocal, final DateTime expireDate, final List<Question> questions) {

        int saves = 0;

        for (Question question : questions) {

            //TODO: make batch
            final Boolean savedQuestion = savedAccountQuestion(accountId, question, todayLocal);
            if (!savedQuestion) {
                saveQuestion(accountId, question, todayLocal, expireDate);
                saves += 1;
            }
        }

        return saves;
    }

    private void saveQuestion(final Long accountId, final Question question, final DateTime todayLocal, final DateTime expireDate) {
        try {
            LOGGER.debug("action=saved_question processor=question_survey account_id={} question_id={} today_local={} expire_date={}", accountId, question.id, todayLocal, expireDate);
            this.questionResponseDAO.insertAccountQuestion(accountId, question.id, todayLocal, expireDate);
        } catch (UnableToExecuteStatementException exception) {
            final Matcher matcher = MatcherPatternsDB.PG_UNIQ_PATTERN.matcher(exception.getMessage());
            if (!matcher.find()) {
                LOGGER.debug("exception={} account_id={}", exception.toString(), accountId);
            }
        }
    }

    private Boolean savedAccountQuestion(final Long accountId, final Question question, final DateTime created) {
        final List<AccountQuestion> questions = questionResponseReadDAO.getAskedQuestionByQuestionIdCreatedDate(accountId, question.id, created);
        return !questions.isEmpty();
    }
}
