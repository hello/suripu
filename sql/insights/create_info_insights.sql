
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

CREATE TABLE info_insight_cards (
    id SERIAL PRIMARY KEY,
    category SMALLINT NOT NULL,
    image_url VARCHAR(256),
    title VARCHAR(256),
    text TEXT,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

GRANT ALL PRIVILEGES ON info_insight_cards TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE info_insight_cards_id_seq TO ingress_user;


-- source: http://sleepfoundation.org/sleep-news/lights-out-good-nights-sleep
INSERT INTO info_insight_cards (category, image_url, title, text)
VALUES (2, 'https://s3.amazonaws.com/hello-data/insights_images/light_1.jpeg',
'Effects of light in the environment on your sleep',
'Light influences our internal clock through specialized "light sensitive" cells in the retina of our eyes,
which sends messages to the brain that keep us in a 24-hour cycle or the **circadian rhythm**.
These are physical, mental and behavioral changes that follow a roughly a 24-hour cycle,
responding primarily to light and darkness in our environment.\n\nThe increase and decrease of light cues,
trigger different chemical reactions our body, causing changes to our physiology and behavior. As evening
approaches and the light in our environment recedes, the hormone melatonin begins to rise,
the body temperature falls, thus making us less alert and more likely to fall sleep.
In the morning light, melatonin levels are low, the body temperature begins to rise, and other
chemical shifts, such as a rise in the activating hormone cortisol, then will help us to feel alert
and ready for the day.'
);
