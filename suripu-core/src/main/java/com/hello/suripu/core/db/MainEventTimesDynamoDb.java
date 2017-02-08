package com.hello.suripu.core.db;


import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.google.common.collect.Sets;
import com.hello.suripu.core.db.dynamo.Attribute;
import com.hello.suripu.core.db.dynamo.Util;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.MainEventTimes;
import com.hello.suripu.core.models.SleepPeriod;
import org.apache.commons.collections.map.HashedMap;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jarredheinrich on 2/7/17.
 */
public class MainEventTimesDynamoDb implements MainEventTimesDAO{

    private final static Logger LOGGER = LoggerFactory.getLogger(MainEventTimesDynamoDb.class);

    private final Table table;

    public static enum AttributeName implements Attribute {
        ACCOUNT_ID("account_id", "N"), // hash-key
        DATE("date", "S"), // range-key
        MORNING_IN_BED_TIME("morning_in_bed_time", "N"),
        MORNING_IN_BED_OFFSET("morning_in_bed_offset", "N"),
        MORNING_SLEEP_TIME("morning_sleep_time", "N"),
        MORNING_SLEEP_OFFSET("morning_sleep_offset", "N"),
        MORNING_WAKE_UP_TIME("morning_wake_up_time", "N"),
        MORNING_WAKE_UP_OFFSET("morning_wake_up_offset", "N"),
        MORNING_OUT_OF_BED_TIME("morning_out_of_bed_time", "N"),
        MORNING_OUT_OF_BED_OFFSET("morning_out_of_bed_offset", "N"),
        MORNING_CREATED_AT("morning_created_at", "N"),
        //afternoon_evening
        AFTERNOON_IN_BED_TIME("afternoon_in_bed_time", "N"),
        AFTERNOON_IN_BED_OFFSET("afternoon_in_bed_offset", "N"),
        AFTERNOON_SLEEP_TIME("afternoon_sleep_time", "N"),
        AFTERNOON_SLEEP_OFFSET("afternoon_sleep_offset", "N"),
        AFTERNOON_WAKE_UP_TIME("afternoon_wake_up_time", "N"),
        AFTERNOON_WAKE_UP_OFFSET("afternoon_wake_up_offset", "N"),
        AFTERNOON_OUT_OF_BED_TIME("afternoon_out_of_bed_time", "N"),
        AFTERNOON_OUT_OF_BED_OFFSET("afternoon_out_of_bed_offset", "N"),
        AFTERNOON_CREATED_AT("afternoon_created_at", "N"),
        //night
        NIGHT_IN_BED_TIME("night_in_bed_time", "N"),
        NIGHT_IN_BED_OFFSET("night_in_bed_offset", "N"),
        NIGHT_SLEEP_TIME("night_sleep_time", "N"),
        NIGHT_SLEEP_OFFSET("night_sleep_offset", "N"),
        NIGHT_WAKE_UP_TIME("night_wake_up_time", "N"),
        NIGHT_WAKE_UP_OFFSET("night_wake_up_offset", "N"),
        NIGHT_OUT_OF_BED_TIME("night_out_of_bed_time", "N"),
        NIGHT_OUT_OF_BED_OFFSET("night_out_of_bed_offset", "N"),
        NIGHT_CREATED_AT("night_created_at", "N");

        private final String name;
        private final String type;

        AttributeName(final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String shortName() {
            return name;
        }

        @Override
        public String sanitizedName() {
            return toString();
        }

        @Override
        public String type() {
            return type;
        }

        public static Set<String> getAllAttributes() {
            final Set<String> attributes = Sets.newHashSet();
            for (final Attribute attribute : MainEventTimesDynamoDb.AttributeName.values()) {
                attributes.add(attribute.shortName());
            }
            return attributes;
        }
    }
    private enum EventAttributes{

