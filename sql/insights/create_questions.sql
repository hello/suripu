

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
VALUES ('Do you feel like you sleep better when it''s hot or cold?',
'EN', 'one_time', 'choice',
'{"Hot", "Cold", "Not Sure"}', null, 'anytime', 'sleep_temperature');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you snore in your sleep?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}',  null, 'anytime', 'snore');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Are you a light sleeper?',
'EN', 'one_time', 'choice',
'{"Yes", "Somewhat", "No"}', null, 'anytime', 'light_sleeper');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you drink any caffeinated drinks?',
'EN', 'one_time', 'checkbox',
'{"Coffee", "Tea", "Others", "None"}', null, 'anytime', 'caffeine');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you exercise?',
'EN', 'one_time', 'choice', '{"Everyday", "A few times a week", "Once a week", "No"}',
null, 'anytime', 'workout');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you take naps during the day?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime', 'nap');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you usually have trouble falling asleep?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime', 'trouble_sleeping');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you take any medication to help you fall asleep?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('What activities do you usually do before going to bed?',
'EN', 'one_time', 'checkbox',
'{"Read a book", "Listen to Music", "Use tablet/phone/computer", "Watch TV", "None of the above"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you ever experience an uncomfortable sensation in your legs at night?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How often do you experience insomnia?',
'EN', 'one_time', 'choice',
'{"Frequently", "Occasionally", "Rarely", "Never"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Have you been diagnosed with any sleep disorders?',
'EN', 'one_time', 'choice',
'{"Yes", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Have you ever woken up gasping for breath?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Have you ever fallen asleep while driving?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime');

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
'{"Most nights", "Occasionally", "Never"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you practice meditation?',
'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('What is your usual dinner time?',
'EN', 'one_time', 'choice',
'{"Before 6PM", "Between 6PM and 8PM", "After 8PM"}', null, 'anytime', 'eat_late');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('What time do you usually go to bed?',
'EN', 'one_time', 'choice',
'{"Before 8PM", "Between 8PM and 11PM", "After 11PM but before 1AM", "After 1AM"}', null, 'anytime', 'bedtime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you talk in your sleep?',
'EN', 'one_time', 'choice',
'{"Regularly", "Occasionally", "No"}', null, 'anytime', 'sleep_talk');


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
'{"Great", "Good", "Okay", "Not so good"}', null, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How are you feeling right now?',
'EN', 'occasionally', 'choice',
'{"Great", "Good", "Okay", "Not so good"}', null, 'afternoon');


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
'{"Energetic", "Somewhat energetic", "Lethargic"}', null, 'afternoon');

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



----- Example to create new question

--INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
--VALUES (
--  'No! Try not. Do, or do not. There is no try.', -- text
--  'EN', -- lang
--  'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
--  'choice', -- response_type
--  '{"OK Jedi Master!", "I have a bad feeling about this."}', -- text responses
--  null, -- dependency
--  'anytime' -- ask_time
--);

---- insert the response text into response_choices

--INSERT INTO response_choices (question_id, response_text)
--    (SELECT id, UNNEST(responses) FROM questions WHERE id IN (SELECT id FROM questions ORDER BY id DESC LIMIT 1));

---- update questions with the right response_ids

--UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
--  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
--  FROM response_choices where question_id IN
--  (select id from questions order by id DESC LIMIT 1) GROUP BY question_id) AS S
--WHERE questions.id = S.question_id;


-- Add new "category" field to question table to facilitate trigger-questions
CREATE TYPE QUESTION_CATEGORY AS ENUM (
  'none',
  'onboarding',
  'base',
  'daily',
  'anomaly_light'
);

ALTER TABLE questions ADD COLUMN category QUESTION_CATEGORY DEFAULT 'none';

-- update category
UPDATE questions SET category='onboarding' WHERE id = 1;
UPDATE questions SET category='onboarding' WHERE id = 2;
UPDATE questions SET category='onboarding' WHERE id = 3;


