package com.hello.suripu.app.cli;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;

import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import com.hello.suripu.app.configuration.SuripuAppConfiguration;
import com.hello.suripu.core.clients.AmazonDynamoDBClientFactory;
import com.hello.suripu.core.configuration.DynamoDBTableName;
import com.hello.suripu.core.db.AggregateSleepScoreDAODynamoDB;
import com.hello.suripu.core.db.AlarmDAODynamoDB;

import com.hello.suripu.core.db.BayesNetHmmModelDAODynamoDB;
import com.hello.suripu.core.db.BayesNetHmmModelPriorsDAODynamoDB;
import com.hello.suripu.core.db.CalibrationDynamoDB;
import com.hello.suripu.core.db.FeatureStore;
import com.hello.suripu.core.db.FirmwareUpgradePathDAO;
import com.hello.suripu.core.db.InsightsDAODynamoDB;
import com.hello.suripu.core.db.KeyStoreDynamoDB;
import com.hello.suripu.core.db.MergedUserInfoDynamoDB;
import com.hello.suripu.core.db.OTAHistoryDAODynamoDB;
import com.hello.suripu.core.db.ResponseCommandsDAODynamoDB;

import com.hello.suripu.core.db.ScheduledRingTimeHistoryDAODynamoDB;
import com.hello.suripu.core.db.SleepStatsDAODynamoDB;
import com.hello.suripu.core.db.SmartAlarmLoggerDynamoDB;
import com.hello.suripu.core.db.TeamStore;
import com.hello.suripu.core.db.TimeZoneHistoryDAODynamoDB;
import com.hello.suripu.core.db.WifiInfoDynamoDB;
import com.hello.suripu.core.passwordreset.PasswordResetDB;
import com.hello.suripu.core.preferences.AccountPreferencesDynamoDB;
import com.hello.suripu.core.db.SleepHmmDAODynamoDB;
import com.hello.suripu.coredw.db.TimelineDAODynamoDB;
import com.hello.suripu.coredw.db.TimelineLogDAODynamoDB;
import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

public class CreateDynamoDBTables extends ConfiguredCommand<SuripuAppConfiguration> {

    public CreateDynamoDBTables() {
        super("create_dynamodb_tables", "Create dynamoDB tables");
    }

    @Override
    protected void run(Bootstrap<SuripuAppConfiguration> bootstrap, Namespace namespace, SuripuAppConfiguration configuration) throws Exception {
        final AWSCredentialsProvider awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
        final AmazonDynamoDBClientFactory factory = AmazonDynamoDBClientFactory.create(awsCredentialsProvider, configuration.dynamoDBConfiguration());

        createUserInfoTable(configuration, factory);
        createAlarmTable(configuration, factory);
        createFeaturesTable(configuration, factory);
        createTeamsTable(configuration, factory);
        createSleepScoreTable(configuration, factory);
        createScheduledRingTimeHistoryTable(configuration, factory);
        createTimeZoneHistoryTable(configuration, factory);
        createInsightsTable(configuration, factory);
        createAccountPreferencesTable(configuration, factory);
        createSenseKeyStoreTable(configuration, factory);
        createPillKeyStoreTable(configuration, factory);
        createTimelineTable(configuration, factory);
        createPasswordResetTable(configuration, factory);
        createSleepHmmTable(configuration, factory);
//        createRingTimeHistoryTable(configuration, factory);
        createSleepStatsTable(configuration, factory);
//        createAlgorithmTestTable(configuration, factory);
        createTimelineLogTable(configuration, factory);
        createSmartAlarmLogTable(configuration, factory);
        createOTAHistoryTable(configuration, factory);
        createResponseCommandsTable(configuration, factory);
        createFWUpgradePathTable(configuration, factory);
        createHmmBayesNetModelPriorTable(configuration, factory);
        createHmmBayesNetModelTable(configuration, factory);
        createCalibrationTable(configuration, factory);
        createWifiInfoTable(configuration, factory);

    }

