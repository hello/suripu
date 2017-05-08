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