

--
-- Questions and Responses tables, in insightsDB
--

-- tables created: questions, response_choices, account_questions, responses, account_question_ask_time

-- Cleanup
--
--DROP TABLE questions, response_choices, account_questions, responses, account_question_ask_time;
--DROP TYPE IF EXISTS RESPONSE_TYPE;
--DROP TYPE IF EXISTS ASK_TIME_TYPE;
--DROP TYPE IF EXISTS FREQUENCY_TYPE;

CREATE TYPE RESPONSE_TYPE AS ENUM ('choice', 'checkbox', 'quantity', 'duration', 'time', 'text');

CREATE TYPE ASK_TIME_TYPE AS ENUM ('morning', 'afternoon', 'evening', 'anytime');

CREATE TYPE FREQUENCY_TYPE AS ENUM (
    'one_time',
    'daily', -- up to 7 times per week
    'occasionally', -- up to 3 times a week
    'trigger' -- only when there's a relevant event
);

CREATE TYPE ACCOUNT_INFO_TYPE AS ENUM(
    'sleep_temperature',
    'workout',
    'snore',
    'sleep_talk',
    'caffeine',
    'light_sleeper',
    'nap',
    'trouble_sleeping',
    'eat_late',
    'bedtime'
);


-- all the questions
CREATE TABLE questions (
    id SERIAL PRIMARY KEY,
    parent_id INTEGER default 0, -- link to parent question in English
    question_text TEXT,
    lang VARCHAR(8), -- question lang 'EN' etc
    frequency FREQUENCY_TYPE, -- how often should the question be asked
    response_type RESPONSE_TYPE, -- type of response
    responses text[],
    responses_ids INTEGER[],
    dependency INTEGER, -- id of question that this depends on
    ask_time ASK_TIME_TYPE, -- when to ask the user this question
    account_info ACCOUNT_INFO_TYPE DEFAULT NULL, -- data related to Account Information
    created TIMESTAMP default current_timestamp -- question creation date
);

CREATE UNIQUE INDEX uniq_question_text_lang on questions(question_text, lang);

GRANT ALL PRIVILEGES ON questions TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE questions_id_seq TO ingress_user;


