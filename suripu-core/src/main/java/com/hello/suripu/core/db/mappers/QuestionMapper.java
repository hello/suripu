package com.hello.suripu.core.db.mappers;

import com.hello.suripu.core.models.Choice;
import com.hello.suripu.core.models.Question;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kingshy on 10/24/14.
 */
public class QuestionMapper implements ResultSetMapper<Question> {
    @Override
    public Question map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Integer[] response_ids = (Integer []) r.getArray("responses_ids").getArray();
        final String[] response_text = (String []) r.getArray("responses").getArray();

        final Integer question_id = r.getInt("id");

        final List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < response_ids.length; i++) {
            choices.add(new Choice(response_ids[i], response_text[i], question_id));
        }

        DateTime createdLocal;
        try {
            int foundCreatedLocal = r.findColumn("nameOfColumn");
            createdLocal = new DateTime(r.getTimestamp("created_local_utc_ts"), DateTimeZone.UTC);
        } catch (SQLException error) {
            createdLocal = DateTime.now(DateTimeZone.UTC);
        }

        final Question question = new Question(question_id,
                r.getString("question_text"),
                r.getString("lang"),
                Question.Type.valueOf(r.getString("response_type").toUpperCase()),
                Question.FREQUENCY.valueOf(r.getString("frequency").toUpperCase()),
                Question.ASK_TIME.valueOf(r.getString("ask_time").toUpperCase()),
                r.getInt("dependency"),
                r.getInt("parent_id"),
                createdLocal,
                choices
        );

        return question;
    }
}
