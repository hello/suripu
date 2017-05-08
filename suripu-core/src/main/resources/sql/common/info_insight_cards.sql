
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

--jyfan 8/10/2015 Fix typo with wake_variance card.

UPDATE info_insight_cards SET text=
'The circadian clock is a sequence of timed chemical cues that tells your body when to wind down or wake up. Your ability to fall into quality sleep relies in large part on the strength of these signals. Disruptions to the clock, such as time zone changes or having a variable work schedule, can affect how well you sleep. Much of the clock is determined by genetics — you might just naturally be a “night owl” or “morning lark” that wakes up particularly late or early in the day. Whatever your sleep schedule, keeping it consistent — particularly your wake time — will keep these signals strong, and the quality of your sleep high.'
WHERE category='wake_variance';

--jyfan 8/25/2015 Adding bed_light_duration insight category

ALTER TYPE insight_category ADD VALUE 'bed_light_duration';

INSERT INTO info_insight_cards (category, title, text)
VALUES
 ---
('bed_light_duration',
'Associating Your Bedroom with Sleep',
'Regularly watching television, using your phone, or engaging in other mentally stimulating activities just before bed can create a subconscious link between your bedroom and staying alert. This often makes it harder to fall asleep when you want to.

On the other hand, reserving your bedroom for sleep and intimacy can create a link between your bedroom and sleep, which will help your body fall asleep more easily when it’s time for bed.'
);

-- Modify all titles to be title case 08/31/2015

UPDATE info_insight_cards SET title = 'Effects of Ambient Light on Your Sleep' WHERE id = 1;
UPDATE info_insight_cards SET title = 'Sound: Ambient Sound or Sudden Loud Noises' WHERE id = 3;
UPDATE info_insight_cards SET title = 'Sleep Duration: What''s the Ideal Amount?' WHERE id = 6;
UPDATE info_insight_cards SET title = 'Effects of Exercise on Your Sleep' WHERE id = 10;
UPDATE info_insight_cards SET title = 'Waking Up to Sleep Better' WHERE id = 18;

-- 09/04/2015 Text change for sleep_duration
UPDATE info_insight_cards
SET text='Most healthy adults, including the elderly need **between 7.5 to 9 hours** of sleep per night to function at their best.  Children need even more (the younger they are, the more they need); for example, current guidelines say school-age children should sleep at least 10 hours per night.  However, there is no "magic number" for the sleep duration that is ideal for everyone.  It is as individual as you are.  Not only do different age groups need different amounts of sleep, but the amount you need is also unique to you as an individual.'
WHERE category='sleep_duration';


-- Modify grammar 9/8/2015
UPDATE info_insight_cards

SET text='Maintaining good sleep habits is crucial to having regular, good quality sleep. The **most important habit** is to keep a consistent sleep and wake time for every day of the week. Other good habits to cultivate include:

**Avoid napping** during the day as it may disrupt your sleep time.

**Avoid caffeine** about 3 hours before bedtime. Caffeine stimulates your nervous system, making you more alert and less likely to fall asleep. Note that certain energy or soft drinks may also contain caffeine.

**Stay away from stimulants** such as nicotine and alcohol. While you may feel sleepy after consuming alcohol, your sleep might be disrupted at a later time when the effect has worn off.

**Regular exercise** has been shown to promote good sleep. However, vigorous exercise close to bedtime may be too stimulating, try a more gentle exercise such as yoga.

Get sufficient **exposure to natural light**, which helps to maintain a healthy sleep-wake cycle. Go for a 15-minute walk after lunch, or take a coffee break during the afternoon.

**Avoid bright lights** close to bedtime to allow the production of melatonin, a sleep-inducing hormone. The “blue light” from many modern smartphones or tablets has been shown to **interfere with** the body’s melatonin levels. Try not to use such devices in bed.

**Develop a bedtime routine**. Wind down about an hour before bedtime. Engage in relaxing activities such as reading, listening to soothing music, take a warm shower or bath.

**Associate your bedroom with sleep**. Avoid non-sleep activities such as watching TV, working, or using the computer when in the bedroom.'


--jyfan 9/22/2015 Update text for humidity card.

UPDATE info_insight_cards SET text=
'Humidity is an important part of an optimal sleeping environment. You should aim to keep your bedroom humidity level at around 50% year round.

Air that is too dry can irritate your throat and nasal passages, which can make it more difficult for you to fall asleep. If this is the case, consider investing in a humidifier for use during drier seasons. Also remember to keep hydrated by drinking plenty of water, and apply lotion before bed to soothe dry skin.

Conversely, dampness can lead to mold growth, which can affect your sleep if you suffer from mold allergies. High humidity paired with high temperatures can be especially uncomfortable, making it even more difficult to get restorative sleep. You may want to think about a fan or a dehumidifier to lower the humidity, or using cotton sheets and sweat-wicking pajamas to help you feel more comfortable.'

WHERE category='humidity';


--jyfan 10/02/2015 Adding bed_light_intensity insight category

ALTER TYPE insight_category ADD VALUE 'bed_light_intensity_ratio';

