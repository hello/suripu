package com.hello.suripu.core.db;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.core.models.Event;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

/**
 * Created by pangwu on 6/5/14.
 */
public class EventDAODynamoDBIT {

    private BasicAWSCredentials awsCredentials;
    private AmazonDynamoDBClient amazonDynamoDBClient;
    private EventDAODynamoDB eventDAODynamoDB;
    private final String tableName = "event_test";


    @Before
    public void setUp(){
        this.awsCredentials = new BasicAWSCredentials("FAKE_AWS_KEY", "FAKE_AWS_SECRET");
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(0);
        this.amazonDynamoDBClient = new AmazonDynamoDBClient(this.awsCredentials, clientConfiguration);
        this.amazonDynamoDBClient.setEndpoint("http://localhost:7777");

        cleanUp();

        try {
            EventDAODynamoDB.createTable(tableName, this.amazonDynamoDBClient);
            this.eventDAODynamoDB = new EventDAODynamoDB(
                    this.amazonDynamoDBClient,
                    tableName
            );


        }catch (ResourceInUseException rie){
            rie.printStackTrace();
        }
    }


    @After
    public void cleanUp(){
        final DeleteTableRequest deleteTableRequest = new DeleteTableRequest()
                .withTableName(tableName);
        try {
            this.amazonDynamoDBClient.deleteTable(deleteTableRequest);
        }catch (ResourceNotFoundException ex){
            ex.printStackTrace();
        }
    }

    @Test
    public void testSetAndGetEventsForDates(){
        final DateTime startOfDay1 = DateTime.now().withTimeAtStartOfDay();
        final ArrayList<Event> eventsForDay1 = new ArrayList<Event>();

        final Event eventForDay1 = new Event(Event.Type.MOTION, startOfDay1.getMillis(), startOfDay1.plusMinutes(1).getMillis(), DateTimeZone.getDefault().getOffset(startOfDay1));
        eventsForDay1.add(eventForDay1);
        long accountId = 1;


        final DateTime startOfDay2 = startOfDay1.plusDays(1);
        final Event eventForDay2 = new Event(Event.Type.MOTION,
                startOfDay1.plusDays(1).getMillis(),
                startOfDay1.plusDays(1).plusMinutes(1).getMillis(),
                DateTimeZone.getDefault().getOffset(startOfDay1.plusDays(1)));

        final ArrayList<Event> eventsForDay2 = new ArrayList<Event>();
        eventsForDay2.add(eventForDay2);

        final Map<DateTime, List<Event>> eventDayMap = new HashMap<DateTime, List<Event>>();
        eventDayMap.put(startOfDay1, eventsForDay1);
        eventDayMap.put(startOfDay2, eventsForDay2);
        this.eventDAODynamoDB.setEventsForDates(accountId, eventDayMap);

        final Map<DateTime, ImmutableList<Event>> actual = this.eventDAODynamoDB.getEventsForDates(accountId, eventDayMap.keySet());
        for(final DateTime targetDay:eventDayMap.keySet()){
            assertThat(actual.containsKey(targetDay), is(Boolean.TRUE));
            assertThat(actual.get(targetDay), containsInAnyOrder(eventDayMap.get(targetDay).toArray()));
        }



    }


