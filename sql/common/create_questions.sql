

--
-- Questions and Responses tables
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

CREATE UNIQUE INDEX uniq_responses_account_question_response_id ON responses(account_id, account_question_id, response_id);
CREATE INDEX responses_account_id ON responses(account_id);
CREATE INDEX responses_account_question_id ON responses(account_question_id);

GRANT ALL PRIVILEGES ON responses TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE responses_id_seq TO ingress_user;


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
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you sleep better when it''s hot or cold',
'EN', 'one_time', 'choice',
'{"Hot", "Cold", "No Effect"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you snore or talk in your sleep?',
'EN', 'one_time', 'checkbox',
'{"Snore", "Sleep-talk", "None of the above"}',  null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Are you a light sleeper?',
'EN', 'one_time', 'choice',
'{"Yes", "Somewhat", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you consume any caffeine drinks?',
'EN', 'one_time', 'checkbox',
'{"Coffee", "Tea", "Energy drinks", "Others", "None"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you workout regularly?',
'EN', 'one_time', 'choice', '{"Everyday", "More than 4 times a week", "2 to 4 times a week", "Once a week", "None"}',
null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you take naps during the day?',
'EN', 'one_time', 'choice',
'{"Yes", "Sometimes", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you usually have trouble falling asleep?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you take any medication to help you fall asleep?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('What activities do you usually undertake before going to bed? (Check all that applies.)',
'EN', 'one_time', 'checkbox',
'{"Reading", "Use tablet/phone/computers", "Watch TV", "Listen to Music", "Chatting", "Working"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you experience an uncomfortable/restless sensation in your legs at night?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How often do you experience insomnia?',
'EN', 'one_time', 'choice',
'{"Frequently", "Occasionally", "Rarely", "Never"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Have you been diagnose with any sleep disorders?',
'EN', 'one_time', 'choice',
'{"Yes", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Have you ever wake up gasping for breath?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Have you ever fallen asleep while driving?',
'EN', 'one_time', 'choice',
'{"Yes", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you feel that you get enough sleep?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Are you quick to recover from jet-lag?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you watch TV in your bedroom?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you practice meditation?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('What is your usual dinner time?',
'EN', 'one_time', 'choice',
'{"5pm or earlier", "6pm", "7pm", "8pm", "9pm or later"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('What time do you usually go to bed?',
'EN', 'one_time', 'choice',
'{"8pm or earlier", "9pm", "10pm", "11pm", "12am", "1am", "2am or later"}', null, 'anytime');


--
-- 2. daily calibration type questions, asked often
--
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How was your sleep last night?',
'EN', 'daily', 'choice',
'{"Great", "Okay", "Poor"}', null, 'morning');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How are you feeling today?',
'EN', 'daily', 'choice',
'{"Great", "Fine", "Okay", "Not good"}', null, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How are you feeling right now?',
'EN', 'occasionally', 'choice',
'{"Great", "Fine", "Okay", "Not good"}', null, 'afternoon');


--
-- 3. ongoing questions, asked occasionally
--

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Did you workout today?',
'EN', 'occasionally', 'choice',
'{"Yes", "No"}', 5, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How intense was your workout today?',
'EN', 'occasionally', 'choice',
'{"Easy", "Moderate", "Vigorous", "Very vigorous"}', 5, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('What time of day was your workout today?',
'EN', 'occasionally', 'choice',
'{"Morning", "Afternoon", "Evening", "Did not workout"}', 5, 'evening');


INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Are you feeling sleepy?',
'EN', 'occasionally', 'choice',
'{"Yes", "No", "A little"}', null, 'afternoon');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How energetic do you feel today?',
'EN', 'daily', 'choice',
'{"Very energetic", "Somewhat energetic", "OK", "Lethargic"}', null, 'afternoon');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How are you feeling right now?',
'EN', 'occasionally', 'choice',
'{"Great", "Fine", "Okay", "Not good"}', null, 'afternoon');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Did you have any naps today?',
'EN', 'occasionally', 'choice',
'{"Yes", "No"}', 6, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Did you have a late, big dinner today?',
'EN', 'occasionally', 'choice',
'{"Yes", "No"}', null, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Did you consume any of these within 3 hours of going to bed?',
'EN', 'occasionally', 'checkbox',
'{"Coffee", "Tea with caffeine", "Milk", "Wine", "Beer", "Cocktails", "Hard Liquor"}', null, 'evening');

--
-- Questions requiring quantities
--
--INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
--VALUES ('How many caffeine drinks did you have today BEFORE 5pm?',
--'EN', 'occasionally', 'quantity',
--'{"number", "0"}', 4, 'evening');
--
--INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
--VALUES ('How many caffeine drinks did you have today AFTER 5pm?',
--'EN', 'occasionally', 'quantity',
--'{"number", "0"}', 4, 'evening');
--
--INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
--VALUES ('How long did you workout today?',
--'EN', 'occasionally', 'duration',
--'{"mins", "15"}', 5, 'evening');
--
--INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
--VALUES ('How many alcoholic drinks did you have today?',
--'EN', 'occasionally', 'quantity',
--'{"number", "0"}', null, 'evening');
--
--INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
--VALUES ('What time did you have dinner today?',
--'EN', 'occasionally', 'time',
--'{"time", "17:00"}', null, 'evening');

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

