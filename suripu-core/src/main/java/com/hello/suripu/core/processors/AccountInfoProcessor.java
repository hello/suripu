package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.QuestionResponseReadDAO;
import com.hello.suripu.core.models.AccountInfo;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kingshy on 1/12/15.
 */
public class AccountInfoProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountInfoProcessor.class);

    private final QuestionResponseReadDAO questionResponseDAO;
    private final ImmutableMap<AccountInfo.Type, Question> infoQuestionMap;

    private static final String RESPONSE_DRINKS_COFFEE = "coffee";
    private static final String RESPONSE_YES = "yes";
    private static final String RESPONSE_SOMETIMES = "sometimes";

    public static class Builder {
        private QuestionResponseReadDAO questionResponseDAO;
        private Map<AccountInfo.Type, Question> infoQuestionMap = new HashMap<>();

        public Builder withQuestionResponseDAO(final QuestionResponseReadDAO questionResponseDAO) {
            this.questionResponseDAO = questionResponseDAO;
            return this;
        }

        public Builder withMapping(final QuestionResponseReadDAO questionResponseDAO) {
            final List<Question> baseQuestions = questionResponseDAO.getBaseQuestions();
            for (final Question question : baseQuestions) {
                if (question.accountInfo == AccountInfo.Type.SLEEP_TEMPERATURE) {
                    infoQuestionMap.put(AccountInfo.Type.SLEEP_TEMPERATURE, question);

                } else if (question.accountInfo == AccountInfo.Type.SNORE) {
                    infoQuestionMap.put(AccountInfo.Type.SNORE, question);

                } else if (question.accountInfo == AccountInfo.Type.SLEEP_TALK) {
                    infoQuestionMap.put(AccountInfo.Type.SLEEP_TALK, question);

                } else if (question.accountInfo == AccountInfo.Type.WORKOUT) {
                    infoQuestionMap.put(AccountInfo.Type.WORKOUT, question);

                } else if (question.accountInfo == AccountInfo.Type.CAFFEINE) {
                    infoQuestionMap.put(AccountInfo.Type.CAFFEINE, question);
                }
            }
            return this;
        }

        public AccountInfoProcessor build() {
            return new AccountInfoProcessor(questionResponseDAO, infoQuestionMap);
        }
    }

    public AccountInfoProcessor(final QuestionResponseReadDAO questionResponseDAO, final Map<AccountInfo.Type, Question> infoQuestionMap) {
        this.questionResponseDAO = questionResponseDAO;
        this.infoQuestionMap = ImmutableMap.copyOf(infoQuestionMap);
    }

    /**
     * get user sleeping temperature preference (hot, cold or none)
     * @param accountId
     * @return SleepTempType (HOT, COLD or NONE)
     */
    public AccountInfo.SleepTempType checkTemperaturePreference(final Long accountId) {
        final Optional<Response> optionalResponse = getSingleUserResponse(accountId, AccountInfo.Type.SLEEP_TEMPERATURE);
        if(optionalResponse.isPresent()) {
            final Optional<Integer> optionalResponseId = optionalResponse.get().responseId;
            if (optionalResponseId.isPresent()) {
                final Integer responseId = optionalResponseId.get();
                return AccountInfo.SleepTempType.fromInteger(responseId);
            }
        }
        return AccountInfo.SleepTempType.NONE;
    }

    /**
     * Check if the user snores
     * @param accountId
     * @return boolean
     */
    public Boolean checkUserSnore(final Long accountId) {
        return this.getYesNoResponse(accountId, AccountInfo.Type.SNORE);
    }

    /**
     * Check if the user sleep-talks
     * @param accountId
     * @return boolean
     */
    public Boolean checkUserSleepTalk(final Long accountId) {
        return this.getYesNoResponse(accountId, AccountInfo.Type.SLEEP_TALK);
    }

    public Boolean checkUserIsALightSleeper(final Long accountId) {
        return this.getYesNoResponse(accountId, AccountInfo.Type.LIGHT_SLEEPER);
    }

    public Boolean checkUserDrinksCaffeine(final Long accountId) {
        final Optional<Response> optionalResponse = this.getSingleUserResponse(accountId, AccountInfo.Type.CAFFEINE);
        if(optionalResponse.isPresent()) {
            final String responseText = optionalResponse.get().response;
            if (responseText.equalsIgnoreCase(RESPONSE_DRINKS_COFFEE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * To process "yes", "no", "sometimes" responses and return a boolean result
     * @param accountId Long
     * @param infoType AccountInfo.Type
     * @return true or false
     */
    private Boolean getYesNoResponse(final Long accountId, final AccountInfo.Type infoType) {
        final Optional<Response> optionalResponse = this.getSingleUserResponse(accountId, infoType);
        if(optionalResponse.isPresent()) {
            final String responseText = optionalResponse.get().response;
            //TODO: still easier to compare text response for now
            if (responseText.equalsIgnoreCase(RESPONSE_YES) || responseText.equalsIgnoreCase(RESPONSE_SOMETIMES)) {
                return true;
            }
        }
        return false;

    }

    private Optional<Response> getSingleUserResponse(final Long accountId, final AccountInfo.Type infoType) {
        if (infoQuestionMap.containsKey(infoType)) {
            final Integer question_id = infoQuestionMap.get(infoType).id;
            final ImmutableList<Response> responses = questionResponseDAO.getAccountResponseByQuestionId(accountId, question_id);
            if (responses.size() > 0) {
                return Optional.of(responses.get(0));
            }
        }
        return Optional.absent();
    }
}