-- New question for light-anomaly-detection results 2016-01-29
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
VALUES (
  'Was the light in your bedroom different than normal last night?', -- text
  'EN', -- lang
  'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
  'choice', -- response_type
  '{"Yes, it was different", "No, it wasn''t different", "I''m not sure"}', -- text responses
  null, -- dependency
  'anytime', -- ask_time
  'anomaly_light' -- trigger by light
);

---- insert the response text into response_choices
INSERT INTO response_choices (question_id, response_text)
    (SELECT id, UNNEST(responses) FROM questions WHERE id IN (SELECT id FROM questions ORDER BY id DESC LIMIT 1));

---- update questions with the right response_ids

UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
  FROM response_choices where question_id IN
  (select id from questions order by id DESC LIMIT 1) GROUP BY question_id) AS S
WHERE questions.id = S.question_id;

---- update light-anomaly-detection questions
UPDATE questions
SET question_text='Was your sleep or nighttime routine last night different from usual?'
WHERE category='anomaly_light';

---- New questions for goals 1 go outside 2016-03-22
ALTER TYPE question_category ADD VALUE 'goal_go_outside';

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How often did you spend 15 min outside?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"4 days or more", "1 day or more", "I didn''t"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'goal_go_outside' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'Was having a weekly goal helpful?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Yes", "No, not helpful"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'goal_go_outside' --category
  );

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How was your sleep last week?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Good, as usual", "Better", "The same"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'goal_go_outside' --category
  );

INSERT INTO response_choices (question_id, response_text)
    (SELECT id, UNNEST(responses) FROM questions WHERE id IN (SELECT id FROM questions ORDER BY id DESC LIMIT 3));

UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
  FROM response_choices where question_id IN
  (select id from questions order by id DESC LIMIT 3) GROUP BY question_id) AS S
WHERE questions.id = S.question_id;

---- New questions for goals 2016-04-15

--Changing responses for previously created question
UPDATE questions SET responses='{"Good", "Okay", "Poor"}' WHERE question_text='How was your sleep last week?' AND lang='EN';
UPDATE response_choices SET response_text='Good' WHERE response_text='Good, as usual' AND question_id=(SELECT id FROM questions WHERE question_text='How was your sleep last week?');
UPDATE response_choices SET response_text='Okay' WHERE response_text='Better' AND question_id=(SELECT id FROM questions WHERE question_text='How was your sleep last week?');
UPDATE response_choices SET response_text='Poor' WHERE response_text='The same' AND question_id=(SELECT id FROM questions WHERE question_text='How was your sleep last week?');

ALTER TYPE question_category ADD VALUE 'goal';
UPDATE questions SET category='goal' WHERE category='goal_go_outside';

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'Did you change your behavior based on last week''s goal?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Yes", "No"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'goal' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How often did you meet this week''s goal?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"4 days or more", "1 day or more", "I didn''t"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'goal' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How did last week''s goal affect your sleep?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"It got better", "No improvement", "Not relevant"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'goal' --category
);

INSERT INTO response_choices (question_id, response_text)
    (SELECT id, UNNEST(responses) FROM questions WHERE id IN (SELECT id FROM questions ORDER BY id DESC LIMIT 3));

UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
  FROM response_choices where question_id IN
  (select id from questions order by id DESC LIMIT 3) GROUP BY question_id) AS S
WHERE questions.id = S.question_id;

--- Update question text grammar consistency
UPDATE questions SET question_text='How often did you meet last week''s goal?' WHERE question_text='How often did you meet this week''s goal?' AND lang='EN';

-- jyfan 2016-06-03
ALTER TABLE questions ADD COLUMN dependency_response INTEGER[] DEFAULT '{}';
UPDATE questions SET dependency_response='{}' WHERE dependency_response IS null;