INSERT INTO info_insight_cards (category, title, text)
VALUES
 ---
('bed_light_intensity_ratio',
'Light Exposure and Your Biological Clock',
'The right exposure to light can promote healthy sleep, boost productivity, and improve your overall well being.

Light, whether it be artificial or natural, is the primary signal for your circadian rhythm. This governs not only your sleep, but also your mood, appetite, and more. In the morning, light prompts your body to wake up while blood flow and alertness increases. In the evening, the dimming of light tells your body to cool down, relax, and rest.

Too little light early in the day confuses your internal clock, which can feel like a miniature dose of jet lag. Similarly, too much exposure to bright or blue light in the evening can artificially pump you up when you’re trying to unwind.

If you use a lot of artificial light at night, make sure you balance it out by exposing yourself to more light during the morning and during the day. Exposure to light can enhance your sleep as long as it is synced with the natural patterns of your internal clock.'
);

--jyfan 10/16/2015 Updating air quality text

UPDATE info_insight_cards SET text=
'Clean air is an important part of a healthy environment. A high concentration of airborne particulates — microscopic fragments of matter that can penetrate deep into your lungs — can irritate your throat and airways, exacerbate asthma symptoms, and disrupt your sleep.

 Particulates can come from indoor sources of pollutants like smoke, cooking fumes, and even some household cleaners. You should always take care to minimize your exposure to these types of pollutants. If necessary, clear your room by opening a window.

 Particulate pollution can also come from outdoor sources both natural and artificial. You can check <airnow.gov> if there is an air quality advisory for your area. If so, you should follow EPA recommendations, and avoid spending too much time outdoors.'

WHERE category='air_quality';

UPDATE info_insight_cards SET title='Particulate Matter and Your Health' WHERE category='air_quality';

--jyfan 10/20/2015 Some temp grammar fixes
UPDATE info_insight_cards SET text=
'Maintaining good sleep habits is crucial to getting regular, high quality sleep. The **most important habit** is to keep a consistent sleep and wake time for every day of the week, including weekends. Other great habits to cultivate include:

**Avoid napping** during the day as it may interfere with your ability to fall asleep.

**Avoid caffeine** about 3 hours before bedtime. Caffeine stimulates your nervous system, making you more alert and preventing you from falling asleep. Note that certain energy or soft drinks also contain caffeine.

**Stay away from stimulants** such as nicotine and alcohol. While you may feel sleepy after consuming alcohol, studies show the quality of your sleep becomes worse.

**Regular exercise** has been shown to promote good sleep. However, avoid vigorous exercise too close to bedtime, as it may have a stimulating effect.

Get sufficient **exposure to natural light** during the day, which helps you maintain a healthy sleep-wake cycle. Go for a 15-minute walk after lunch, or take a coffee break in the afternoon.

**Avoid bright lights** close to bedtime to allow the production of melatonin, a sleep-inducing hormone. The “blue light” from many modern smartphones or tablets has been shown to **interfere with** your body’s melatonin levels. Try not to use such devices in bed.

**Develop a bedtime routine**. Wind down about an hour before bedtime. Give yourself some time to relax with a book, soothing music, or even a warm bath.

**Associate your bedroom with sleep**. Avoid activities such as watching TV, working, or using the computer in your bedroom.'
WHERE category='sleep_hygiene';

UPDATE info_insight_cards SET text=
'Light influences your internal clock through specialized "light sensitive" cells in your retina, which send messages to your brain to regulate your **circadian rhythm**. These are physical, mental and behavioral changes that follow a roughly 24-hour cycle, responding primarily to light and dark in your environment.

The increase and decrease of light cues trigger different chemical reactions in your body, prompting changes to your physiology and behavior. As evening approaches and the light in your environment recedes, your body starts to produce more melatonin and body temperature falls, making you less alert and more likely to fall asleep. When sunlight returns in the morning, a decrease in melatonin levels and rise in body temperature, along with other chemical shifts such as an increase in the activating hormone cortisol, then help you feel alert and ready for the day.'
WHERE category='light';

--jyfan 11/3/2015 grammar affect/effect correction
UPDATE info_insight_cards SET text=
'The temperature of your sleeping environment and how comfortable you feel in it affects the quality of sleep because our brain has an ideal **set point** for body temperature that it is trying to achieve, almost like an internal thermostat. If it’s too cold, or too hot, the body struggles to achieve this set point, hence interrupting sleep. The usual recommendation is to keep the bedroom cool, quiet, & dark, between 65°F (18°C) and 72°F (22°C). However, the perfect room temperature level is different for everyone and should be whatever is most comfortable for the sleeper.'
WHERE category='temperature';

--jyfan/11/5/2015 partner motion text
ALTER TYPE insight_category ADD VALUE 'partner_motion';

INSERT INTO info_insight_cards (category, title, text)
VALUES
 ---
