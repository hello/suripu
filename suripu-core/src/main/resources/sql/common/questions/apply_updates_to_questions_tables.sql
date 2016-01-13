-- NOTE!!! Put all updates to questions related tables here
-- changes to current tables.



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

--INSERT INTO response_choices (question_id, response_text) (SELECT id, UNNEST(responses) FROM questions WHERE id IN (SELECT id FROM questions ORDER BY id DESC LIMIT 1));

---- update questions with the right response_ids

--UPDATE questions SET responses = S.texts, responses_ids = S.ids FROM (
--  SELECT question_id, ARRAY_AGG(id) AS ids, ARRAY_AGG(response_text) AS texts
--  FROM response_choices where question_id IN
--  (select id from questions order by id DESC LIMIT 1) GROUP BY question_id) AS S
--WHERE questions.id = S.question_id;

----- END Example




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

-- add new questions
INSERT INTO questions (question_text, lang, frequency, response_type, responses, dependency, ask_time, account_info)
VALUES ('Do you talk in your sleep?', 'EN', 'one_time', 'choice',
'{"Yes", "No", "Sometimes"}', null, 'anytime', 'sleep_talk');

INSERT INTO response_choices (question_id, response_text) VALUES (33, 'Yes'), (33, 'No'), (33, 'Sometimes');
UPDATE questions SET responses_ids = '{114, 115, 116}' WHERE id = 33;

-- update some responses
UPDATE responses SET question_id = 33, response_id = 114 WHERE question_id = 2 AND response_id = 5;
UPDATE responses SET account_question_id = 0 where question_id = 33;

-- update question 2 to snoring only
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

-- grammar!!
update questions SET question_text = 'Have you ever woken up gasping for breath?' where id = 13;
update questions set question_text = 'Have you been diagnosed with any sleep disorders?' where id = 12;


