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
import com.hello.suripu.core.util.DateTimeUtil;
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
public class MainEventTimesDynamoDB implements MainEventTimesDAO{

    private final static Logger LOGGER = LoggerFactory.getLogger(MainEventTimesDynamoDB.class);

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
            for (final Attribute attribute : MainEventTimesDynamoDB.AttributeName.values()) {
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
    private static String getCreatedAtAttributeName(final SleepPeriod.Period period){
       if(period == SleepPeriod.Period.MORNING){
           return AttributeName.MORNING_CREATED_AT.shortName();
       }
       if(period == SleepPeriod.Period.AFTERNOON_EVENING){
           return AttributeName.AFTERNOON_CREATED_AT.shortName();
       }
       return AttributeName.NIGHT_CREATED_AT.shortName();

    }

    private static class EventTimeAttributeNames {
        final String createdAt;
        final String inBedTime;
        final String inBedOffset;
        final String sleepTime;
        final String sleepOffset;
        final String wakeUpTime;
        final String wakeUpOffset;
        final String outOfBedTime;
        final String outOfBedOffset;

        EventTimeAttributeNames(final String createdAt, final String inBedTimeAttributeName, final String inBedOffsetAttributeName,
                                final String sleepTimeAttributeName, final String sleepOffsetAttributeName,
                                final String wakeUpTimeAttributeName, final String wakeUpOffsetAttributeName,
                                final String outOfBedTimeAttributeName, final String outOfBedOffsetAttributeName) {
            this.createdAt = createdAt;
            this.inBedTime = inBedTimeAttributeName;
            this.inBedOffset = inBedOffsetAttributeName;
            this.sleepTime = sleepTimeAttributeName;
            this.sleepOffset = sleepOffsetAttributeName;
            this.wakeUpTime = wakeUpTimeAttributeName;
            this.wakeUpOffset = wakeUpOffsetAttributeName;
            this.outOfBedTime = outOfBedTimeAttributeName;
            this.outOfBedOffset = outOfBedOffsetAttributeName;
        }
        public static EventTimeAttributeNames getEventAttributeNames(final SleepPeriod.Period period) {
            final String createdAtAttributeName = getCreatedAtAttributeName(period);
            final Map<Event.Type, EventAttributes> eventAttributesMap = EventAttributes.getEventAttributeForPeriod(period);
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

    private MainEventTimesDynamoDB(final Table table){
        this.table = table;


    }

    public static MainEventTimesDynamoDB create(final AmazonDynamoDB client, final String tableName) {
        final DynamoDB dynamoDB = new DynamoDB(client);
        final Table table = dynamoDB.getTable(tableName);
        return new MainEventTimesDynamoDB(table);
    }

    public static TableDescription createTable(final AmazonDynamoDB client, final String tableName) {

        final CreateTableResult result = Util.createTable(client, tableName, AttributeName.ACCOUNT_ID, AttributeName.DATE, 1L, 1L);

        return result.getTableDescription();
    }

    public Map<SleepPeriod.Period, MainEventTimes> getEventTimes(final Long accountId, final DateTime targetDate){
        //final PrimaryKey key = new PrimaryKey(AttributeName.ACCOUNT_ID.shortName(), accountId);
        //final RangeKeyCondition rangeKey = new RangeKeyCondition(AttributeName.DATE.shortName()).eq(date);
        final String targetDateString = DateTimeUtil.dateToYmdString(targetDate);
        final Item item = table.getItem(AttributeName.ACCOUNT_ID.shortName(), accountId, AttributeName.DATE.shortName(),targetDateString);
        if (item == null){
            LOGGER.debug("msg=get-main-events-fail-main-events-missing account_id={} date={}", accountId, targetDateString);
            final Map<SleepPeriod.Period, MainEventTimes> mainEventMap =  new HashMap<>();
            return mainEventMap;
        }

        final Map<SleepPeriod.Period, MainEventTimes> mainEventMap = fromItem(item);
        return mainEventMap;
    }

    public boolean updateEventTimes(final Long accountId, final DateTime targetDate, final MainEventTimes mainEventTimes){

        final String targetDateString = DateTimeUtil.dateToYmdString(targetDate);


        if (!mainEventTimes.hasValidEventTimes()){
            LOGGER.error("error=update-sleep-period-event-times-failed reason=invalid-event-times account_id={} date={} sleep_period={}", accountId, targetDateString, mainEventTimes.sleepPeriod.period);
            return false;
        }
        final List<AttributeUpdate> attributeUpdateList = getAttributeUpdateList(mainEventTimes);

        final UpdateItemSpec updateItemSpec = new UpdateItemSpec()
                .withPrimaryKey(AttributeName.ACCOUNT_ID.shortName(), accountId, AttributeName.DATE.shortName(), targetDateString)
                .withReturnValues(ReturnValue.ALL_NEW)
                .withAttributeUpdate(attributeUpdateList);

        final UpdateItemOutcome updatedItem = table.updateItem(updateItemSpec);
        final Item item = updatedItem.getItem();
        //check for all four eventTimes and createdAt
        final EventTimeAttributeNames eventTimeAttributeNames = EventTimeAttributeNames.getEventAttributeNames(mainEventTimes.sleepPeriod.period);
        final boolean hasAttributes = hasSleepPeriod(eventTimeAttributeNames, item);
        if(!hasAttributes){
            LOGGER.error("error=update-sleep-period-event-times-failed reason=missing-event-times account_id={} date={} sleep_period={}", accountId, targetDateString, mainEventTimes.sleepPeriod.period);
            return false;
        }

        final boolean hasCorrectValues = hasCorrectEventTimes(item, mainEventTimes);
        if (!hasCorrectValues){
            LOGGER.error("error=update-sleep-period-event-times-failed reason=incorrected-event_time account_id={} date={} sleep_period={}", accountId, targetDateString, mainEventTimes.sleepPeriod.period);
            return false;
        }
        return true;
    }

    private List<AttributeUpdate> getAttributeUpdateList(final MainEventTimes mainEventTimes){

        final EventTimeAttributeNames eventTimeAttributeNames = EventTimeAttributeNames.getEventAttributeNames(mainEventTimes.sleepPeriod.period);

        final AttributeUpdate createdAtUpdate = new AttributeUpdate(eventTimeAttributeNames.createdAt);
        createdAtUpdate.put(mainEventTimes.createdAt);

        final AttributeUpdate inBedTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.inBedTime);
        final AttributeUpdate inBedOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.inBedOffset);
        inBedTimeUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.IN_BED).TIME);
        inBedOffsetUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.IN_BED).OFFSET);

        final AttributeUpdate sleepTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.sleepTime);
        final AttributeUpdate sleepOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.sleepOffset);
        sleepTimeUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.SLEEP).TIME);
        sleepOffsetUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.SLEEP).OFFSET);

        final AttributeUpdate wakeUpTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.wakeUpTime);
        final AttributeUpdate wakeUpOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.wakeUpOffset);
        wakeUpTimeUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.WAKE_UP).TIME);
        wakeUpOffsetUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.WAKE_UP).OFFSET);

        final AttributeUpdate outOfBedTimeUpdate = new AttributeUpdate(eventTimeAttributeNames.outOfBedTime);
        final AttributeUpdate outOfBedOffsetUpdate = new AttributeUpdate(eventTimeAttributeNames.outOfBedOffset);
        outOfBedTimeUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.OUT_OF_BED).TIME);
        outOfBedOffsetUpdate.put(mainEventTimes.eventTimeMap.get(Event.Type.OUT_OF_BED).OFFSET);

        final List<AttributeUpdate> attributeUpdateList = Arrays.asList(createdAtUpdate, inBedTimeUpdate, inBedOffsetUpdate,
                sleepTimeUpdate, sleepOffsetUpdate, wakeUpTimeUpdate, wakeUpOffsetUpdate, outOfBedTimeUpdate, outOfBedOffsetUpdate);
        return attributeUpdateList;
    }

    private Map<SleepPeriod.Period, MainEventTimes> fromItem(final Item item){
        final Map<SleepPeriod.Period, MainEventTimes> sleepPeriodEventTimesMap = new HashMap<>();

        //Search for main events in all three sleep periods
        for (final SleepPeriod.Period period : SleepPeriod.getAll()){
            final EventTimeAttributeNames eventTimeAttributeNames = EventTimeAttributeNames.getEventAttributeNames(period);

            if (hasSleepPeriod(eventTimeAttributeNames, item)){
                final long createdAt = item.getLong(eventTimeAttributeNames.createdAt);

                final long inBedTime =item.getLong(eventTimeAttributeNames.inBedTime);
                final int inBedOffset =item.getInt(eventTimeAttributeNames.inBedOffset);
                final MainEventTimes.EventTime inBedEventTime = new MainEventTimes.EventTime(inBedTime, inBedOffset);

                final long sleepTime =item.getLong(eventTimeAttributeNames.sleepTime);
                final int sleepEventOffset =item.getInt(eventTimeAttributeNames.sleepOffset);
                final MainEventTimes.EventTime sleepEventTime = new MainEventTimes.EventTime(sleepTime, sleepEventOffset);

                final long wakeUpTime =item.getLong(eventTimeAttributeNames.wakeUpTime);
                final int wakeUpOffset =item.getInt(eventTimeAttributeNames.wakeUpOffset);
                final MainEventTimes.EventTime wakeUpEventTime = new MainEventTimes.EventTime(wakeUpTime, wakeUpOffset);

                final long outOfBedTime =item.getLong(eventTimeAttributeNames.outOfBedTime);
                final int outOfBedOffset =item.getInt(eventTimeAttributeNames.outOfBedOffset);
                final MainEventTimes.EventTime outOfBedEventTime = new MainEventTimes.EventTime(outOfBedTime, outOfBedOffset);

                final MainEventTimes mainEventTimes = MainEventTimes.create(createdAt, inBedEventTime, sleepEventTime, wakeUpEventTime, outOfBedEventTime);
                sleepPeriodEventTimesMap.put(period, mainEventTimes);
            }
        }

        return sleepPeriodEventTimesMap;
    }

    private boolean hasSleepPeriod(final EventTimeAttributeNames eventTimeAttributeNames, final Item item){

        final boolean hasCreatedAt = item.hasAttribute(eventTimeAttributeNames.createdAt);
        final boolean hasInBed = item.hasAttribute(eventTimeAttributeNames.inBedTime) && item.hasAttribute(eventTimeAttributeNames.inBedOffset);
        final boolean hasSleep = item.hasAttribute(eventTimeAttributeNames.sleepTime) && item.hasAttribute(eventTimeAttributeNames.sleepOffset);
        final boolean hasWakeUp = item.hasAttribute(eventTimeAttributeNames.wakeUpTime) && item.hasAttribute(eventTimeAttributeNames.wakeUpOffset);
        final boolean hasOutOfBed = item.hasAttribute(eventTimeAttributeNames.wakeUpOffset) && item.hasAttribute(eventTimeAttributeNames.outOfBedTime);
        return hasCreatedAt && hasInBed && hasSleep && hasWakeUp && hasOutOfBed;
    }

    private boolean hasCorrectEventTimes(final Item item, final MainEventTimes mainEventTimes){

        final MainEventTimes updatedMainEvent = fromItem(item).get(mainEventTimes.sleepPeriod.period);
        final List<Event.Type> mainEventTypes = Arrays.asList(Event.Type.IN_BED, Event.Type.SLEEP,Event.Type.WAKE_UP,Event.Type.OUT_OF_BED);

        if(updatedMainEvent.createdAt != mainEventTimes.createdAt){
            return false;
        }

        for (final Event.Type mainEventType : mainEventTypes){

            final boolean correctTime = updatedMainEvent.eventTimeMap.get(mainEventType).TIME.longValue() == mainEventTimes.eventTimeMap.get(mainEventType).TIME.longValue();
            final boolean correctOffset = updatedMainEvent.eventTimeMap.get(mainEventType).OFFSET.intValue() == mainEventTimes.eventTimeMap.get(mainEventType).OFFSET.intValue();
            if(!correctTime || !correctOffset){
                return false;
            }
        }
        return true;

    }

}
