
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

CREATE TYPE INSIGHT_CATEGORY AS ENUM (
    'generic',
    'sleep_hygiene',
    'light',
    'sound',
    'temperature',
    'humidity',
    'air_quality',
    'sleep_duration',
    'time_to_sleep',
    'sleep_time',
    'wake_time',
    'workout',
    'caffeine',
    'alcohol',
    'diet',
    'daytime_sleepiness',
    'daytime_activities',
    'sleep_score',
    'sleep_quality'
);

DROP TABLE info_insight_cards;

CREATE TABLE info_insight_cards (
    id SERIAL PRIMARY KEY,
    category INSIGHT_CATEGORY NOT NULL,
    image_url VARCHAR(256),
    title VARCHAR(256),
    text TEXT,
    created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

GRANT ALL PRIVILEGES ON info_insight_cards TO ingress_user;
GRANT ALL PRIVILEGES ON SEQUENCE info_insight_cards_id_seq TO ingress_user;


-- source: http://sleepfoundation.org/sleep-news/lights-out-good-nights-sleep
INSERT INTO info_insight_cards (category, image_url, title, text)
VALUES
 ---
('light', 'https://s3.amazonaws.com/hello-data/insights_images/light_1.jpeg',
'Effects of light in the environment on your sleep',
'Light influences our internal clock through specialized "light sensitive" cells in the retina of our eyes, which sends messages to the brain that keep us in a 24-hour cycle or the **circadian rhythm**. These are physical, mental and behavioral changes that follow a roughly a 24-hour cycle, responding primarily to light and darkness in our environment.

The increase and decrease of light cues, trigger different chemical reactions our body, causing changes to our physiology and behavior. As evening approaches and the light in our environment recedes, the hormone melatonin begins to rise, the body temperature falls, thus making us less alert and more likely to fall sleep. In the morning light, melatonin levels are low, the body temperature begins to rise, and other chemical shifts, such as a rise in the activating hormone cortisol, then will help us to feel alert and ready for the day.'
),
----
('temperature', '', 'Effects of Temperature on Your Sleep',
'The temperature of your sleeping environment and how comfortable you feel in it effects the quality of sleep because our brain has an ideal **set point** for body temperature that it is trying to achieve, almost like an internal thermostat. If it’s too cold, or too hot, the body struggles to achieve this set point, hence interrupting sleep. The usual recommendation is to keep the bedroom cool, quiet, & dark, between 65°F (18°C) and 72°F (22°C). However, the perfect room temperature level is different for everyone and should be whatever is most comfortable for the sleeper.'
),
----
('sound', '', 'Sound: ambient sound or sudden loud noises',
'Even while you''re asleep, your brain continues to register and process sounds. A loud noise can interrupt sleep causing you to wake up, shift between stages of sleep, or experience a rise or fall in heart rate and blood pressure. Whether sounds disturb your sleep or not depends on factors such as the stage of sleep you''re in, the time of night, and even your own personal feelings about the sounds themselves. Researchers have seen that we are more likely to wake up when a sound is relevant or emotionally charged and has personal significance. If you have difficulty falling asleep or staying asleep, creating a constant ambient sound could help mask activity from inside and outside your environment allowing a more peaceful slumber.'
),
----
('humidity', '', 'Effects of Humidity on Your Sleep',
'Humid heat exposure during night sleep is an important factors effecting sleep because heat stress increases the thermal load during REM (Rapid Eye Movement) sleep, which increases wakefulness. Humid heat exposure increases heat stress because of the difference in the sweat response caused by the humidity. Decreased ambient humidity allows sweat to evaporate, thereby dissipating the heat, whereas increased humidity does not allow the sweat to evaporate, causing the skin to remain wet, causing you to wake up more readily.'
),
----
('air_quality', '', 'Effects of Air Quality',
'Poor air quality causes interruptions in the quality of your sleep, whereas good air quality can improve your sleep. Breathing in contaminants or air pollutants while you are sleeping results in restlessness and a lack of sufficient oxygen into the bloodstream, causing restlessness, excessive daytime sleepiness, lethargy, and confusion.

 The deep breathing that occurs during sleep causes the body to inhale many irritating particles that agitate the respiratory system and cause coughing, sneezing, inflammation of the sinuses, and other sleep disturbing effects. Those with allergies, asthma, or other breathing difficulties find it most bothersome because poor air quality can aggravate the symptoms of these conditions and make sleeping nearly impossible.

 Breathing poor quality air while sleeping can result in clogged sinuses, a stuffy nose, headache, or scratchy throat due to the contaminants breathed in during the night.
'),
----
('sleep_duration', '', 'Sleep Duration: What''s the ideal amount?',
'Most healthy adults, including the elderly need **between 7.5 to 9 hours** of sleep per night to function at their best.  Children need even more (the younger they are, the more they need); for example, current guidelines say school-age children should sleep at least 10 hours per night.  However, there is no "magic number" on the sleep duration that is ideal for everyone.  It is as individual as you are.  Not only do different age groups need different amounts of sleep, but it is unique to the individual.''
),
----
('time_to_sleep', '', 'Sleep Onset',
'*Sleep onset* is the transition from wakefulness into sleep. *Sleep onset insomnia* is trouble falling asleep. *Sleep maintenance insomnia* is trouble staying asleep.

 Sleep onset is different for everyone but if it takes too long or happens immediately then it can be unhealthy and an indication of a sleep disorder like narcolepsy or insomnia. The transition from wakefulness to sleep and vise versa should happen peacefully and gradually as opposed to abruptly. Once you wake up, you should arise immediately as opposed to "snoozing" as this is the best scenario for starting your day. It is important that when your alarm goes off at your selected wake time, you get right up.

 Specialists recommend keeping a sleep log in order to track your success. Sense keeps a record your bedtime and wake time, and notifies you of any changes in your sleep patterns.'
),
----
('sleep_time', '', 'Sleep & Wake Time',
'Both your sleep time and wake time should be **relatively consistent** because the body''s clock gets used to a certain pattern. This helps to keep the body in sync and to ensure the optimal and most restful sleep. You should select a wake time that you can observe every day, including weekends depending on your individual needs and schedule.

 Our bodies follow a circadian rhythm that relies on consistency. There are many things that you do at about the same time every day, sleep should follow suit. Anchoring your wake time in place is a cue to your body about when you should be awake and when you should be asleep. Waking at the same time every day will actually helps you to sleep better at night. This is especially important for people who have difficulties falling or staying asleep, a characteristic of insomnia.
'
),
----
('wake_time', '', 'Sleep & Wake Time',
'Both your sleep time and wake time should be **relatively consistent** because the body''s clock gets used to a certain pattern. This helps to keep the body in sync and to ensure the optimal and most restful sleep. You should select a wake time that you can observe every day, including weekends depending on your individual needs and schedule.

 Our bodies follow a circadian rhythm that relies on consistency. There are many things that you do at about the same time every day, sleep should follow suit. Anchoring your wake time in place is a cue to your body about when you should be awake and when you should be asleep. Waking at the same time every day will actually helps you to sleep better at night. This is especially important for people who have difficulties falling or staying asleep, a characteristic of insomnia.
'
),
----
('workout', '', 'Effects of Working Out on Your Sleep',
'Studies indicate people sleep significantly better and feel more alert during the day if they get **at least 150 minutes** (the national guideline) of **moderate to vigorous** of exercise a week.

 Upwards of a 65% improvement in sleep quality is realized through regular exercise. In addition regular exercise relaxes you, lengthens and deepens sleep and you will feel less sleepy during the day, compared to those with less physical activity. Exercise triggers an increase in body temperature, and the post-exercise drop in temperature may also promote falling asleep.

 Exercise also significantly improves the sleep of people with chronic insomnia by its effects on circadian rhythms, by decreasing arousals, anxiety and depressive symptoms.'
),
----
('caffeine', '', 'Caffeine',
'Sleep is very **much affected by caffeine** intake and there is an association between the daily intake of caffeine, sleep problems and daytime sleepiness. Even a moderate dose of caffeine at bedtime; up to 6 hours prior to bedtime, has a significant effect on sleep disturbance. The most marked effects of caffeine on sleep is prolonged sleep latency, shorter total sleep time, increase in light sleep and shortening of deep sleep time, as well as more frequent awakenings. Specialists recommend *refraining from substantial caffeine* use for a minimum of *6 hours prior to bedtime*.'
),
----
('alcohol', '', 'Alcohol',
'One of the most common misconceptions is that alcohol consumption is a good sleep aid, as it does help induce sleep. However overall, it is **more disruptive to sleep**, causing arousals particularly in the second half of the night. In addition it reduces rapid eye movement (REM) sleep, which is the deepest and most restorative stage of sleep.  Alcohol is a poor choice as a hypnotic because it may lead to alcohol dependence and alcoholism, and may intensify sleep-related breathing disturbances like sleep apnea. Alcohol does not improve sleep quality and actually is a detriment to the quality of sleep.'
),
----
('diet', '', 'Diet',
'Diet has one of the most **significant effects on sleep**, from what you consume before bed (and at what time) to the direct link between sleep apnea (sleep disordered breathing) and obesity. Sleep is in fact inherently linked with how we eat (and how much). Food is also related to sleep by appetite and metabolism. People who don''t get enough sleep are more likely to have bigger appetites due to the fact that their leptin levels (an appetite regulating hormone) fall, promoting appetite increase. Researchers showed that not getting enough sleep actually affects how hard it is to resist unhealthy foods.

 This link between appetite and sleep provides further evidence that sleep and obesity are linked. To top it off, the psychological manifestations of fatigue, sleep and hunger are similar. There is also an association between the number of calories consumed and sleep duration. Those who consumed the most were more likely to have the shortest sleep duration.'
),
----
('daytime_sleepiness', '', 'Daytime Sleepiness',
'*Excessive Daytime sleepiness* (EDS) or feeling tired during the day is characterized by persistent sleepiness, headaches and lack of energy, even after a seemingly prolonged nighttime sleep.

 EDS is related to insufficient sleep time, disrupted sleep, or a shift or disruption in circadian timing, or hypersomnia and an increased need for sleep during a 24-hour period. EDS is a condition encompassing several sleep disorders where increased sleep is a symptom, of another underlying disorder like narcolepsy, sleep apnea or a circadian rhythm disorder, it is a neurological disorder in which there is a sudden recurrent uncontrollable compulsion to sleep. EDS  is also known as narcolepsy. Among adults in the US, about 35 to 40% of the population have excessive daytime sleepiness or have problems with falling asleep or with daytime sleepiness.

 EDS may affect daytime function associated difficulties in attention, reaction time, memory function, and other behavioral impacts. The most common complaints of EDS include forgetfulness, fatigue, mood changes, inattentiveness, or lapses of attention. In general, EDS can be attributed to a variety of behavioral and lifestyle issues.'
),
----
('daytime_activities', '', 'Daytime Activities',
'A growing body of scientific research indicates that **stressful daytime activities** can have profound effects on sleep, including sleep disruptions and insomnia. Seven out of ten adults in the US report experiencing stress or anxiety daily basis, and most say it interferes at least moderately with their lives and gives them trouble sleeping. The majority of adults with a stress-induced sleep problems experience it at least once per week, and more than half experience it at least several times a week.

 Research indicates links between sleep and stress (especially work stress) that highlights the potential for a vicious cycle. Even if you don''t have a lot of events in your life that others would consider stressful, the degree to which you believe that events are stressful leads to insomnia.

 However, it is not only those exposed to extreme amounts of stress (like our military personnel for example), who experience difficulties sleeping. Also anxiety produced from a common act of emotional labor (i.e., faking a smile while at work) is sufficient enough to lead to insomnia.'
),
----
('sleep_quality', '', 'Amount of Movement during Sleep',
'Tossing and turning generally occurs during brief interruptions of sleep during the night. Chemical systems in your brain work to paralyze skeletal muscles during rapid eye movement (REM) sleep, keeping you still at night. But simple factors such as high temperature or light levels can lead to restless sleep. Excessive alcohol, heavy meals, and caffeine before bed can also cause tossing and turning in bed. Lastly, serious conditions such as acid reflux, periodic limb movement, and sleep apnea can contribute to high levels of movement during the night.'
)
;

INSERT INTO info_insight_cards (category, image_url, title, text)
VALUES ('sleep_hygiene', '', 'Healthy Sleep Habits',
'Maintaining good sleep habits is crucial to having regular, good quality sleep. The **most important habit** is to keep a consistent sleep and wake time for every day of the week. Other good habits to cultivate includes:

**Avoid napping** during the day as it may disrupt your sleep time.

**Avoid caffeine** about 3 hours before bedtime. Caffeine stimulates your nervous system, making you more alert and harder to fall asleep. Note that certain energy or soft drinks may also contain caffeine.

**Stay away from stimulants** such as nicotine and alcohol. While you may feel sleepy after consuming alcohol, your sleep might be disrupted at a later time when the effect has worn off.

**Regular exercise** has been shown to promote good sleep. However, vigorous exercise close to bedtime may be too stimulating, try a more gentle exercise such as yoga.

Get sufficient **exposure to natural light**, which helps to maintain a healthy sleep-wake cycle. Go for a 15-minute walk after lunch, or take a coffee break during the afternoon.

**Avoid bright lights** close to bedtime to allow the production of melatonin, a sleep-inducing hormone. The “blue light” from many modern smartphones or tablets has been shown to **interfere with** the body’s melatonin levels. Try not to use such devices in bed.

**Develop a bedtime routine**. Wind down about an hour before bedtime, engage in relaxing activities such as reading, listening to soothing music, take a warm shower or bath.

**Associate your bedroom with sleep**. Avoid non-sleep activities such as watching TV, working, or using the computer when in the bedroom.'
);

--jyfan 7/29/2015 Adding wake_variance insight category

ALTER TYPE insight_category ADD VALUE 'wake_variance';

INSERT INTO info_insight_cards (category, title, text)
VALUES
 ---
('wake_variance',
'Waking up to sleep better',
'The circadian clock is a sequence of timed chemical cues that tells your body when to wind down or wake up. Your ability to fall into quality sleep relies in large part on the strength of these signals. Disruptions to the clock, such as time zone changes or having a variable work schedule, can affect how well you sleep. Much of the clock is determined by genetics — you might just naturally be a “night owl” or “morning lark” that wakes up particularly late or early in the day. Whatever your sleep schedule, keeping it consistent — particularly your wake time — will keep these signals strong, and the quality of your sleep high.'
);

--jyfan 8/10/2015 Fixed typo with wake_variance card. DELETE ME when done

UPDATE info_insight_cards SET text=
'The circadian clock is a sequence of timed chemical cues that tells your body when to wind down or wake up. Your ability to fall into quality sleep relies in large part on the strength of these signals. Disruptions to the clock, such as time zone changes or having a variable work schedule, can affect how well you sleep. Much of the clock is determined by genetics — you might just naturally be a “night owl” or “morning lark” that wakes up particularly late or early in the day. Whatever your sleep schedule, keeping it consistent — particularly your wake time — will keep these signals strong, and the quality of your sleep high.'
WHERE category='wake_variance';

-- end DELETE ME

-- Modify all titles to be title case 08/31/2015

UPDATE info_insight_cards SET title = 'Effects of Ambient Light on Your Sleep' WHERE id = 1;
UPDATE info_insight_cards SET title = 'Sound: Ambient Sound or Sudden Loud Noises' WHERE id = 3;
UPDATE info_insight_cards SET title = 'Sleep Duration: What''s the Ideal Amount?' WHERE id = 6;
UPDATE info_insight_cards SET title = 'Effects of Exercise on Your Sleep' WHERE id = 10;
UPDATE info_insight_cards SET title = 'Waking Up to Sleep Better' WHERE id = 18;

--jyfan 8/25/2015 Adding bed_light_duration insight category

ALTER TYPE insight_category ADD VALUE 'bed_light_duration';

INSERT INTO info_insight_cards (category, title, text)
VALUES
 ---
('bed_light_duration',
'Associating your bedroom with sleep',
'Regularly watching television, using your phone, or engaging in other mentally stimulating activities just before bed can create a subconscious link between your bedroom and staying alert. This often makes it harder to fall asleep when you want to. On the other hand, reserving your bedroom for sleep and intimacy can create a link between your bedroom and sleep, which will help your body fall asleep more easily when it’s time for bed.'
);