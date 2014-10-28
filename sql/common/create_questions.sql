--
-- Populate questions table
--


--
-- 1. baseline, one-time only questions
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
VALUES ('Do you consume any caffeinated drink?',
'EN', 'one_time', 'checkbox',
'{"Coffee", "Tea", "Energy drinks", "Others", "None"}', null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you workout regularly?',
'EN', 'one_time', 'choice',
'{"Everyday", "More than 4 times a week", "2 to 4 times a week", "Once a week", "None"}',
null, 'anytime');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Do you take naps during the day?',
'EN', 'one_time', 'choice',
'{"Yes", "Sometimes", "No"}', null, 'anytime');


--
-- 2. calibration type questions, asked often
--
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How was your sleep last night?',
'EN', 'daily', 'choice',
'{"Great", "Okay", "Poor"}', null, 'morning');


--
-- 3. ongoing questions, asked occasionally
--
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How many caffeinated drinks have you taken today?',
'EN', 'occasionally', 'choice',
'{"5 and more", "2 to 4", "just 1", "None"}', 4, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('Did you workout today?',
'EN', 'occasionally', 'choice',
'{"Yes", "No"}', 6, 'evening');

INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time)
VALUES ('How are you feeling right now?',
'EN', 'occasionally', 'choice',
'{"Great", "Okay", "Tired", "Sleepy", "Stressed", "Terrible"}', null, 'afternoon');


--
-- Populate response_choices table
--

-- insert all available responses into response_choices
INSERT INTO response_choices (question_id, response_text) (SELECT id, unnest(responses) from questions);

-- update questions with the ids created above
UPDATE questions
SET responses = subquery.responses, responses_ids = subquery.responses_ids
FROM (SELECT question_id,
    array_agg(response_text) AS responses, array_agg(id) AS responses_ids FROM response_choices
    WHERE question_id IN (SELECT id FROM questions)
    GROUP BY question_id) AS subquery
WHERE questions.id = subquery.question_id;