('partner_motion',
'Sleep Better, Together.',
'When you sleep next to someone, their movements can cause you to move more too, and this can affect the overall quality of your sleep. However, sleeping next to someone can have a positive effect as well. Many couples report sleeping better when they sleep together than when they sleep apart, hinting at a more complex link between your sleep and your partner.

 Satisfaction with your relationship is simultaneously impacted by and affects sleep quality. In addition to managing personal sleep disruptions such as sleep apnea and snoring, you and your partner can encourage each other to keep good sleep habits. Go to bed and wake up at a consistent time, keep distracting electronics out of the bedroom, and if possible, you and your partner should try to go to bed at the same time.'
 );


 --jakepic1 12/10/2015 category_name column
ALTER TABLE info_insight_cards ADD COLUMN category_name VARCHAR (255);

UPDATE info_insight_cards SET category_name='Light' WHERE category='light';
UPDATE info_insight_cards SET category_name='Noise' WHERE category='sound';
UPDATE info_insight_cards SET category_name='Temperature' WHERE category='temperature';
UPDATE info_insight_cards SET category_name='Air Quality' WHERE category='air_quality';
UPDATE info_insight_cards SET category_name='Sleep Quality' WHERE category='sleep_quality';
UPDATE info_insight_cards SET category_name='Wake Variance' WHERE category='wake_variance';
UPDATE info_insight_cards SET category_name='Light' WHERE category='bed_light_duration';
UPDATE info_insight_cards SET category_name='Light Balance' WHERE category='bed_light_intensity_ratio';
UPDATE info_insight_cards SET category_name='Humidity' WHERE category='humidity';
UPDATE info_insight_cards SET category_name='Sleep Tips' WHERE category='sleep_duration';
UPDATE info_insight_cards SET category_name='Sleep Tips' WHERE category='sleep_hygiene';
UPDATE info_insight_cards SET category_name='Sleep Quality' WHERE category='partner_motion';
UPDATE info_insight_cards SET category_name='' WHERE category_name IS NULL;



--- Jake and Jingyun 2015-12-11
INSERT INTO info_insight_cards (category, title, text) VALUES
('generic', 'Welcome to Sense',
'The Insights you''ll see here are tailored just for you, based on Sense''s analysis of your sleep patterns and bedroom environment. Over time, these Insights will become even more personalized as Sense learns about your sleep habits.

Occasionally, you''ll see Questions here as well. Answering these questions helps Sense learn more about you, which means your Insights will become more detailed and accurate in the future.

That''s it for now. There''s no need to tell Sense you''re ready for bed — all you need to do is sleep. Check back in a few days for more Insights in this space.');


UPDATE info_insight_cards SET category_name='Sense' WHERE category='generic';


UPDATE info_insight_cards SET text=
'Everyone''s sleep is different, but there are some general guidelines that can help anyone get better sleep:

* Taking a midday nap can sometimes be tempting, but it can throw off your natural sleep cycle and make it much harder to fall asleep at night.

* Stay away from stimulants such as nicotine and alcohol. While you may feel sleepy after consuming alcohol, your sleep might be disrupted during the night after the effect has worn off.

* Regular exercise has been shown to promote good sleep, but vigorous exercise close to bedtime may amp you up and make it harder to sleep. If the only time you have to exercise is late at night before bed, try a more gentle exercise like yoga.

While these tips can be useful to anyone trying to improve their sleep, you''ll get the most benefit from learning the specific factors that affect your sleep as an individual.  Soon, you''ll begin to see personalized Insights based on your own sleep patterns'
WHERE category='sleep_hygiene';


UPDATE info_insight_cards SET title=
'Healthy Sleep Habits'
WHERE category='sleep_hygiene';


UPDATE info_insight_cards SET text=
'While everybody is different, most healthy adults need between 7.5 to 9 hours of good sleep per night to function at their best. That said, try not to worry if it isn''t possible to get a full night''s sleep on occasion. Instead, focus on making the most out of the sleep you can get.

Use the Insights provided by Sense to better understand how you can improve your sleep, and remember to answer the Questions asked here to help Sense learn more about your sleep. The more you use Sense, and the more Questions you answer, the more detailed and accurate your Insights will be.'
WHERE category='sleep_duration';


UPDATE info_insight_cards SET title=
'A Better Night''s Sleep'
WHERE category='sleep_duration';

UPDATE info_insight_cards SET category_name='Sleep Quality' WHERE category='partner_motion';

--- Jake & jyfan markdown spacing update 2015-12-15

UPDATE info_insight_cards SET text=
'Everyone''s sleep is different, but there are some general guidelines that can help anyone get better sleep:

* Taking a midday nap can sometimes be tempting, but it can throw off your natural sleep cycle and make it much harder to fall asleep at night.
* Stay away from stimulants such as nicotine and alcohol. While you may feel sleepy after consuming alcohol, your sleep might be disrupted during the night after the effect has worn off.
* Regular exercise has been shown to promote good sleep, but vigorous exercise close to bedtime may amp you up and make it harder to sleep. If the only time you have to exercise is late at night before bed, try a more gentle exercise like yoga.

While these tips can be useful to anyone trying to improve their sleep, you''ll get the most benefit from learning the specific factors that affect your sleep as an individual.  Soon, you''ll begin to see personalized Insights based on your own sleep patterns'
WHERE category='sleep_hygiene';