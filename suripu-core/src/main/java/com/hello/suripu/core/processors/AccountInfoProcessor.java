package com.hello.suripu.core.processors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hello.suripu.core.db.QuestionResponseDAO;
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

    private static final String TEMPERATURE_NONE = "no effect";

    private final QuestionResponseDAO questionResponseDAO;
    private final ImmutableMap<AccountInfo.Type, Question> infoQuestionMap;


    public static class Builder {
        private QuestionResponseDAO questionResponseDAO;
        private Map<AccountInfo.Type, Question> infoQuestionMap = new HashMap<>();

        public Builder withQuestionResponseDAO(final QuestionResponseDAO questionResponseDAO) {
            this.questionResponseDAO = questionResponseDAO;
            return this;
        }

        public Builder withMapping(final QuestionResponseDAO questionResponseDAO) {
            final List<Question> baseQuestions = questionResponseDAO.getBaseQuestions();
            for (final Question question : baseQuestions) {
                // TODO: refactor this, ¡¡don't use string!!
                final String text = question.text.toLowerCase();
                if (text.contains("hot or cold")) {
                    infoQuestionMap.put(AccountInfo.Type.TEMPERATURE, question);
                } else if (text.contains("snore")) {
                    infoQuestionMap.put(AccountInfo.Type.SNORE, question);
                } else if (text.contains("talk in your sleep")) {
                    infoQuestionMap.put(AccountInfo.Type.SLEEP_TALK, question);
                } else if (text.contains("workout regularly")) {
                    infoQuestionMap.put(AccountInfo.Type.WORKOUT, question);
                }
            }
            return this;
        }

        public AccountInfoProcessor build() {
            return new AccountInfoProcessor(questionResponseDAO, infoQuestionMap);
        }
    }

    public AccountInfoProcessor(final QuestionResponseDAO questionResponseDAO, final Map<AccountInfo.Type, Question> infoQuestionMap) {
        this.questionResponseDAO = questionResponseDAO;
        this.infoQuestionMap = ImmutableMap.copyOf(infoQuestionMap);
    }

    public AccountInfo.SleepTempType checkTemperaturePreference(final Long accountId) {
        final Optional<Response> optionalResponse = getSingleUserResponse(accountId, AccountInfo.Type.TEMPERATURE);
        if(optionalResponse.isPresent()) {
            final String response = optionalResponse.get().response.toLowerCase();
            if (response.equals("hot")) {
                return AccountInfo.SleepTempType.HOT;
            } else if (response.equals("cold")) {
                return AccountInfo.SleepTempType.COLD;
            }
        }
        return AccountInfo.SleepTempType.NONE;
    }

    public Boolean checkUserSnore(final Long accountId) {
        final Optional<Response> optionalResponse = getSingleUserResponse(accountId, AccountInfo.Type.SNORE);
        if(optionalResponse.isPresent()) {
            final String responseText = optionalResponse.get().response;
            if (responseText.equals("snore")) {
                return true;
            }
        }
        return false;
    }

    public Boolean checkUserSleepTalk(final Long accountId) {
        final Optional<Response> optionalResponse = getSingleUserResponse(accountId, AccountInfo.Type.SLEEP_TALK);
        if(optionalResponse.isPresent()) {
            final String responseText = optionalResponse.get().response;
            if (responseText.equals("sleep-talk")) {
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
