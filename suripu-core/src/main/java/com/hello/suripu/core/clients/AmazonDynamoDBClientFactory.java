package com.hello.suripu.core.clients;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.google.common.collect.Maps;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.configuration.NewDynamoDBConfiguration;
import com.hello.suripu.core.db.AlarmDAODynamoDB;
import com.hello.suripu.core.db.BaseDynamoDB;
import com.hello.suripu.core.db.BayesNetHmmModelDAODynamoDB;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.RingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SensorsViewsDynamoDB;
import com.hello.suripu.core.db.SleepHmmDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.WifiInfoDynamoDB;
import com.hello.suripu.core.metrics.InstrumentedDynamoDBClient;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AmazonDynamoDBClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmazonDynamoDBClientFactory.class);

    private final AWSCredentialsProvider awsCredentialsProvider;
    private final ClientConfiguration clientConfiguration;
    private final Map<String, AmazonDynamoDB> clients = Maps.newHashMap();
    private final Map<String, AmazonDynamoDB> instrumentedClients = Maps.newHashMap(); // key is className
    private final NewDynamoDBConfiguration dynamoDBConfiguration;

    private final static ClientConfiguration DEFAULT_CLIENT_CONFIGURATION = new ClientConfiguration().withConnectionTimeout(200).withMaxErrorRetry(1);

    @Deprecated
    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider) {

        final NewDynamoDBConfiguration dynamoDBConfiguration = new NewDynamoDBConfiguration();
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, DEFAULT_CLIENT_CONFIGURATION, dynamoDBConfiguration);
    }

    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider, final ClientConfiguration clientConfiguration, final NewDynamoDBConfiguration dynamoDBConfiguration) {
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, clientConfiguration, dynamoDBConfiguration);
    }

    public static AmazonDynamoDBClientFactory create(final AWSCredentialsProvider awsCredentialsProvider, final NewDynamoDBConfiguration dynamoDBConfiguration) {
        return new AmazonDynamoDBClientFactory(awsCredentialsProvider, DEFAULT_CLIENT_CONFIGURATION, dynamoDBConfiguration);
    }

    private AmazonDynamoDBClientFactory(final AWSCredentialsProvider awsCredentialsProvider, final ClientConfiguration clientConfiguration, final NewDynamoDBConfiguration dynamoDBConfiguration) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.clientConfiguration = clientConfiguration;
        this.dynamoDBConfiguration = dynamoDBConfiguration;
    }


    @Deprecated
    public synchronized AmazonDynamoDB getForEndpoint(final String endpoint) {
        if(clients.containsKey(endpoint)) {
            return clients.get(endpoint);
        }

        final AmazonDynamoDB client = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        client.setEndpoint(endpoint);
        clients.put(endpoint, client);
        return client;
    }


    public synchronized AmazonDynamoDB getForTable(final DynamoDBTableName tableName) {
        if(!dynamoDBConfiguration.tables().containsKey(tableName) || !dynamoDBConfiguration.endpoints().containsKey(tableName)) {
            throw new IllegalArgumentException("Check configuration. Invalid tableName: " + tableName.toString());
        }

        final String endpoint = dynamoDBConfiguration.endpoints().get(tableName);
        if(clients.containsKey(endpoint)) {
            return clients.get(endpoint);
        }

        final AmazonDynamoDB client = new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration);
        client.setEndpoint(endpoint);
        clients.put(endpoint, client);
        return client;
    }

    public synchronized AmazonDynamoDB getInstrumented(final DynamoDBTableName tableName, final Class<?> klass) {
        if(!dynamoDBConfiguration.tables().containsKey(tableName) || !dynamoDBConfiguration.endpoints().containsKey(tableName)) {
            throw new IllegalArgumentException("Check configuration. Invalid tableName: " + tableName.toString());
        }

        final String endpoint = dynamoDBConfiguration.endpoints().get(tableName);
        if(instrumentedClients.containsKey(klass.getName())) {
            return instrumentedClients.get(klass.getName());
        }

        final AmazonDynamoDB client = new InstrumentedDynamoDBClient(new AmazonDynamoDBClient(awsCredentialsProvider, clientConfiguration), klass);
        client.setEndpoint(endpoint);
        instrumentedClients.put(klass.getName(), client);
        return client;
    }

    public synchronized BaseDynamoDB get(final DynamoDBTableName tableName) {
        final AmazonDynamoDB client = getForTable(tableName);
        final String tableNameFromConfig = dynamoDBConfiguration.tables().get(tableName);

        // TODO: add here as required
        switch (tableName) {
            case ALARM_INFO:
                return new MergedUserInfoDynamoDB(client, tableNameFromConfig);
            case ALARM_LOG:
                return new SmartAlarmLoggerDynamoDB(client, tableNameFromConfig);
            case ALARMS:
                return new AlarmDAODynamoDB(client, tableNameFromConfig);
            case BAYESNET_MODEL:
                return new BayesNetHmmModelDAODynamoDB(client, tableNameFromConfig);
            case BAYESNET_PRIORS:
                return new BayesNetHmmModelPriorsDAODynamoDB(client, tableNameFromConfig);
            case CALIBRATION:
                return new CalibrationDynamoDB(client, tableNameFromConfig);
            case INSIGHTS:
                return new InsightsDAODynamoDB(client, tableNameFromConfig);
            case PASSWORD_RESET:
                return PasswordResetDB.create(client, tableNameFromConfig);
            case PREFERENCES:
                return AccountPreferencesDynamoDB.create(client, tableNameFromConfig);
            case RING_TIME_HISTORY:
                return new RingTimeHistoryDAODynamoDB(client, tableNameFromConfig);
            case SENSE_LAST_SEEN:
                return SensorsViewsDynamoDB.create(client, tableNameFromConfig);
            case SLEEP_HMM:
                return new SleepHmmDAODynamoDB(client, tableNameFromConfig);
            case TEAMS:
                return new TeamStore(client, tableNameFromConfig);
            case TIMEZONE_HISTORY:
                return new TimeZoneHistoryDAODynamoDB(client, tableNameFromConfig);
            case WIFI_INFO:
                return new WifiInfoDynamoDB(client, tableNameFromConfig);
        }

        LOGGER.error("{} is not properly configured.", tableName);
        throw new IllegalArgumentException("Bad tablename");
    }



}
