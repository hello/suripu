package com.hello.suripu.app.cli;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hello.suripu.algorithm.core.AlgorithmException;
import com.hello.suripu.algorithm.core.AmplitudeData;
import com.hello.suripu.algorithm.core.DataSource;
import com.hello.suripu.algorithm.core.Segment;
import com.hello.suripu.algorithm.event.EventDetectionAlgorithm;
import com.hello.suripu.algorithm.sleep.AwakeDetectionAlgorithm;
import com.hello.suripu.algorithm.sleep.SleepDetectionAlgorithm;
import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.datasource.InMemoryAmplitudeDataSource;
import com.hello.suripu.core.db.AccountDAO;
import com.hello.suripu.core.db.AccountDAOImpl;
import com.hello.suripu.core.db.EventDAODynamoDB;
import com.hello.suripu.core.db.TrackerMotionDAO;
import com.hello.suripu.core.db.util.JodaArgumentFactory;
import com.hello.suripu.core.models.Account;
import com.hello.suripu.core.models.Event;
import com.hello.suripu.core.models.TrackerMotion;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.db.ManagedDataSource;
import com.yammer.dropwizard.db.ManagedDataSourceFactory;
import com.yammer.dropwizard.jdbi.ImmutableListContainerFactory;
import com.yammer.dropwizard.jdbi.ImmutableSetContainerFactory;
import com.yammer.dropwizard.jdbi.OptionalContainerFactory;
import com.yammer.dropwizard.jdbi.args.OptionalArgumentFactory;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 6/16/14.
 */
public class RecreateEventsCommand extends ConfiguredCommand<SuripuAppConfiguration> {

    private final static Logger LOGGER = LoggerFactory.getLogger(RecreateEventsCommand.class);

    public RecreateEventsCommand(){
        super("recreate_events", "Recreate events from motion table");

    }

    @Override
    public void configure(Subparser subparser) {
        super.configure(subparser);
        subparser.addArgument("user_name").nargs("?").help("The account_id from Account table, please consult Tim.");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final ManagedDataSourceFactory managedDataSourceFactory = new ManagedDataSourceFactory();
        final ManagedDataSource dataSource = managedDataSourceFactory.build(configuration.getSensorsDB());

        final DBI jdbi = new DBI(dataSource);
        jdbi.registerArgumentFactory(new OptionalArgumentFactory(configuration.getSensorsDB().getDriverClass()));
        jdbi.registerContainerFactory(new ImmutableListContainerFactory());
        jdbi.registerContainerFactory(new ImmutableSetContainerFactory());
        jdbi.registerContainerFactory(new OptionalContainerFactory());
        jdbi.registerArgumentFactory(new JodaArgumentFactory());


        final TrackerMotionDAO trackerMotionDAO = jdbi.onDemand(TrackerMotionDAO.class);
        final AccountDAO accountDAO = jdbi.onDemand(AccountDAOImpl.class);



        final AWSCredentialsProvider awsCredentialsProvider= new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getEventDBConfiguration().getEndpoint());
        final String eventTableName = configuration.getEventDBConfiguration().getTableName();
        final EventDAODynamoDB eventDAODynamoDB = new EventDAODynamoDB(client, eventTableName);





        final String userName = namespace.getString("user_name");
        final Optional<Account> accountOptional = accountDAO.getByEmail(userName);
        if(!accountOptional.isPresent()){
            LOGGER.warn("Email " + userName + " not exists!");
            return;
        }

        final Account account = accountOptional.get();
        final ImmutableList<TrackerMotion> oldDataFromPostgres = trackerMotionDAO.getBetween(account.id,
                new DateTime(0),
                DateTime.now().plusHours(23).plusMinutes(59).plusSeconds(59).plusMillis(999));

        final LinkedList<AmplitudeData> fullData = new LinkedList<AmplitudeData>();
        for(final TrackerMotion datum:oldDataFromPostgres){
            fullData.add(new AmplitudeData(datum.timestamp, datum.value, datum.offsetMillis));
        }
        final DataSource<AmplitudeData> inMemoryDataSource = new InMemoryAmplitudeDataSource(fullData);
        final EventDetectionAlgorithm algorithm = new EventDetectionAlgorithm(inMemoryDataSource, 10 * 60 * 1000);
        final SleepDetectionAlgorithm sleepDetectionAlgorithm = new AwakeDetectionAlgorithm(inMemoryDataSource, 10 * 60 * 1000);

        final DateTime firstDate = new DateTime(oldDataFromPostgres.get(0).timestamp, DateTimeZone.forOffsetMillis(-25200000)).withTimeAtStartOfDay();
        final DateTime lastDate = new DateTime(oldDataFromPostgres.get(oldDataFromPostgres.size() - 1).timestamp, DateTimeZone.forOffsetMillis(-25200000)).withTimeAtStartOfDay();



        int dayOffset = 0;
        final Map<DateTime, List<Event>> generatedEvents = new HashMap<DateTime, List<Event>>();


        while(firstDate.plusDays(dayOffset).getMillis() < lastDate.getMillis()){

            final DateTime targetDay = firstDate.plusDays(dayOffset);

            LOGGER.info(DateTime.now().toString() + ": detecting events for target date: " + targetDay);
            Segment sleepPeriod = new Segment();

            try{
                sleepPeriod = sleepDetectionAlgorithm.getSleepPeriod(targetDay);
            }catch (AlgorithmException alex){
                LOGGER.warn("Sleep detection error: " + alex.getMessage());
            }


            final List<Segment> rawEvents = algorithm.getEventsForDate(targetDay);
            if(rawEvents.size() != 0){
                final List<Event> events = new ArrayList<Event>();
                for(final Segment rawEvent:rawEvents){

                    if(rawEvent.getStartTimestamp() >= sleepPeriod.getStartTimestamp() && rawEvent.getEndTimestamp() <= sleepPeriod.getEndTimestamp()){
                        events.add(new Event(Event.Type.SLEEP_MOTION, rawEvent.getStartTimestamp(), rawEvent.getEndTimestamp(), rawEvent.getOffsetMillis()));
                    }else {
                        events.add(new Event(Event.Type.MOTION, rawEvent.getStartTimestamp(), rawEvent.getEndTimestamp(), rawEvent.getOffsetMillis()));
                    }

                }
                generatedEvents.put(targetDay, events);

            }

            dayOffset++;

            LOGGER.info(DateTime.now().toString() + ": event detection completed for target date: " + targetDay);
        }

        LOGGER.info(DateTime.now().toString() + ": Saving events to dynamoDB.");

        // Shall we use the batch?
        eventDAODynamoDB.setEventsForDates(account.id, generatedEvents);
        LOGGER.info(DateTime.now().toString() + ": All events have been saved for user: " + account.email);

    }
}