ALTER TYPE question_category ADD VALUE 'survey';

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How satisfied are you with your sleep?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Very satisfied", "Satisfied", "Moderately satisfied", "Dissatisfied", "Very dissatisfied"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'survey' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'Are you worried or distressed about sleep problems?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Not at all", "A little", "Somewhat", "Very", "Extremely"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'survey' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How much difficulty, if any, do you have falling asleep?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"None", "Mild", "Moderate", "Severe", "Very Severe"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'survey' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How much difficulty, if any, do you have staying asleep?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"None", "Mild", "Moderate", "Severe", "Very Severe"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'survey' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How often do you find yourself waking earlier than you should?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"None", "Mild", "Moderate", "Severe", "Very Severe"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'survey' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'If you struggle with sleep, how much do these problems interfere with the rest of your day?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Not at all", "A little", "Somewhat", "Very", "Extremely"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'survey' --category
);

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'If you struggle with sleep, how noticeable do you think the effects of these problems are to other people?', -- text
      'EN', -- lang
      'trigger', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Not at all", "A little", "Somewhat", "Very", "Extremely"}', --text responses
      null, -- dependency
      'anytime', -- ask_time
      'survey' --category
);

INSERT INTO response_choices (question_id, response_text)
    (SELECT id, UNNEST(responses) FROM questions WHERE id IN (SELECT id FROM questions ORDER BY id DESC LIMIT 7));

UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
  FROM response_choices where question_id IN
  (select id from questions order by id DESC LIMIT 7) GROUP BY question_id) AS S
WHERE questions.id = S.question_id;

-- dependency for survey 1 level 2 ask
UPDATE questions SET dependency_response=(SELECT ARRAY_AGG(id) FROM response_choices WHERE id IN (
    SELECT id FROM response_choices WHERE response_text=ANY(ARRAY['Moderately satisfied', 'Dissatisfied', 'Very dissatisfied'])
                                    AND question_id=(SELECT id FROM questions WHERE question_text='How satisfied are you with your sleep?')))
    WHERE question_text='Are you worried or distressed about sleep problems?';

-- dependency for survey 1 level 3 ask
UPDATE questions SET dependency_response=(SELECT ARRAY_AGG(id) FROM response_choices WHERE id IN (
    SELECT id FROM response_choices WHERE response_text=ANY(ARRAY['A little', 'Somewhat', 'Very', 'Extremely'])
                                    AND question_id=(SELECT id FROM questions WHERE question_text='Are you worried or distressed about sleep problems?')))
    WHERE question_text=ANY(ARRAY['If you struggle with sleep, how much do these problems interfere with the rest of your day?',
                                  'If you struggle with sleep, how noticeable do you think the effects of these problems are to other people?',
                                  'How often do you find yourself waking earlier than you should?',
                                  'How much difficulty, if any, do you have staying asleep?',
                                  'How much difficulty, if any, do you have falling asleep?']);

-- jyfan update question category for daily question for prioritization
UPDATE questions SET category='daily' WHERE question_text='How was your sleep last night?';

-- jyfan 2016-06-08 copy editing
UPDATE questions SET question_text='Did you consume any of these within 3 hours of going to bed?' WHERE question_text='Did you consume any of these within 3 hours of going to bed? (Check all that applies)';

-- jyfan 2016-06-20 fix survey response
UPDATE questions SET responses='{"Never", "Occasionally", "Somewhat often", "Often", "Very often"}' WHERE question_text='How often do you find yourself waking earlier than you should?';
UPDATE response_choices SET response_text='Never' WHERE response_text='None' AND question_id = (SELECT id FROM questions WHERE question_text='How often do you find yourself waking earlier than you should?');
UPDATE response_choices SET response_text='Occasionally' WHERE response_text='Mild' AND question_id = (SELECT id FROM questions WHERE question_text='How often do you find yourself waking earlier than you should?');
UPDATE response_choices SET response_text='Somewhat often' WHERE response_text='Moderate' AND question_id = (SELECT id FROM questions WHERE question_text='How often do you find yourself waking earlier than you should?');
UPDATE response_choices SET response_text='Often' WHERE response_text='Severe' AND question_id = (SELECT id FROM questions WHERE question_text='How often do you find yourself waking earlier than you should?');
UPDATE response_choices SET response_text='Very often' WHERE response_text='Very Severe' AND question_id = (SELECT id FROM questions WHERE question_text='How often do you find yourself waking earlier than you should?');

