package com.hello.suripu.core.db;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.db.mappers.AccountDateMapper;
import com.hello.suripu.core.db.mappers.AccountQuestionMapper;
import com.hello.suripu.core.db.mappers.AccountQuestionResponsesMapper;
import com.hello.suripu.core.db.mappers.QuestionMapper;
import com.hello.suripu.core.db.mappers.RecentResponseMapper;
import com.hello.suripu.core.db.mappers.ResponseMapper;
import com.hello.suripu.core.models.AccountDate;
import com.hello.suripu.core.models.AccountQuestion;
import com.hello.suripu.core.models.AccountQuestionResponses;
import com.hello.suripu.core.models.Question;
import com.hello.suripu.core.models.Response;
import org.joda.time.DateTime;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;

import java.sql.Timestamp;
import java.util.List;

public interface QuestionResponseReadDAO {
    @RegisterMapper(QuestionMapper.class)
    @SqlQuery("SELECT * FROM questions ORDER BY id")
    ImmutableList<Question> getAllQuestions();

    @RegisterMapper(QuestionMapper.class)
    @SqlQuery("SELECT * FROM questions WHERE frequency = 'one_time' ORDER BY id")
    ImmutableList<Question> getBaseQuestions();

    @RegisterMapper(AccountQuestionMapper.class)
    @SqlQuery("SELECT * FROM account_questions AQ WHERE " +
            "account_id = :account_id AND expires_local_utc_ts >= :expiration AND AQ.id NOT IN " +
            "(SELECT account_question_id FROM responses) ORDER BY id DESC")
    ImmutableList<AccountQuestion> getAccountQuestions(@Bind("account_id") long accountId,
                                                       @Bind("expiration") DateTime expiration);

    // TODO need to optimize and create index
    @RegisterMapper(RecentResponseMapper.class)
    @SqlQuery("SELECT R.id AS id, " +
            "Q.account_id AS account_id, " +
            "Q.question_id AS question_id, " +
            "Q.id AS account_question_id, " +
            "R.response_id AS response_id, " +
            "R.skip AS skip, " +
            "Q.created_local_utc_ts AS ask_time, " +
            "R.question_freq AS question_freq " +
            "FROM responses R " +
            "LEFT OUTER JOIN account_questions Q ON R.account_question_id = Q.id " +
            "WHERE Q.account_id = :account_id ORDER BY R.id DESC LIMIT :limit")
    ImmutableList<Response> getLastFewResponses(@Bind("account_id") long accountId,
                                                @Bind("limit") int limit);

    @SqlQuery("SELECT DISTINCT question_id FROM responses WHERE account_id = :account_id AND " +
            "(question_id < :max_base_id OR created >= :one_week_ago)")
    List<Integer> getBaseAndRecentAnsweredQuestionIds(@Bind("account_id") long accountId,
                                                      @Bind("max_base_id") int maxBaseId,
                                                      @Bind("one_week_ago") DateTime oneWeekAgo);

    @SqlQuery("SELECT DISTINCT question_id FROM responses WHERE account_id = :account_id AND " +
            "(question_freq = CAST(:frequency AS FREQUENCY_TYPE) OR created >= :one_week_ago)")
    List<Integer> getBaseAndRecentAnsweredQuestionIds(@Bind("account_id") long accountId,
                                                      @Bind("frequency") String frequency,
                                                      @Bind("one_week_ago") DateTime oneWeekAgo);

    @RegisterMapper(RecentResponseMapper.class)
    @SqlQuery("SELECT R.account_id AS account_id, " +
            "R.question_id AS question_id, " +
            "0 AS response_id, " +
            "R.skip AS skip, " +
            "R.account_question_id AS account_question_id, " +
            "AQ.created_local_utc_ts as ask_time, " +
            "R.question_freq AS question_freq " +
            "FROM responses R " +
            "LEFT OUTER JOIN account_questions AQ ON AQ.id = R.account_question_id " +
            "WHERE R.account_id = :account_id AND " +
            "(question_freq = CAST(:frequency AS FREQUENCY_TYPE) OR R.created >= :one_week_ago) " +
            "ORDER by R.id;")
    ImmutableList<Response> getBaseAndRecentResponses(@Bind("account_id") long accountId,
                                                      @Bind("frequency") String frequency,
                                                      @Bind("one_week_ago") DateTime oneWeekAgo);