    private void createSmartAlarmLogTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory){
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SMART_ALARM_LOG);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.SMART_ALARM_LOG);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SmartAlarmLoggerDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createAccountPreferencesTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.PREFERENCES);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.PREFERENCES);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AccountPreferencesDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createScheduledRingTimeHistoryTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.RING_TIME_HISTORY);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.RING_TIME_HISTORY);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = ScheduledRingTimeHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createSleepScoreTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String version = configuration.getSleepScoreVersion();
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SLEEP_SCORE) + "_" + version;
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.SLEEP_SCORE);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AggregateSleepScoreDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    /*
    private void createAlgorithmTestTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final DynamoDBConfiguration config = configuration.getAlgorithmTestDBConfiguration();
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(config.getEndpoint());
        final String tableName = config.getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AlgorithmResultsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }
     */

    private void createInsightsTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.INSIGHTS);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.INSIGHTS);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = InsightsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTeamsTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.TEAMS);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.TEAMS);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TeamStore.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createUserInfoTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.ALARM_INFO);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.ALARM_INFO);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = MergedUserInfoDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createFeaturesTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {

        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.FEATURES);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.FEATURES);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = FeatureStore.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createAlarmTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.ALARMS);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.ALARMS);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = AlarmDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTimeZoneHistoryTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {

        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.TIMEZONE_HISTORY);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.TIMEZONE_HISTORY);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TimeZoneHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createSenseKeyStoreTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SENSE_KEY_STORE);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.SENSE_KEY_STORE);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = KeyStoreDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createPillKeyStoreTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.PILL_KEY_STORE);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.PILL_KEY_STORE);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = KeyStoreDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTimelineTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.TIMELINE);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.TIMELINE);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TimelineDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }


    private void createPasswordResetTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.TIMELINE);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.TIMELINE);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = PasswordResetDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println("Table: " + tableName + " " + description.getTableStatus());
        }
    }

    /*
    private void createRingTimeHistoryTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory){
        final AmazonDynamoDBClient client = new AmazonDynamoDBClient(awsCredentialsProvider);

        client.setEndpoint(configuration.getRingTimeHistoryDBConfiguration().getEndpoint());
        final String tableName = configuration.getRingTimeHistoryDBConfiguration().getTableName();
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = RingTimeHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println("Table: " + tableName + " " + description.getTableStatus());
        }
    }
    */

    private void createSleepHmmTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SLEEP_HMM);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.SLEEP_HMM);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SleepHmmDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createSleepStatsTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String version = configuration.getSleepStatsVersion();

        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SLEEP_STATS) + "_" + version;
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.SLEEP_STATS);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = SleepStatsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createTimelineLogTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.TIMELINE_LOG);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.TIMELINE_LOG);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = TimelineLogDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createOTAHistoryTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.OTA_HISTORY);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.OTA_HISTORY);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = OTAHistoryDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createResponseCommandsTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.SYNC_RESPONSE_COMMANDS);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.SYNC_RESPONSE_COMMANDS);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = ResponseCommandsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createFWUpgradePathTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.FIRMWARE_UPGRADE_PATH);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.FIRMWARE_UPGRADE_PATH);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = FirmwareUpgradePathDAO.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }


    private void createHmmBayesNetModelPriorTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.BAYESNET_PRIORS);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.BAYESNET_PRIORS);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = BayesNetHmmModelPriorsDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createHmmBayesNetModelTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.BAYESNET_MODEL);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.BAYESNET_MODEL);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = BayesNetHmmModelDAODynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }


    private void createCalibrationTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.CALIBRATION);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.CALIBRATION);

        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = CalibrationDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

    private void createWifiInfoTable(final SuripuAppConfiguration configuration, final AmazonDynamoDBClientFactory factory) {
        final String tableName = configuration.dynamoDBConfiguration().tables().get(DynamoDBTableName.WIFI_INFO);
        final AmazonDynamoDB client = factory.getForTable(DynamoDBTableName.WIFI_INFO);
        try {
            client.describeTable(tableName);
            System.out.println(String.format("%s already exists.", tableName));
        } catch (AmazonServiceException exception) {
            final CreateTableResult result = WifiInfoDynamoDB.createTable(tableName, client);
            final TableDescription description = result.getTableDescription();
            System.out.println(description.getTableStatus());
        }
    }

}
