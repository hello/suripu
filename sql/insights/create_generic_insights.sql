
--        GENERIC(0),
--        SLEEP_HYGIENE(1),
--        LIGHT(2),
--        SOUND(3),
--        TEMPERATURE(4),
--        HUMIDITY(5),
--        AIR_QUALITY(6),
--        SLEEP_DURATION(7),
--        TIME_TO_SLEEP(8),
--        SLEEP_TIME(9),
--        WAKE_TIME(10),
--        WORKOUT(11),
--        CAFFEINE(12),
--        ALCOHOL(13),
--        DIET(14),
--        DAYTIME_SLEEPINESS(15),
--        DAYTIME_ACTIVITIES(16),
--        SLEEP_SCORE(17),
--        SLEEP_QUALITY(18);

CREATE TABLE generic_insight_cards (
    id SERIAL PRIMARY KEY,
    category SMALLINT NOT NULL,
    image_url VARCHAR(256),
    text TEXT,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

GRANT ALL PRIVILEGES ON generic_insight_cards TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE generic_insight_cards_id_seq TO ingress_user;


-- source: http://sleepfoundation.org/sleep-news/lights-out-good-nights-sleep
INSERT INTO generic_insight_cards (category, image_url, text)
VALUES (2, 'https://s3.amazonaws.com/hello-data/insights_images/light_1.jpeg',
'A key factor in regulating sleep and your biological clocks is exposure to light or
to darkness so falling asleep with lights on may not be the best thing for a
good night''s sleep.\n\nExposure to light stimulates a nerve pathway from the eye
to parts of the brain that control hormones, body temperature and other functions
that play a role in making us feel sleepy or wide-awake.\n\nToo much light, right
before bedtime may prevent you from getting a good night’s sleep.
In fact, one study recently found that exposure to unnatural light cycles
may have real consequences for our health including increased risk for depression.
Regulating exposure to light is an effective way to keep circadian rhythms in check.\n\n
Setting good sleep habits is particularly important for infants and children , as
it directly impacts mental and physical development. Circadian rhythms develop at
about six weeks, and by three to six months, most infants have a regular sleep-wake cycle.
Learning to work with your body is essential for good health, because every living
creature needs sleep.');

INSERT INTO generic_insight_cards (category, image_url, text)
VALUES (2, 'https://s3.amazonaws.com/hello-data/insights_images/light_1.jpeg',
'To regulate your exposure to light, you can try the following:\n\n
- During the day, find tme for sunlight.\n\n
- At night, keep your sleep environment dark. Light-blocking curtains, drapes or
an eye mask can also help, and if you find yourself waking up in the middle of the
night, avoid as much light as possible by using a low illumination night light.\n\n
- Before bedtime, limit television viewing and computer use, especially in the bedroom,
as they hinder quality sleep.');