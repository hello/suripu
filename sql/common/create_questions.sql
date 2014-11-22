

--
-- Questions and Responses tables
--

CREATE TYPE RESPONSE_TYPE AS ENUM ('choice', 'checkbox', 'text');

CREATE TYPE ASK_TIME_TYPE AS ENUM ('morning', 'afternoon', 'evening', 'anytime');

CREATE TYPE FREQUENCY_TYPE AS ENUM (
    'one_time',
    'daily', -- up to 7 times per week
    'occasionally', -- up to 3 times a week
    'trigger' -- only when there's a relevant event
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
    created TIMESTAMP default current_timestamp -- question creation date
);

CREATE UNIQUE INDEX uniq_question_text_lang on questions(question_text, lang);

GRANT ALL PRIVILEGES ON questions TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE questions_id_seq TO ingress_user;


-- tracks all the responses
CREATE TABLE response_choices (
  id SERIAL PRIMARY KEY,
  question_id INTEGER,
  response_text TEXT,
  created TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_question_id_response_text on response_choices(question_id, response_text);

GRANT ALL PRIVILEGES ON response_choices TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE response_choices_id_seq TO ingress_user;


-- save pre-generated questions for the a user for a particular day
CREATE TABLE account_questions(
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT,
  question_id INTEGER,
  created_local_utc_ts TIMESTAMP, -- Date question should be shown (Y-m-d in user's local time)
  expires_local_utc_ts TIMESTAMP, -- question expiration date
  created TIMESTAMP default current_timestamp -- when the question was created
);

CREATE UNIQUE INDEX uniq_aq_account_qid_created_ts ON account_questions(account_id, question_id, created_local_utc_ts);
CREATE INDEX aq_account_id ON account_questions(account_id);
CREATE INDEX aq_account_id_expires ON account_questions(account_id, expires_local_utc_ts);

GRANT ALL PRIVILEGES ON account_questions TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_questions_id_seq TO ingress_user;


-- user's responses to our questions
CREATE TABLE responses (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    question_id INTEGER,
    account_question_id BIGINT default 0,
    response_id INTEGER default 0,
    skip BOOLEAN default FALSE,
    question_freq FREQUENCY_TYPE,
    created TIMESTAMP default current_timestamp
);

CREATE UNIQUE INDEX uniq_responses_account_id_question_id ON responses(account_id, account_question_id);
CREATE INDEX responses_account_id ON responses(account_id);
CREATE INDEX responses_account_question_id ON responses(account_question_id);

GRANT ALL PRIVILEGES ON responses TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE responses_id_seq TO ingress_user;

-- TODO: index for responses table

-- set the next time to ask a user question after some skips
-- no entries if the user has not skipped any
CREATE TABLE account_question_ask_time (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT,
    next_ask_time_local_utc TIMESTAMP, -- Date to start asking question again
    created TIMESTAMP default current_timestamp
);

CREATE INDEX aq_ask_time_account_id ON account_question_ask_time(account_id);

GRANT ALL PRIVILEGES ON account_question_ask_time TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE account_question_ask_time_id_seq TO ingress_user;



--
-- Populate questions table
--

--
-- 1. baseline, one-time only questions (reserved first 10K ids for these)
--    * first three questions are asked during onboarding
--
INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (1, 'Do you sleep better when it''s hot or cold',
'EN', 'one_time', 'choice',
'{"Hot", "Cold", "No Effect"}', null, 'anytime');

INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (2, 'Do you snore or talk in your sleep?',
'EN', 'one_time', 'checkbox',
'{"Snore", "Sleep-talk", "None of the above"}',  null, 'anytime');

INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (3, 'Are you a light sleeper?',
'EN', 'one_time', 'choice',
'{"Yes", "Somewhat", "No"}', null, 'anytime');

INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (4, 'Do you consume any caffeinated drink?',
'EN', 'one_time', 'checkbox',
'{"Coffee", "Tea", "Energy drinks", "Others", "None"}', null, 'anytime');

INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (5, 'Do you workout regularly?',
'EN', 'one_time', 'choice', '{"Everyday", "More than 4 times a week", "2 to 4 times a week", "Once a week", "None"}',
null, 'anytime');

INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (6, 'Do you take naps during the day?',
'EN', 'one_time', 'choice',
'{"Yes", "Sometimes", "No"}', null, 'anytime');

--
--Additional Questions
--
--"Do you fall asleep easily"
--'{"Yes", "No", "Somewhat"}'

--"Do you take any medication to help you fall asleep"
--'{"Regularly", "Occasionally", "No"}'

--"What activities do you usually undertake before going to bed?"
--
--"Do you experience an uncomfortable/restless sensation in your legs at night?"
--"How often do you experience insomnia?"
--"Have you been diagnose with any sleep disorders?"

--"Have you ever wake up gasping for breath?"
--
--"Have you ever fallen asleep while driving?"
--
--"Do you feel that you get enough sleep?"
--
--"Are you quick to recover from jet-lag?"
--
--"Do you watch TV in your bedroom?"
--
--"Do you practice meditation?"

-- Diet questions
-- "Are you a vegetarian?"
-- "Do you consume meat in every meal?"

--
-- 2. calibration type questions, asked often
--
INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (10000, 'How was your sleep last night?',
'EN', 'daily', 'choice',
'{"Great", "Okay", "Poor"}', null, 'morning');

--"How generally well do you feel today?"
--'{"Very well", "Fine", "OK", "Not well at all"}'
--
--"How energetic do you feel today?"
--'{"Very energetic", "Somewhat energetic", "OK", "Lethargic"}'


--
-- 3. ongoing questions, asked occasionally
--
INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (10001, 'How many caffeine drinks did you have today BEFORE 5pm?',
'EN', 'occasionally', 'choice',
'{"5 and more", "2 to 4", "just 1", "None"}', 4, 'evening');

'How many caffeine drinks did you have today AFTER 5pm?'


INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (10002, 'Did you workout today?',
'EN', 'occasionally', 'choice',
'{"Yes", "No"}', 6, 'evening');

--"How intense was your workout today?"
--"How long did you workout today?"
--"What time of day did you exercise today?"
--"How many alcoholic drinks did you have today?"
--"Did you have any naps today?"
--"Did you consume any of these within 3 hours of going to bed?"
--"What time did you have dinner today?"
--"Did you have a late, big dinner today?"
--
--



INSERT INTO questions (id, question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES (10003, 'How are you feeling right now?',
'EN', 'occasionally', 'choice',
'{"Great", "Okay", "Tired", "Sleepy", "Stressed", "Terrible"}', null, 'afternoon');


--
-- Populate response_choices table
--

-- here's how we populate response_choices and re-add the ids back to questions table

-- insert all available responses into response_choices
INSERT INTO response_choices (question_id, response_text) (SELECT id, UNNEST(responses) FROM questions);

-- update questions with the ids created above
UPDATE questions
SET responses = subquery.responses, responses_ids = subquery.responses_ids
FROM (SELECT question_id,
    ARRAY_AGG(response_text) AS responses, ARRAY_AGG(id) AS responses_ids
    FROM response_choices WHERE question_id IN (SELECT id FROM questions)
    GROUP BY question_id) AS subquery
WHERE questions.id = subquery.question_id;