    @Test
    public void testSetAndGetEventsForDate(){
        final DateTime startOfDay1 = DateTime.now().withTimeAtStartOfDay();
        final ArrayList<Event> events = new ArrayList<Event>();

        final Event eventForDay1 = new Event(Event.Type.MOTION, startOfDay1.getMillis(), startOfDay1.plusMinutes(1).getMillis(), DateTimeZone.getDefault().getOffset(startOfDay1));
        events.add(eventForDay1);
        long accountId = 1;
        this.eventDAODynamoDB.setEventsForDate(accountId, startOfDay1, events);
        ImmutableList<Event> actual = this.eventDAODynamoDB.getEventsForDate(accountId, startOfDay1);

        assertThat(events, containsInAnyOrder(actual.toArray()));

        final Event eventForDay2 = new Event(Event.Type.MOTION,
                startOfDay1.plusDays(1).getMillis(),
                startOfDay1.plusDays(1).plusMinutes(1).getMillis(),
                DateTimeZone.getDefault().getOffset(startOfDay1.plusDays(1)));

        final ArrayList<Event> eventsForDay2 = new ArrayList<Event>();
        eventsForDay2.add(eventForDay2);

        actual = this.eventDAODynamoDB.getEventsForDate(accountId, startOfDay1.plusDays(1));
        assertThat(actual, containsInAnyOrder(new Event[]{ }));

        this.eventDAODynamoDB.setEventsForDate(accountId, startOfDay1.plusDays(1), eventsForDay2);
        actual = this.eventDAODynamoDB.getEventsForDate(accountId, startOfDay1.plusDays(1));
        assertThat(actual, containsInAnyOrder(new Event[]{ eventForDay2 }));


        final Event.Type[] eventTypes = new Event.Type[]{Event.Type.MOTION, Event.Type.NOISE, Event.Type.LIGHT};

        int numberOfMinutesPerDay = 24 * 60;
        final ArrayList<Event> allList = new ArrayList<Event>();

        for(final Event.Type type:eventTypes) {
            final ArrayList<Event> eventListOfCertainType = new ArrayList<Event>();
            for (int i = 0; i < numberOfMinutesPerDay; i++) {
                final DateTime eventStartTime = startOfDay1.plusMinutes(i);
                final Event event = new Event(type,
                        (eventStartTime.getMillis()),
                        (eventStartTime.plusMinutes(1).getMillis()),
                        DateTimeZone.getDefault().getOffset(eventStartTime));

                eventListOfCertainType.add(event);
            }


            allList.addAll(eventListOfCertainType);

        }


        this.eventDAODynamoDB.setEventsForDate(accountId, startOfDay1, allList);
        actual = this.eventDAODynamoDB.getEventsForDate(accountId, startOfDay1);
        assertThat(actual, containsInAnyOrder(allList.toArray()));


    }



    /*
    @Test
    public void testCompactnessBySnappyWithFullContent(){
        int numberOfMinutesPerDay = 24 * 60;

        long totalSize = 0;
        long uncompressedTotalSize = 0;


        final Event.Type[] eventTypes = new Event.Type[]{Event.Type.MOTION, Event.Type.NOISE, Event.Type.LIGHT};
        final DateTime startOfTheDay = DateTime.now().withTimeAtStartOfDay();

        for(final Event.Type type:eventTypes) {
            final ArrayList<Event> eventListOfCertainType = new ArrayList<Event>();
            for (int i = 0; i < numberOfMinutesPerDay; i++) {
                final DateTime eventStartTime = startOfTheDay.plusMinutes(i);
                final Event event = new Event(type,
                        (eventStartTime.getMillis()),
                        (eventStartTime.plusMinutes(1).getMillis()),
                        DateTimeZone.getDefault().getOffset(eventStartTime));

                eventListOfCertainType.add(event);
            }



            final ObjectMapper mapper = new ObjectMapper();

            try {
                final String jsonString = mapper.writeValueAsString(eventListOfCertainType);
                final byte[] uncompressed = jsonString.getBytes("UTF-8");

                final byte[] compressed = Snappy.compress(uncompressed);

                uncompressedTotalSize += uncompressed.length;
                totalSize += compressed.length;

                final byte[] decompressed = Snappy.uncompress(compressed);

                final List<Event> actual = mapper.readValue(decompressed, new TypeReference<List<Event>>(){});
                assertThat(actual, containsInAnyOrder(eventListOfCertainType.toArray()));


            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        assertThat(totalSize, greaterThan(64L * 1024 / 2));

    }
    */