        MORNING_IN_BED(AttributeName.MORNING_IN_BED_TIME,AttributeName.MORNING_IN_BED_OFFSET),
        MORNING_SLEEP(AttributeName.MORNING_SLEEP_TIME,AttributeName.MORNING_SLEEP_OFFSET),
        MORNING_WAKE_UP(AttributeName.MORNING_WAKE_UP_TIME,AttributeName.MORNING_WAKE_UP_OFFSET),
        MORNING_OUT_OF_BED(AttributeName.MORNING_OUT_OF_BED_TIME,AttributeName.MORNING_OUT_OF_BED_OFFSET),

        AFTERNOON_IN_BED(AttributeName.AFTERNOON_IN_BED_TIME,AttributeName.AFTERNOON_IN_BED_OFFSET),
        AFTERNOON_SLEEP(AttributeName.AFTERNOON_SLEEP_TIME,AttributeName.AFTERNOON_SLEEP_OFFSET),
        AFTERNOON_WAKE_UP(AttributeName.AFTERNOON_WAKE_UP_TIME,AttributeName.AFTERNOON_WAKE_UP_OFFSET),
        AFTERNOON_OUT_OF_BED(AttributeName.AFTERNOON_OUT_OF_BED_TIME,AttributeName.AFTERNOON_OUT_OF_BED_OFFSET),

        NIGHT_IN_BED(AttributeName.NIGHT_IN_BED_TIME,AttributeName.NIGHT_IN_BED_OFFSET),
        NIGHT_SLEEP(AttributeName.NIGHT_SLEEP_TIME,AttributeName.NIGHT_SLEEP_OFFSET),
        NIGHT_WAKE_UP(AttributeName.NIGHT_WAKE_UP_TIME,AttributeName.NIGHT_WAKE_UP_OFFSET),
        NIGHT_OUT_OF_BED(AttributeName.NIGHT_OUT_OF_BED_TIME,AttributeName.NIGHT_OUT_OF_BED_OFFSET);


        private final AttributeName eventTime;
        private final AttributeName eventOffset;
        EventAttributes(final AttributeName eventTime, final AttributeName eventOffset) {
            this.eventTime = eventTime;
            this.eventOffset = eventOffset;
        }
        public static  Map<Event.Type, EventAttributes> getEventAttributeForPeriod(SleepPeriod.Period period){
            final Map<Event.Type, EventAttributes> eventAttributesMap = new HashedMap();
            if (period == SleepPeriod.Period.MORNING){
                eventAttributesMap.put(Event.Type.IN_BED, MORNING_IN_BED);
                eventAttributesMap.put(Event.Type.SLEEP, MORNING_SLEEP);
                eventAttributesMap.put(Event.Type.WAKE_UP, MORNING_WAKE_UP);
                eventAttributesMap.put(Event.Type.OUT_OF_BED, MORNING_OUT_OF_BED);
            }
            else if (period == SleepPeriod.Period.AFTERNOON_EVENING) {
                eventAttributesMap .put(Event.Type.IN_BED, AFTERNOON_IN_BED);
                eventAttributesMap.put(Event.Type.SLEEP, AFTERNOON_SLEEP);
                eventAttributesMap.put(Event.Type.WAKE_UP, AFTERNOON_WAKE_UP);
                eventAttributesMap.put(Event.Type.OUT_OF_BED, AFTERNOON_OUT_OF_BED);
            }

            else {
                eventAttributesMap .put(Event.Type.IN_BED, NIGHT_IN_BED);
                eventAttributesMap .put(Event.Type.SLEEP, NIGHT_SLEEP);
                eventAttributesMap .put(Event.Type.WAKE_UP, NIGHT_WAKE_UP);
                eventAttributesMap .put(Event.Type.OUT_OF_BED, NIGHT_OUT_OF_BED);
            }

            return eventAttributesMap ;
        }

    }
    private static String getCreatedAtAttributeName(final SleepPeriod sleepPeriod){
       if(sleepPeriod.PERIOD == SleepPeriod.Period.MORNING){
           return AttributeName.MORNING_CREATED_AT.shortName();
       }
       if(sleepPeriod.PERIOD == SleepPeriod.Period.AFTERNOON_EVENING){
           return AttributeName.AFTERNOON_CREATED_AT.shortName();
       }
       return AttributeName.NIGHT_CREATED_AT.shortName();

    }