    //TODO: optimize
    @SqlQuery("SELECT question_id FROM responses WHERE account_id = :account_id AND question_id <= 3")
    List<Integer> getAnsweredOnboardingQuestions(@Bind("account_id") long accountId);

    @SingleValueResult
    @SqlQuery("SELECT next_ask_time_local_utc FROM account_question_ask_time WHERE account_id = :account_id ORDER BY id DESC LIMIT 1")
    Optional<Timestamp> getNextAskTime(@Bind("account_id") long accountId);

    @RegisterMapper(AccountQuestionResponsesMapper.class)
    @SqlQuery("SELECT Q.*, R.created AS response_created " +
            "FROM account_questions Q " +
            "LEFT OUTER JOIN responses R ON R.account_question_id = Q.id " +
            "WHERE Q.account_id = :account_id AND Q.expires_local_utc_ts >= :expiration ORDER BY Q.id")
    ImmutableList<AccountQuestionResponses> getQuestionsResponsesByDate(@Bind("account_id") long accountId,
                                                                        @Bind("expiration") DateTime expiration);

    @RegisterMapper(ResponseMapper.class)
    @SqlQuery("SELECT R.*, C.response_text FROM responses R " +
            "INNER JOIN response_choices C ON R.response_id = C.id " +
            "WHERE account_id = :account_id AND R.question_id = :question_id ORDER BY id DESC")
    ImmutableList<Response> getAccountResponseByQuestionId(@Bind("account_id") long account_id,
                                                           @Bind("question_id") int question_id);

    @RegisterMapper(ResponseMapper.class)
    @SqlQuery("SELECT R.*, C.response_text FROM responses R " +
            "INNER JOIN response_choices C ON R.response_id = C.id " +
            "WHERE account_id = :account_id AND R.question_id IN (SELECT id FROM questions WHERE category = CAST(:question_category AS question_category)) ORDER BY created DESC")
    ImmutableList<Response> getAccountResponseByQuestionCategoryStr(@Bind("account_id") long account_id,
                                                                    @Bind("question_category") String question_category);

    @RegisterMapper(AccountQuestionMapper.class)
    @SqlQuery("SELECT * FROM account_questions WHERE account_id = :account_id and question_id = :question_id ORDER BY id DESC LIMIT :limit")
    ImmutableList<AccountQuestion> getRecentAskedQuestionByQuestionId (@Bind("account_id") final long account_id,
                                                                       @Bind("question_id") final int question_id,
                                                                       @Bind("limit") final int limit);

    @RegisterMapper(AccountDateMapper.class)
    @SqlQuery("SELECT account_id, DATE_TRUNC('day', created_local_utc_ts) - INTERVAL '1 days' as night FROM account_questions where id IN (select account_question_id from responses where response_id = :response_id) ORDER BY account_id, night DESC")
    ImmutableList<AccountDate> getAccountDatebyResponse (@Bind("response_id") final int response_id);

    @RegisterMapper(AccountQuestionMapper.class)
    @SqlQuery("SELECT * FROM account_questions WHERE account_id = :account_id and question_id = :question_id AND created_local_utc_ts = :created_local_utc_ts")
    ImmutableList<AccountQuestion> getAskedQuestionByQuestionIdCreatedDate (@Bind("account_id") final long account_id,
                                                                            @Bind("question_id") final int question_id,
                                                                            @Bind("created_local_utc_ts") final DateTime created_date);
}
