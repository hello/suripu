

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
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you sleep better when it''s hot or cold',
'EN', 'one_time', 'choice',
'{"Hot", "Cold", "No Effect"}', null, 'anytime', 'sleep_temperature');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you snore in your sleep?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}',  null, 'anytime', 'snore');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Are you a light sleeper?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Somewhat"}', null, 'anytime', 'light_sleeper');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you consume any caffeine drinks?',
'EN', 'one_time', 'checkbox',
'{"Coffee", "Tea", "Others", "None"}', null, 'anytime', 'caffeine');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you workout regularly?',
'EN', 'one_time', 'choice', '{"Everyday", "A few times a week", "Once a week", "None"}',
null, 'anytime', 'workout');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you take naps during the day?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime', 'nap');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you usually have trouble falling asleep?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime', 'trouble_sleeping');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you take any medication to help you fall asleep?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('What activities do you usually undertake before going to bed? (Check all that applies.)',
'EN', 'one_time', 'checkbox',
'{"Reading", "Use tablet/phone/computer", "Watch TV", "Listen to Music", "Others"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you experience an uncomfortable sensation in your legs at night?',
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

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('What is your usual dinner time?',
'EN', 'one_time', 'choice',
'{"5pm or earlier", "6pm", "7pm", "8pm", "9pm or later"}', null, 'anytime', 'eat_late');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('What time do you usually go to bed?',
'EN', 'one_time', 'choice',
'{"Before 8pm", "Between 8 - 10:59pm", "Between 11pm - 12:59am", "1am or later"}', null, 'anytime', 'bedtime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you practice meditation?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you talk in your sleep?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime', 'sleep_talk');


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
VALUES ('Did you consume any of these within 3 hours of going to bed? (Check all that applies)',
'EN', 'occasionally', 'checkbox',
'{"Coffee", "Tea with caffeine", "Alcoholic drinks", "None"}', null, 'evening');

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


-- changes to snore/sleep-talk tables
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
ALTER TABLE questions ADD COLUMN account_info ACCOUNT_INFO_TYPE DEFAULT NULL;

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you talk in your sleep?', 'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime', 'sleep_talk');

INSERT INTO response_choices (question_id, response_text) VALUES (33, 'Yes'), (33, 'No'), (33, 'Sometimes');
UPDATE questions SET responses_ids = '{114, 115, 116}' WHERE id = 33;

UPDATE responses SET question_id = 33, response_id = 114 WHERE question_id = 2 AND response_id = 5;
UPDATE responses SET account_question_id = 0 where question_id = 33;

UPDATE questions SET question_text = 'Do you snore in your sleep?', responses = '{"Yes", "No", "Sometimes"}', account_info = 'snore' WHERE id = 2;

UPDATE response_choices SET response_text = 'Yes' where id = 4;
UPDATE response_choices SET response_text = 'No' where id = 5;
UPDATE response_choices SET response_text = 'Sometimes' where id = 6;

update questions set account_info = 'sleep_temperature' where id = 1;
update questions set account_info = 'light_sleeper' where id = 3;
update questions set account_info = 'caffeine' where id = 4;
update questions set account_info = 'workout' where id = 5;
update questions set account_info = 'nap' where id = 6;
update questions set account_info = 'trouble_sleeping' where id = 7;
update questions set account_info = 'eat_late' where id = 19;
update questions set account_info = 'bedtime' where id = 20;

-- make changes to questions and correct response mapping
DELETE FROM response_choices WHERE id = 12;
DELETE FROM responses WHERE response_id = 12;
UPDATE questions SET responses = '{Coffee,Tea,Others,None}', responses_ids = '{10, 11, 13, 14}' where id = 4;

DELETE FROM response_choices where id = 17;
UPDATE responses set response_id = 16 where response_id = 17;
UPDATE response_choices SET response_text = 'A few times a week' where id = 16;
UPDATE questions SET responses = '{"Everyday", "A few times a week", "Once a week", "None"}', responses_ids = '{15,16,18,19}' where id = 5;

UPDATE questions set question_text = 'Do you experience an uncomfortable sensation in your legs at night?' where id = 10;

-- 20
UPDATE response_choices SET response_text = 'before 8pm' where id = 66;
UPDATE response_choices SET response_text = 'Between 8 - 10:59pm' where id = 67;
UPDATE response_choices SET response_text = 'Between 11pm - 12:59am' where id = 69;
UPDATE response_choices SET response_text = '1am or later' where id = 72;

UPDATE responses SET response_id = 67 where response_id = 68;
UPDATE responses SET response_id = 69 where response_id = 70;
UPDATE responses SET response_id = 69 where response_id = 71;

DELETE FROM response_choices WHERE id IN (68, 70, 71);

UPDATE questions SET responses = '{"Before 8pm", "Between 8 - 10:59pm", "Between 11pm - 12:59am", "1am or later"}', responses_ids = '{66,67,69,72}' where id = 20;

-- 32
UPDATE response_choices SET response_text = 'Alcoholic drinks' WHERE id = 105;
DELETE FROM responses WHERE response_id = 105;

UPDATE responses SET response_id = 105 WHERE response_id >= 106 AND response_id <= 108;
DELETE FROM response_choices WHERE id = 106;
DELETE FROM response_choices WHERE id = 107;

UPDATE response_choices SET response_text = 'None' WHERE id = 108;

UPDATE questions SET responses = '{"Coffee", "Tea with caffeine", "Alcoholic drinks", "None"}', responses_ids = '{103, 104, 105, 108}' WHERE id = 32;

-- #9: activities before bed
UPDATE response_choices SET response_text = 'Use tablet/phone/computer' WHERE id = 30;
UPDATE response_choices SET response_text = 'Reading/Listen to music' WHERE id = 29;
UPDATE responses SET response_id = 29 WHERE response_id = 32;

UPDATE response_choices SET response_text = 'Others' WHERE id = 32;
UPDATE responses SET response_id = 32 WHERE response_id = 33;
UPDATE responses SET response_id = 32 WHERE response_id = 34;

-- do this after checking for dupes
delete from responses where response_id = 34;

UPDATE response_choices SET response_text = 'None of the above' WHERE id = 33;
DELETE FROM response_choices WHERE id = 34;

UPDATE questions set responses = '{"Reading/Listen to music", "Use tablet/phone/computer", "Watch TV", "Others", "None of the above"}',
responses_ids = '{29, 30, 31, 32, 33}' WHERE id = 9;