    private static class EventTimeAttributeNames {
        final String CREATED_AT;
        final String IN_BED_TIME;
        final String IN_BED_OFFSET;
        final String SLEEP_TIME;
        final String SLEEP_OFFSET;
        final String WAKE_UP_TIME;
        final String WAKE_UP_OFFSET;
        final String OUT_OF_BED_TIME;
        final String OUT_OF_BED_OFFSET;

        EventTimeAttributeNames(final String createdAt, final String inBedTimeAttributeName, final String inBedOffsetAttributeName,
                                final String sleepTimeAttributeName, final String sleepOffsetAttributeName,
                                final String wakeUpTimeAttributeName, final String wakeUpOffsetAttributeName,
                                final String outOfBedTimeAttributeName, final String outOfBedOffsetAttributeName) {
            this.CREATED_AT = createdAt;
            this.IN_BED_TIME = inBedTimeAttributeName;
            this.IN_BED_OFFSET = inBedOffsetAttributeName;
            this.SLEEP_TIME = sleepTimeAttributeName;
            this.SLEEP_OFFSET = sleepOffsetAttributeName;
            this.WAKE_UP_TIME = wakeUpTimeAttributeName;
            this.WAKE_UP_OFFSET = wakeUpOffsetAttributeName;
            this.OUT_OF_BED_TIME = outOfBedTimeAttributeName;
            this.OUT_OF_BED_OFFSET = outOfBedOffsetAttributeName;
        }
        public static EventTimeAttributeNames getEventAttributeNames(final SleepPeriod sleepPeriod) {
            final String createdAtAttributeName = getCreatedAtAttributeName(sleepPeriod);
            final Map<Event.Type, EventAttributes> eventAttributesMap = EventAttributes.getEventAttributeForPeriod(sleepPeriod.PERIOD);
            final String inBedTimeAttributeName = eventAttributesMap.get(Event.Type.IN_BED).eventTime.shortName();
            final String inBedOffsetAttributeName = eventAttributesMap.get(Event.Type.IN_BED).eventOffset.shortName();
            final String sleepTimeAttributeName = eventAttributesMap.get(Event.Type.SLEEP).eventTime.shortName();
            final String sleepOffsetAttributeName = eventAttributesMap.get(Event.Type.SLEEP).eventOffset.shortName();
            final String wakeIpTimeAttributeName = eventAttributesMap.get(Event.Type.WAKE_UP).eventTime.shortName();
            final String wakeUpOffsetAttributeName = eventAttributesMap.get(Event.Type.WAKE_UP).eventOffset.shortName();
            final String outOfBedTimeAttributeName = eventAttributesMap.get(Event.Type.OUT_OF_BED).eventTime.shortName();
            final String outOfBedOffsetAttributeName = eventAttributesMap.get(Event.Type.OUT_OF_BED).eventOffset.shortName();
            return new EventTimeAttributeNames(createdAtAttributeName, inBedTimeAttributeName, inBedOffsetAttributeName,
                    sleepTimeAttributeName, sleepOffsetAttributeName,
                    wakeIpTimeAttributeName, wakeUpOffsetAttributeName,
                    outOfBedTimeAttributeName, outOfBedOffsetAttributeName);
        }
    }

    private MainEventTimesDynamoDb(final Table table){
        this.table = table;


    }

    public static MainEventTimesDynamoDb create(final AmazonDynamoDB client, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(client);
        final Table table = dynamoDB.getTable(tableName);
        return new MainEventTimesDynamoDb(table);
    }

    public static TableDescription createTable(final AmazonDynamoDB client, final String tableName) {

        final CreateTableResult result = Util.createTable(client, tableName, AttributeName.ACCOUNT_ID, AttributeName.DATE, 1L, 1L);

        return result.getTableDescription();
    }