-- jyfan 2016-08-31 logic change
UPDATE response_choices SET response_text='Very severe' WHERE response_text='Very Severe';

UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
  FROM response_choices where question_id IN
  (SELECT DISTINCT question_id FROM response_choices WHERE response_text='Very severe') GROUP BY question_id) AS S
WHERE questions.id = S.question_id;

UPDATE questions SET question_text='Are you worried about your sleep?' WHERE question_text='Are you worried or distressed about sleep problems?';
UPDATE questions SET question_text='How noticeable do you think the effects of your difficulty sleeping are to other people?' WHERE question_text='If you struggle with sleep, how noticeable do you think the effects of these problems are to other people?';
UPDATE questions SET question_text='How much does your difficulty sleeping interfere with the rest of your day? (e.g. daytime fatigue, mood, concentration, memory, mood, etc.)' WHERE question_text='If you struggle with sleep, how much do these problems interfere with the rest of your day?';

UPDATE questions SET dependency_response=(SELECT ARRAY_AGG(id) FROM response_choices WHERE id IN (
    SELECT id FROM response_choices WHERE response_text=ANY(ARRAY['Moderately satisfied', 'Dissatisfied', 'Very dissatisfied'])
                                    AND question_id=(SELECT id FROM questions WHERE question_text='How satisfied are you with your sleep?')))
    WHERE question_text=ANY(ARRAY['How much difficulty, if any, do you have falling asleep?',
                                  'How much difficulty, if any, do you have staying asleep?',
                                  'How often do you find yourself waking earlier than you should?']);

UPDATE questions SET dependency_response=(SELECT ARRAY_AGG(id) FROM response_choices WHERE id IN (
    SELECT id FROM response_choices WHERE (response_text=ANY(ARRAY['Very severe', 'Severe', 'Very often', 'Often'])
                                        AND (question_id IN (
                                            SELECT id FROM questions WHERE question_text='How much difficulty, if any, do you have falling asleep?' OR
                                            question_text='How often do you find yourself waking earlier than you should?' OR
                                            question_text='How much difficulty, if any, do you have staying asleep?'))))
                                        )
    WHERE question_text=ANY(ARRAY['Are you worried about your sleep?']);

UPDATE questions SET dependency_response=(SELECT ARRAY_AGG(id) FROM response_choices WHERE id IN (
    SELECT id FROM response_choices WHERE response_text=ANY(ARRAY['Somewhat', 'Very', 'Extremely'])
                                    AND question_id=(SELECT id FROM questions WHERE question_text='Are you worried about your sleep?')))
    WHERE question_text=ANY(ARRAY['How noticeable do you think the effects of your difficulty sleeping are to other people?',
                                  'How much does your difficulty sleeping interfere with the rest of your day? (e.g. daytime fatigue, mood, concentration, memory, mood, etc.)']);


-- jyfan 2016-09-20 survey text tweak Matt
UPDATE questions SET question_text='How often do you find yourself waking earlier than you want to?' WHERE question_text='How often do you find yourself waking earlier than you should?';

-- jyfan 2017-02-28 add demo category
ALTER TYPE question_category ADD VALUE 'demo';

UPDATE questions SET category='demo' WHERE (category='none' AND frequency='one_time');

-- jyfan 2017-02-28 add another daily question
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, category)
  VALUES (
      'How refreshed do you feel?', -- text
      'EN', -- lang
      'daily', -- frequency (note, trigger is currently not implemented in QuestionProcessor)
      'choice', --response_type,
      '{"Not at all", "A little bit", "Very much", "Completely"}', --text responses
      null, -- dependency
      'morning', -- ask_time
      'daily' --category
);

INSERT INTO response_choices (question_id, response_text)
    (SELECT id, UNNEST(responses) FROM questions WHERE id IN (SELECT id FROM questions ORDER BY id DESC LIMIT 1));

UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
  FROM response_choices where question_id IN
  (select id from questions order by id DESC LIMIT 1) GROUP BY question_id) AS S
WHERE questions.id = S.question_id;