    /*
    * So far we compare Gzip compression vs Snappy compression on following event list:
    *   events with all three types: MOTION, NOISE, LIGHT
    *   each minute there is one event
    *   Each type contains one day's event.
    *
    *   Compressed JSON size by Gzip: ~23K
    *   Compressed JSON size by Snappy: ~54K
    *   Compressed Protobuf size by Snappy: ~59K
     */
    /*
    @Test
    public void testCompactnessByGZipWithFullContent(){
        int numberOfMinutesPerDay = 24 * 60;

        long totalSize = 0;
        long uncompressedTotalSize = 0;


        final Event.Type[] eventTypes = new Event.Type[]{Event.Type.MOTION, Event.Type.NOISE, Event.Type.LIGHT};
        final DateTime startOfTheDay = DateTime.now().withTimeAtStartOfDay();

        for(final Event.Type type:eventTypes) {
            final ArrayList<Event> eventListOfCertainType = new ArrayList<Event>();
            for (int i = 0; i < numberOfMinutesPerDay; i++) {
                final DateTime eventStartTime = startOfTheDay.plusMinutes(i);
                final Event event = new Event(type,
                        (eventStartTime.getMillis()),
                        (eventStartTime.plusMinutes(1).getMillis()),
                        DateTimeZone.getDefault().getOffset(eventStartTime));

                eventListOfCertainType.add(event);
            }



            final ObjectMapper mapper = new ObjectMapper();

            try {
                final String jsonString = mapper.writeValueAsString(eventListOfCertainType);
                final byte[] uncompressed = jsonString.getBytes("UTF-8");

                final byte[] compressed = Compression.gzipCompress(uncompressed);

                uncompressedTotalSize += uncompressed.length;
                totalSize += compressed.length;

                final byte[] decompressed = Compression.gzipDecompress(compressed);
                final List<Event> actual = mapper.readValue(decompressed, new TypeReference<List<Event>>(){});
                assertThat(actual, containsInAnyOrder(eventListOfCertainType.toArray()));


            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        assertThat(totalSize, lessThan(64L * 1024 / 2));

    }
    */

    /*
    @Test
    public void testCompactnessByGZipWithTimestampOffset(){
        int numberOfMinutesPerDay = 24 * 60;

        long totalSize = 0;
        long uncompressedTotalSize = 0;


        final Event.Type[] eventTypes = new Event.Type[]{Event.Type.MOTION, Event.Type.NOISE, Event.Type.LIGHT};
        final DateTime startOfTheDay = DateTime.now().withTimeAtStartOfDay();

        for(final Event.Type type:eventTypes) {
            final ArrayList<Event> eventListOfCertainType = new ArrayList<Event>();

            for (int i = 0; i < numberOfMinutesPerDay; i++) {
                final DateTime eventStartTime = startOfTheDay.plusMinutes(i);
                final Event event = new Event(type,
                        (eventStartTime.getMillis() - startOfTheDay.getMillis() / 1000),
                        (eventStartTime.plusMinutes(1).getMillis() - startOfTheDay.getMillis() / 1000),
                        DateTimeZone.getDefault().getOffset(eventStartTime) / 1000);
                eventListOfCertainType.add(event);
            }



            final ObjectMapper mapper = new ObjectMapper();

            try {
                final String jsonString = mapper.writeValueAsString(eventListOfCertainType);
                final byte[] uncompressed = jsonString.getBytes("UTF-8");

                final byte[] compressed = Compression.gzipCompress(uncompressed);

                uncompressedTotalSize += uncompressed.length;
                totalSize += compressed.length;

                final byte[] decompressed = Compression.gzipDecompress(compressed);
                final List<Event> actual = mapper.readValue(decompressed, new TypeReference<List<Event>>(){});
                assertThat(actual, containsInAnyOrder(eventListOfCertainType.toArray()));


            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        assertThat(totalSize, lessThan(64L * 1024 / 2));

    }
    */
}