    public Map<SleepPeriod, MainEventTimes> getEventTimes(final Long accountId, final DateTime date){
        //final PrimaryKey key = new PrimaryKey(AttributeName.ACCOUNT_ID.shortName(), accountId);
        //final RangeKeyCondition rangeKey = new RangeKeyCondition(AttributeName.DATE.shortName()).eq(date);
        final Item item = table.getItem(AttributeName.ACCOUNT_ID.shortName(), accountId, AttributeName.DATE.shortName(),date);
        if (item == null){
            LOGGER.debug("msg=get-main-events-fail-main-events-missing account_id={} date={}", accountId, date);
            final Map<SleepPeriod, MainEventTimes> mainEventMap =  new HashMap<>();
            return mainEventMap;
        }

        final Map<SleepPeriod, MainEventTimes> mainEventMap = fromItem(item);
        return mainEventMap;
    }

    public boolean updateEventTimes(final Long accountId, final DateTime targetDate, final MainEventTimes mainEventTimes){

        final List<AttributeUpdate> attributeUpdateList = getAttributeUpdateList(mainEventTimes);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(AttributeName.ACCOUNT_ID.shortName(), accountId, AttributeName.DATE.shortName(), targetDate)
                .withReturnValues(ReturnValue.ALL_NEW)
                .withAttributeUpdate(attributeUpdateList);

        final UpdateItemOutcome updatedItem = table.updateItem(updateItemSpec);
        final Item item = updatedItem.getItem();
        //check for all four eventTimes and createdAt
        final EventTimeAttributeNames eventTimeAttributeNames = EventTimeAttributeNames.getEventAttributeNames(mainEventTimes.SLEEP_PERIOD);
        if(!hasSleepPeriod(eventTimeAttributeNames, item)){
            LOGGER.error("error=update-sleep-period-event-times-failed-missing-event-times account_id={} date={} sleep_period={}", accountId, targetDate, mainEventTimes.SLEEP_PERIOD.PERIOD);
            return false;
        }


        if (!hasCorrectEventTimes(item, mainEventTimes)){
            LOGGER.error("error=update-sleep-period-event-times-failed-incorrected-event_time account_id={} date={} sleep_period={}", accountId, targetDate, mainEventTimes.SLEEP_PERIOD.PERIOD);
            return false;
        }
        return true;
    }

    private List<AttributeUpdate> getAttributeUpdateList(final MainEventTimes mainEventTimes){
        final EventTimeAttributeNames eventTimeAttributeNames = EventTimeAttributeNames.getEventAttributeNames(mainEventTimes.SLEEP_PERIOD);

        final AttributeUpdate createdAtUpdate = new AttributeUpdate(eventTimeAttributeNames.CREATED_AT);
        createdAtUpdate.put(mainEventTimes.CREATED_AT);

        final AttributeUpdate inBedTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.IN_BED_TIME);
        final AttributeUpdate inBedOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.IN_BED_OFFSET);
        inBedTimeUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.IN_BED).TIME);
        inBedOffsetUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.IN_BED).OFFSET);

        final AttributeUpdate sleepTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.SLEEP_TIME);
        final AttributeUpdate sleepOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.SLEEP_OFFSET);
        sleepTimeUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.SLEEP).TIME);
        sleepOffsetUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.SLEEP).OFFSET);

        final AttributeUpdate wakeUpTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.WAKE_UP_TIME);
        final AttributeUpdate wakeUpOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.WAKE_UP_OFFSET);
        wakeUpTimeUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.WAKE_UP).TIME);
        wakeUpOffsetUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.WAKE_UP).OFFSET);

        final AttributeUpdate outOfBedTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.OUT_OF_BED_TIME);
        final AttributeUpdate outOfBedOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.OUT_OF_BED_OFFSET);
        outOfBedTimeUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.OUT_OF_BED).TIME);
        outOfBedOffsetUpdate.put(mainEventTimes.EVENT_TIME_MAP.get(Event.Type.OUT_OF_BED).OFFSET);

        final List<AttributeUpdate> attributeUpdateList = Arrays.asList(createdAtUpdate, inBedTimeUpdate, inBedOffsetUpdate,
                sleepTimeUpdate, sleepOffsetUpdate, wakeUpTimeUpdate, wakeUpOffsetUpdate, outOfBedTimeUpdate, outOfBedOffsetUpdate);
        return attributeUpdateList;
    }

    private Map<SleepPeriod, MainEventTimes> fromItem(final Item item){
        final Map<SleepPeriod, MainEventTimes> sleepPeriodEventTimesMap = new HashMap<>();

        //Search for main events in all three sleep periods
        for (final SleepPeriod sleepPeriod : SleepPeriod.getAll()){
            final EventTimeAttributeNames eventTimeAttributeNames = EventTimeAttributeNames.getEventAttributeNames(sleepPeriod);

            if (hasSleepPeriod(eventTimeAttributeNames, item)){
                final long createdAt = item.getLong(eventTimeAttributeNames.CREATED_AT);

                final long inBedTime =item.getLong(eventTimeAttributeNames.IN_BED_TIME);
                final int inBedOffset =item.getInt(eventTimeAttributeNames.IN_BED_OFFSET);
                final MainEventTimes.EventTime inBedEventTime = new MainEventTimes.EventTime(inBedTime, inBedOffset);

                final long sleepTime =item.getLong(eventTimeAttributeNames.SLEEP_TIME);
                final int sleepEventOffset =item.getInt(eventTimeAttributeNames.SLEEP_OFFSET);
                final MainEventTimes.EventTime sleepEventTime = new MainEventTimes.EventTime(sleepTime, sleepEventOffset);

                final long wakeUpTime =item.getLong(eventTimeAttributeNames.WAKE_UP_TIME);
                final int wakeUpOffset =item.getInt(eventTimeAttributeNames.WAKE_UP_OFFSET);
                final MainEventTimes.EventTime wakeUpEventTime = new MainEventTimes.EventTime(wakeUpTime, wakeUpOffset);

                final long outOfBedTime =item.getLong(eventTimeAttributeNames.OUT_OF_BED_TIME);
                final int outOfBedOffset =item.getInt(eventTimeAttributeNames.OUT_OF_BED_OFFSET);
                final MainEventTimes.EventTime outOfBedEventTime = new MainEventTimes.EventTime(outOfBedTime, outOfBedOffset);

                final MainEventTimes mainEventTimes = new MainEventTimes(sleepPeriod, createdAt, inBedEventTime, sleepEventTime, wakeUpEventTime, outOfBedEventTime);
                sleepPeriodEventTimesMap.put(sleepPeriod, mainEventTimes);
            }
        }

        return sleepPeriodEventTimesMap;
    }

    private boolean hasSleepPeriod(final EventTimeAttributeNames eventTimeAttributeNames, final Item item){

        final boolean hasCreatedAt = item.hasAttribute(eventTimeAttributeNames.CREATED_AT);
        final boolean hasInBed = item.hasAttribute(eventTimeAttributeNames.IN_BED_TIME) && item.hasAttribute(eventTimeAttributeNames.IN_BED_OFFSET);
        final boolean hasSleep = item.hasAttribute(eventTimeAttributeNames.SLEEP_TIME) && item.hasAttribute(eventTimeAttributeNames.SLEEP_OFFSET);
        final boolean hasWakeUp = item.hasAttribute(eventTimeAttributeNames.WAKE_UP_TIME) && item.hasAttribute(eventTimeAttributeNames.WAKE_UP_OFFSET);
        final boolean hasOutOfBed = item.hasAttribute(eventTimeAttributeNames.WAKE_UP_OFFSET) && item.hasAttribute(eventTimeAttributeNames.OUT_OF_BED_TIME);
        return hasCreatedAt && hasInBed && hasSleep && hasWakeUp && hasOutOfBed;
    }

    private boolean hasCorrectEventTimes(final Item item, final MainEventTimes mainEventTimes){

        final MainEventTimes updatedMainEvent = fromItem(item).get(mainEventTimes.SLEEP_PERIOD.PERIOD);
        if (updatedMainEvent == mainEventTimes){
            return true;
        }
        return false;
    }

}
